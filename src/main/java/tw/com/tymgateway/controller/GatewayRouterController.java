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

    // Note: People module uses gRPC routing via PeopleController
    // Other modules use HTTP routing below

    // Generic route for other modules
    @PostMapping("/**")
    public Mono<ResponseEntity<String>> routePost(@RequestBody(required = false) String body,
                                                 @RequestHeader(value = "Content-Type", required = false) String contentType,
                                                 ServerHttpRequest request) {
        String targetPath = extractTargetPath(request.getPath().value(), "/tymgateway/tymb");
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
        return webClient.get()
                .uri(targetPath)
                .retrieve()
                .toEntity(String.class);
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
