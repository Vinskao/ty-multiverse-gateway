package tw.com.tymgateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
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
     * CORS 過濾器配置
     * 
     * @return CorsWebFilter 配置好的 CORS 過濾器
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowCredentials(true);
        corsConfig.setAllowedOriginPatterns(List.of("*"));
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        corsConfig.setAllowedHeaders(List.of("*"));
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }

    @Value("${BACKEND_SERVICE_URL:http://localhost:8080}")
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

}

