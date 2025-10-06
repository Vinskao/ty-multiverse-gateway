package tw.com.tymgateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

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
    public Mono<ResponseEntity<Map<String, Object>>> getFallback() {
        return Mono.just(createFallbackResponse());
    }

    /**
     * POST 請求降級處理
     * 
     * @return 降級響應
     */
    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> postFallback() {
        return Mono.just(createFallbackResponse());
    }

    /**
     * 創建降級響應
     * 
     * @return 降級響應實體
     */
    private ResponseEntity<Map<String, Object>> createFallbackResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SERVICE_UNAVAILABLE");
        response.put("message", "後端服務暫時不可用，請稍後再試");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("code", HttpStatus.SERVICE_UNAVAILABLE.value());
        
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }
}

