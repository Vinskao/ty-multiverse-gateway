package tw.com.tymgateway.controller;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import tw.com.ty.common.response.BackendApiResponse;
import tw.com.tymgateway.dto.AsyncResultMessage;
import tw.com.tymgateway.service.AsyncResultRegistry;

/**
 * 異步代理控制器基類
 *
 * <p>提供通用的異步代理功能，讓子類繼承使用。</p>
 *
 * <p>異步代理流程：
 * <ol>
 *     <li>向 Backend 發送請求，獲得 requestId</li>
 *     <li>於 Gateway 端等待 Consumer 實際處理結果</li>
 *     <li>將最終資料以 HTTP 200 回傳給前端</li>
 * </ol>
 * </p>
 */
public abstract class BaseAsyncProxyController {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final WebClient backendWebClient;
    protected final AsyncResultRegistry asyncResultRegistry;
    protected final Duration gatewayWaitTimeout;

    protected BaseAsyncProxyController(
        WebClient backendWebClient,
        AsyncResultRegistry asyncResultRegistry,
        @Value("${gateway.async.timeout:30}") long waitTimeoutSeconds
    ) {
        this.backendWebClient = backendWebClient;
        this.asyncResultRegistry = asyncResultRegistry;
        this.gatewayWaitTimeout = Duration.ofSeconds(waitTimeoutSeconds);
    }

    /**
     * 代理異步後端調用，並等待結果
     *
     * @param requestSpec WebClient request spec
     * @param authorization Authorization header
     * @return 最終響應
     */
    protected Mono<ResponseEntity<Object>> proxyAsyncBackendCall(
        WebClient.RequestHeadersSpec<?> requestSpec,
        String authorization
    ) {
        return requestSpec
            .headers(headers -> {
                if (authorization != null && !authorization.isBlank()) {
                    headers.set(HttpHeaders.AUTHORIZATION, authorization);
                }
            })
            .retrieve()
            .bodyToMono(new ParameterizedBackendResponse())
            .flatMap(response -> {
                if (!response.isSuccess()
                    || response.getRequestId() == null
                    || response.getCode() != HttpStatus.ACCEPTED.value()) {
                    logger.error("後端未返回有效的 requestId 或狀態碼不是 202, response={}", response);
                    return Mono.just(ResponseEntity.status(response.getCode())
                        .body((Object) response));
                }

                String requestId = response.getRequestId();
                logger.info("✅ 後端接受請求，requestId={}", requestId);

                return asyncResultRegistry.awaitResult(requestId, gatewayWaitTimeout)
                    .map(this::toSuccessResponse)
                    .onErrorResume(throwable -> {
                        logger.error("等待異步結果超時或失敗: requestId={}, error={}", requestId, throwable.getMessage());
                        return Mono.just(ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                            .body((Object) String.format("等待異步結果超時或失敗: %s", throwable.getMessage())));
                    });
            });
    }

    /**
     * 將異步結果消息轉換為成功響應
     *
     * @param resultMessage 異步結果消息
     * @return HTTP 響應
     */
    protected ResponseEntity<Object> toSuccessResponse(AsyncResultMessage resultMessage) {
        if (!"completed".equalsIgnoreCase(resultMessage.getStatus())) {
            String errorMessage = resultMessage.getError() != null
                ? resultMessage.getError()
                : "異步處理失敗";
            logger.error("異步請求處理失敗: requestId={}, error={}",
                resultMessage.getRequestId(), errorMessage);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorMessage);
        }

        Object data = resultMessage.getData();
        logger.info("📤 返回最終結果: requestId={}", resultMessage.getRequestId());
        return ResponseEntity.ok(data);
    }

    /**
     * 解析 BackendApiResponse 的 ParameterizedTypeReference
     */
    protected static class ParameterizedBackendResponse extends org.springframework.core.ParameterizedTypeReference<BackendApiResponse<Object>> {
    }
}
