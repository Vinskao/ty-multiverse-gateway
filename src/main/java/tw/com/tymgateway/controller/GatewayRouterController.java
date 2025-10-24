package tw.com.tymgateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Gateway 路由控制器
 *
 * <p>手動實現路由功能來替換Spring Cloud Gateway，這樣可以避免與gRPC依賴衝突</p>
 *
 * @author TY Team
 * @version 1.0
 */
@RestController
@RequestMapping("/tymgateway/tymb")
public class GatewayRouterController {

    @Autowired(required = false)
    private WebClient webClient;

    // Note: People, Weapons, Gallery, Deckofcards modules use gRPC routing via respective Controllers
    // Other modules use HTTP routing below

    // Generic route for other modules (excluding gRPC modules)
    @PostMapping("/**")
    public Mono<ResponseEntity<String>> routePost(@RequestBody(required = false) String body,
                                                 @RequestHeader(value = "Content-Type", required = false) String contentType,
                                                 ServerHttpRequest request) {
        String targetPath = extractTargetPath(request.getPath().value(), "/tymgateway/tymb");

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
        String targetPath = extractTargetPath(request.getPath().value(), "/tymgateway/tymb");

        // Skip routing for modules that have dedicated gRPC controllers
        if (isGrpcModule(targetPath)) {
            return Mono.error(new IllegalStateException("Request should be handled by dedicated gRPC controller"));
        }

        return webClient.get()
                .uri(targetPath)
                .retrieve()
                .toEntity(String.class);
    }

    /**
     * 檢查是否為 gRPC 模組的路徑
     * 這些模組有專用的 gRPC Controller，不需要通過通用路由器處理
     */
    private boolean isGrpcModule(String targetPath) {
        return targetPath.startsWith("/people") ||
               targetPath.startsWith("/weapons") ||
               targetPath.startsWith("/gallery") ||
               targetPath.startsWith("/deckofcards");
    }

    /**
     * 從完整路徑中提取目標路徑
     * 例如：/tymgateway/tymb/people/get-by-name -> /people/get-by-name
     */
    private String extractTargetPath(String fullPath, String prefix) {
        if (fullPath.startsWith(prefix)) {
            return fullPath.substring(prefix.length());
        }
        return fullPath;
    }
}
