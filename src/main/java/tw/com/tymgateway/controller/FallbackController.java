package tw.com.tymgateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import tw.com.ty.common.response.GatewayResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 熔斷降級控制器
 * 
 * <p>當後端服務不可用時，提供降級響應</p>
 * 
 * @author TY Team
 * @version 1.0
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /**
     * GET 請求降級處理
     *
     * @return 降級響應
     */
    @GetMapping
    public Mono<ResponseEntity<GatewayResponse<Void>>> getFallback() {
        return Mono.just(createFallbackResponse());
    }

    /**
     * POST 請求降級處理
     *
     * @return 降級響應
     */
    @PostMapping
    public Mono<ResponseEntity<GatewayResponse<Void>>> postFallback() {
        return Mono.just(createFallbackResponse());
    }

    /**
     * 創建降級響應
     *
     * @return 降級響應實體
     */
    private ResponseEntity<GatewayResponse<Void>> createFallbackResponse() {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(GatewayResponse.serviceUnavailable("后端服务暂时不可用，请稍后再试"));
    }
}

