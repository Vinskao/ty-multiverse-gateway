package tw.com.tymgateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Gateway 通用路由控制器
 *
 * <p>处理非 gRPC 模块的通用路由，与 Spring Cloud Gateway 共存：</p>
 * <ul>
 *   <li>Spring Cloud Gateway: 处理 /api/*, /auth/*, /health/* 等简单 HTTP 转发</li>
 *   <li>Manual Controllers: 处理 /people/*, /weapons/*, /gallery/* 等 gRPC 路由</li>
 *   <li>此控制器: 作为 fallback，处理未被上述两者匹配的请求</li>
 * </ul>
 *
 * <p>路由优先级：</p>
 * <ol>
 *   <li>专用 gRPC Controllers (@RequestMapping 精确匹配)</li>
 *   <li>Spring Cloud Gateway routes (application.yml 配置)</li>
 *   <li>此通用控制器 (/** 通配符，最低优先级)</li>
 * </ol>
 *
 * @author TY Team
 * @version 2.0
 */
@RestController
@RequestMapping("/tymg")
public class GatewayRouterController {

    @Autowired(required = false)
    private WebClient webClient;

    // Note: 此控制器作为 fallback，优先级最低
    // gRPC 模块由专用 Controllers 处理
    // 简单 HTTP 转发由 Spring Cloud Gateway 处理

    // 临时禁用通用路由，让 Spring Cloud Gateway 完全接管
    // Generic fallback route for unmatched requests - DISABLED for Spring Cloud Gateway testing

    /*
    @PostMapping("/**")
    public Mono<ResponseEntity<String>> routePost(@RequestBody(required = false) String body,
                                                 @RequestHeader(value = "Content-Type", required = false) String contentType,
                                                 ServerHttpRequest request) {
        String targetPath = extractTargetPath(request.getPath().value(), "/tymg");

        // Skip routing for modules that have dedicated gRPC controllers
        if (isGrpcModule(targetPath)) {
            return Mono.error(new IllegalStateException("Request should be handled by dedicated gRPC controller"));
        }

        return webClient.post()
                .uri(targetPath)
                .header("Content-Type", contentType != null ? contentType : "application/json")
                .bodyValue(body != null ? body : "")
                .retrieve()
                .toEntity(String.class);
    }

    @GetMapping("/**")
    public Mono<ResponseEntity<String>> routeGet(ServerHttpRequest request) {
        String targetPath = extractTargetPath(request.getPath().value(), "/tymg");

        // Skip routing for modules that have dedicated gRPC controllers
        if (isGrpcModule(targetPath)) {
            return Mono.error(new IllegalStateException("Request should be handled by dedicated gRPC controller"));
        }

        return webClient.get()
                .uri(targetPath)
                .retrieve()
                .toEntity(String.class);
    }

    @DeleteMapping("/**")
    public Mono<ResponseEntity<String>> routeDelete(ServerHttpRequest request) {
        String targetPath = extractTargetPath(request.getPath().value(), "/tymg");

        // Skip routing for modules that have dedicated gRPC controllers
        if (isGrpcModule(targetPath)) {
            return Mono.error(new IllegalStateException("Request should be handled by dedicated gRPC controller"));
        }

        return webClient.delete()
                .uri(targetPath)
                .retrieve()
                .toEntity(String.class);
    }

    @PutMapping("/**")
    public Mono<ResponseEntity<String>> routePut(@RequestBody(required = false) String body,
                                                @RequestHeader(value = "Content-Type", required = false) String contentType,
                                                ServerHttpRequest request) {
        String targetPath = extractTargetPath(request.getPath().value(), "/tymg");

        // Skip routing for modules that have dedicated gRPC controllers
        if (isGrpcModule(targetPath)) {
            return Mono.error(new IllegalStateException("Request should be handled by dedicated gRPC controller"));
        }

        return webClient.put()
                .uri(targetPath)
                .header("Content-Type", contentType != null ? contentType : "application/json")
                .bodyValue(body != null ? body : "")
                .retrieve()
                .toEntity(String.class);
    }
    */

    /**
     * 檢查是否為 gRPC 模組的路徑
     * 這些模組有專用的 gRPC Controller，不需要通過通用路由器處理
     */
    private boolean isGrpcModule(String targetPath) {
        // People, Weapons, Gallery, Deckofcards 現在都有專用的控制器
        // 所以這些路徑不會通過通用路由器處理
        return false; // 暫時禁用，讓各個專用控制器處理
    }

    /**
     * 從完整路徑中提取目標路徑
     * 例如：/tymg/people/get-by-name -> /people/get-by-name
     */
    private String extractTargetPath(String fullPath, String prefix) {
        if (fullPath.startsWith(prefix)) {
            return fullPath.substring(prefix.length());
        }
        return fullPath;
    }
}
