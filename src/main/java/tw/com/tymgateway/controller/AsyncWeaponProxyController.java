package tw.com.tymgateway.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import tw.com.tymgateway.service.AsyncResultRegistry;

/**
 * Weapon 模組異步代理 Controller
 *
 * <p>前端請求 /tymg/weapons/** 時，Gateway 會：
 * <ol>
 *     <li>向 Backend 發送請求，獲得 requestId</li>
 *     <li>於 Gateway 端等待 Consumer 實際處理結果</li>
 *     <li>將最終資料以 HTTP 200 回傳給前端</li>
 * </ol>
 * </p>
 */
@RestController
@RequestMapping("/tymg/weapons")
public class AsyncWeaponProxyController extends BaseAsyncProxyController {

    public AsyncWeaponProxyController(
        WebClient backendWebClient,
        AsyncResultRegistry asyncResultRegistry,
        long waitTimeoutSeconds
    ) {
        super(backendWebClient, asyncResultRegistry, waitTimeoutSeconds);
    }

    /**
     * 同步獲取所有武器
     *
     * @param authorization Authorization header (可為空)
     * @return 所有武器列表
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> getAllWeapons(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        logger.info("🔁 Gateway 同步代理請求: /weapons");
        return proxyAsyncBackendCall(
            backendWebClient.get().uri("/weapons"),
            authorization
        );
    }

    /**
     * 同步根據 ID 獲取武器
     *
     * @param weaponId 武器 ID
     * @param authorization Authorization header (可為空)
     * @return 武器數據
     */
    @GetMapping(value = "/{weaponId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> getWeaponById(
        @RequestParam String weaponId,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        logger.info("🔁 Gateway 同步代理請求: /weapons/{}", weaponId);
        return proxyAsyncBackendCall(
            backendWebClient.get().uri("/weapons/{weaponId}", weaponId),
            authorization
        );
    }
}
