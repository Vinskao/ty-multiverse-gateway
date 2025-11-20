package tw.com.tymgateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Swagger UI 重定向控制器
 * 处理 /tymg/swagger-ui 路径的重定向到 SpringDoc 的 Swagger UI
 */
@RestController
public class SwaggerUiController {

    /**
     * 重定向 /tymg/swagger-ui 到 SpringDoc 的 Swagger UI
     * SpringDoc WebFlux 默认路径是 /swagger-ui/index.html
     */
    @GetMapping("/tymg/swagger-ui")
    public Mono<Void> redirectToSwaggerUi(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.SEE_OTHER);
        // SpringDoc WebFlux 使用 /swagger-ui/index.html 作为入口
        response.getHeaders().setLocation(URI.create("/swagger-ui/index.html"));
        return response.setComplete();
    }

    /**
     * 重定向 /tymg/swagger-ui/ 到 /swagger-ui/index.html
     */
    @GetMapping("/tymg/swagger-ui/")
    public Mono<Void> redirectToSwaggerUiSlash(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.SEE_OTHER);
        response.getHeaders().setLocation(URI.create("/swagger-ui/index.html"));
        return response.setComplete();
    }

    /**
     * /tymg/swagger-ui/index.html 直接重定向到 /swagger-ui/index.html
     */
    @GetMapping("/tymg/swagger-ui/index.html")
    public Mono<Void> redirectToSwaggerUiIndex(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.SEE_OTHER);
        response.getHeaders().setLocation(URI.create("/swagger-ui/index.html"));
        return response.setComplete();
    }
}

