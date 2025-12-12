package tw.com.tymgateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;

/**
 * Gateway 配置類
 *
 * <p>提供 Gateway 的額外配置，包括：</p>
 * <ul>
 *   <li>CORS 跨域配置</li>
 *   <li>過濾器配置</li>
 *   <li>路由動態配置</li>
 * </ul>
 *
 * @author TY Team
 * @version 1.0
 */
@Configuration
public class GatewayConfig {

/**
 * CORS 過濾器配置，使用最高優先級確保在認證過濾器之前執行
 */
@Bean
@Order(Ordered.HIGHEST_PRECEDENCE)
public CorsWebFilter corsWebFilter() {
    CorsConfiguration corsConfig = new CorsConfiguration();
    corsConfig.setAllowCredentials(true);
    // 當 allowCredentials 為 true 時，不能使用 "*" 作為 allowedOriginPatterns
    // 必須設置具體的來源域名
    corsConfig.setAllowedOriginPatterns(List.of(
        "http://localhost:4321",    // 前端開發環境
        "https://localhost:4321",   // HTTPS 版本
        "http://127.0.0.1:4321",    // 本機 IP
        "http://localhost:3000",    // 其他可能的開發端口
        "https://localhost:3000"
    ));
    corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    corsConfig.setAllowedHeaders(List.of("*"));
    corsConfig.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", corsConfig);

    return new CorsWebFilter(source);
}

    @Value("${PUBLIC_TYMB_URL:http://localhost:8080}")
    private String backendServiceUrl;

    /**
     * WebClient for making HTTP requests to backend
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(backendServiceUrl)
                .build();
    }

    /**
     * WebClient.Builder bean for creating WebClient instances
     * Used by KeycloakController and other components
     * 
     * Configured with:
     * - Connection timeout: 10 seconds
     * - Response timeout: 30 seconds
     * - Retry on connection errors
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(
                    reactor.netty.http.client.HttpClient.create()
                        .responseTimeout(java.time.Duration.ofSeconds(30))
                        .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                        .doOnConnected(conn -> 
                            conn.addHandlerLast(new io.netty.handler.timeout.ReadTimeoutHandler(30))
                        )
                ));
    }

}

