package tw.com.tymgateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Gateway Swagger 配置
 * 提供 TY Multiverse Gateway API 文檔
 *
 * @author TY Backend Team
 * @version 1.0
 * @since 2025
 */
@Configuration
public class SwaggerConfig {

    @Value("${GATEWAY_SWAGGER_URL:http://localhost:8082}")
    private String gatewaySwaggerUrl;

    @Bean
    public OpenAPI gatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TY Multiverse Gateway API")
                        .description("TY Multiverse 統一 API Gateway - 所有 /tymg/* 路由的文檔\n\n" +
                                "## 路由說明\n\n" +
                                "- **People 模組**: `/tymg/people/*` → People 管理\n" +
                                "- **Weapon 模組**: `/tymg/weapons/*` → 武器管理\n" +
                                "- **Gallery 模組**: `/tymg/gallery/*` → 圖片管理\n" +
                                "- **Async 請求**: `/tymg/api/request-status/*` → 非同步請求狀態\n" +
                                "- **Health 檢查**: `/tymg/health/*` → 健康檢查\n\n" +
                                "## 測試說明\n\n" +
                                "所有 API 都可以直接在 Swagger UI 中測試，會自動添加正確的 `/tymg` 前綴。"
                        )
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("TY Backend Team")
                                .email("backend@ty.com")
                                .url("https://github.com/Vinskao/ty-multiverse"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url(gatewaySwaggerUrl)
                                .description("本地開發環境 Gateway"),
                        new Server()
                                .url("https://peoplesystem.tatdvsonorth.com")
                                .description("生產環境 Gateway")
                ));
    }
}
