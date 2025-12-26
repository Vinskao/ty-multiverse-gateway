package tw.com.tymgateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import tw.com.ty.common.security.config.BaseSecurityConfig;

/**
 * Gateway Security 配置
 *
 * <p>基于 AGENTS.md 的架构设计：</p>
 * <ul>
 *   <li>Gateway 负责统一认证入口（粗粒度）</li>
 *   <li>验证所有请求的 JWT Token 有效性</li>
 *   <li>路由级别的基础权限控制</li>
 *   <li>不做具体方法级别的权限判断（由 Backend 负责）</li>
 * </ul>
 *
 * <p>参考文档：</p>
 * <ul>
 *   <li>ty-multiverse-gateway/AGENTS.md - Gateway 架构说明</li>
 *   <li>ty-multiverse-frontend/AGENTS.md - API 路由定义</li>
 * </ul>
 *
 * @author TY Backend Team
 * @version 1.0
 * @since 2025
 * @see BaseSecurityConfig
 */
@Configuration
@EnableWebFluxSecurity
@Import(BaseSecurityConfig.class)
public class SecurityConfig {

    @Value("${keycloak.auth-server-url}")
    private String keycloakAuthServerUrl;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    /**
     * 配置 Security Web Filter Chain
     *
     * <p>Gateway 的职责（粗粒度）：</p>
     * <ul>
     *   <li>验证 JWT Token 有效性</li>
     *   <li>确保所有业务请求都有有效 Token</li>
     *   <li>公共路径无需 Token</li>
     * </ul>
     *
     * <p>具体的方法级别权限控制由 Backend 的 @PreAuthorize 实现</p>
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            // CSRF 配置
            .csrf(csrf -> csrf.disable())

            // 授权规则：路由级别（粗粒度）
            .authorizeExchange(exchanges -> exchanges
                // ========================================
                // CORS 预检请求：OPTIONS 方法完全放行
                // ========================================
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // ========================================
                // 公共路径：无需 Token
                // ========================================
                // 健康检查和监控
                .pathMatchers("/tymg/health/**").permitAll()
                .pathMatchers("/tymg/actuator/**").permitAll()

                // Swagger UI 和 API 文档
                .pathMatchers("/tymg/swagger-ui/**").permitAll()
                .pathMatchers("/tymg/v3/api-docs/**").permitAll()
                .pathMatchers("/tymg/webjars/**").permitAll()
                .pathMatchers("/tymg/api-docs/**").permitAll()
                // SpringDoc 默认路径（不带 /tymg 前缀）
                .pathMatchers("/swagger-ui/**").permitAll()
                .pathMatchers("/swagger-ui.html").permitAll()
                .pathMatchers("/swagger-ui/index.html").permitAll()
                .pathMatchers("/v3/api-docs/**").permitAll()
                .pathMatchers("/webjars/**").permitAll()

                // Keycloak OAuth endpoints
                .pathMatchers("/tymg/keycloak/**").permitAll()
                
                // Auth endpoints - Forward to Backend
                .pathMatchers("/tymg/auth/**").permitAll()

                // ========================================
                // 业务路径：GET 请求放行，其他方法需要有效 Token
                // ========================================
                // People Module - 所有操作放行（由 AsyncPeopleProxyController 处理，保持 gateway -> backend -> consumer 流程）
                .pathMatchers("/tymg/people/**").permitAll()

                // Weapon Module - GET 请求放行，其他方法需要 Token
                .pathMatchers(HttpMethod.GET, "/tymg/weapons/**").permitAll()
                .pathMatchers(HttpMethod.POST, "/tymg/weapons/**").authenticated()
                .pathMatchers(HttpMethod.PUT, "/tymg/weapons/**").authenticated()
                .pathMatchers(HttpMethod.DELETE, "/tymg/weapons/**").authenticated()

                // Gallery Module - GET 请求放行，其他方法需要 Token
                .pathMatchers(HttpMethod.GET, "/tymg/gallery/**").permitAll()
                .pathMatchers(HttpMethod.POST, "/tymg/gallery/**").authenticated()
                .pathMatchers(HttpMethod.PUT, "/tymg/gallery/**").authenticated()
                .pathMatchers(HttpMethod.DELETE, "/tymg/gallery/**").authenticated()

                // 其他 Async API - 需要 Token（這些通常是异步操作）
                .pathMatchers("/tymg/api/**").authenticated()

                // CKEditor Module - 允許公開訪問 (Token 在 Query Param 中，Gateway 默認 Resolver 無法讀取，故先放行)
                .pathMatchers("/tymg/ckeditor/**").permitAll()

                // ========================================
                // 默认规则：需要有效 Token（JWT 验证已启用）
                // ========================================
                .anyExchange().authenticated()
            )

            // OAuth2 Resource Server：JWT Token 验证
            // Gateway 负责统一认证入口（粗粒度）
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtDecoder(reactiveJwtDecoder())
                )
            )

            .build();
    }

    /**
     * Reactive JWT Decoder 配置
     * 从 Keycloak 获取公钥验证 JWT Token
     */
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        String jwkSetUri = keycloakAuthServerUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/certs";
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}

