package tw.com.tymgateway;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TY Multiverse API Gateway Application
 *
 * <p>這是 TY Multiverse 系統的 API Gateway 入口，使用手動路由配置避免與gRPC衝突：</p>
 * <ul>
 *   <li>統一的 API 路由管理</li>
 *   <li>請求過濾與轉換</li>
 *   <li>限流與熔斷保護</li>
 *   <li>負載均衡</li>
 *   <li>跨域處理</li>
 *   <li>gRPC客戶端支援</li>
 *   <li>RabbitMQ 消息監聽（異步結果推送）</li>
 * </ul>
 *
 * @author TY Team
 * @version 1.0
 * @since 2024
 */
@SpringBootApplication
@EnableRabbit
public class TYMGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(TYMGatewayApplication.class, args);
    }
}

