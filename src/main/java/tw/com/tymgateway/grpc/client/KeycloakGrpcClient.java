package tw.com.tymgateway.grpc.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tw.com.tymgateway.grpc.people.*;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

/**
 * gRPC Keycloak Service Client
 *
 * <p>用於調用後端的 Keycloak gRPC 服務</p>
 *
 * @author TY Team
 * @version 1.0
 */
@Service
@ConditionalOnProperty(name = "grpc.client.enabled", havingValue = "true")
public class KeycloakGrpcClient {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakGrpcClient.class);

    @Value("${grpc.client.backend.host:localhost}")
    private String backendHost;

    @Value("${grpc.client.backend.port:50051}")
    private int backendPort;

    private ManagedChannel channel;
    private KeycloakServiceGrpc.KeycloakServiceBlockingStub blockingStub;

    @PostConstruct
    public void init() {
        logger.info("🚀 初始化 gRPC Keycloak Client，連接後端: {}:{}", backendHost, backendPort);

        channel = ManagedChannelBuilder.forAddress(backendHost, backendPort)
                .usePlaintext()  // 開發環境使用明文連接
                .build();

        blockingStub = KeycloakServiceGrpc.newBlockingStub(channel);

        logger.info("✅ gRPC Keycloak Client 初始化完成");
    }

    @PreDestroy
    public void destroy() {
        logger.info("⏹️ 關閉 gRPC Keycloak Client");
        if (channel != null && !channel.isShutdown()) {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.error("❌ 關閉 gRPC 通道時發生錯誤", e);
                channel.shutdownNow();
            }
        }
    }

    /**
     * 處理 OAuth2 重定向
     */
    public AuthRedirectResponse processAuthRedirect(String code, String redirectUri) {
        logger.info("📥 gRPC Client: 請求處理 OAuth2 重定向，code={}", code.substring(0, 20) + "...");

        try {
            AuthRedirectRequest request = AuthRedirectRequest.newBuilder()
                    .setCode(code)
                    .setRedirectUri(redirectUri)
                    .build();

            AuthRedirectResponse response = blockingStub.processAuthRedirect(request);

            if (response.getSuccess()) {
                logger.info("✅ gRPC Client: 成功處理 OAuth2 重定向");
            } else {
                logger.error("❌ gRPC Client: OAuth2 重定向處理失敗: {}", response.getMessage());
            }

            return response;

        } catch (Exception e) {
            logger.error("❌ gRPC Client: 處理 OAuth2 重定向失敗", e);
            throw new RuntimeException("Failed to process auth redirect via gRPC: " + e.getMessage(), e);
        }
    }

    /**
     * 用戶登出
     */
    public LogoutResponse logout(String refreshToken) {
        logger.info("📥 gRPC Client: 請求用戶登出，refreshToken={}", refreshToken.substring(0, 20) + "...");

        try {
            LogoutRequest request = LogoutRequest.newBuilder()
                    .setRefreshToken(refreshToken)
                    .build();

            LogoutResponse response = blockingStub.logout(request);

            if (response.getSuccess()) {
                logger.info("✅ gRPC Client: 成功登出");
            } else {
                logger.error("❌ gRPC Client: 登出失敗: {}", response.getMessage());
            }

            return response;

        } catch (Exception e) {
            logger.error("❌ gRPC Client: 登出失敗", e);
            throw new RuntimeException("Failed to logout via gRPC: " + e.getMessage(), e);
        }
    }

    /**
     * Token 驗證與刷新
     */
    public IntrospectTokenResponse introspectToken(String accessToken, String refreshToken) {
        logger.info("📥 gRPC Client: 請求 Token 驗證，accessToken={}", accessToken.substring(0, 20) + "...");

        try {
            IntrospectTokenRequest.Builder requestBuilder = IntrospectTokenRequest.newBuilder()
                    .setAccessToken(accessToken);

            if (refreshToken != null && !refreshToken.isEmpty()) {
                requestBuilder.setRefreshToken(refreshToken);
            }

            IntrospectTokenResponse response = blockingStub.introspectToken(requestBuilder.build());

            if (response.getActive()) {
                logger.info("✅ gRPC Client: Token 驗證成功");
            } else {
                logger.warn("⚠️ gRPC Client: Token 無效: {}", response.getMessage());
            }

            return response;

        } catch (Exception e) {
            logger.error("❌ gRPC Client: Token 驗證失敗", e);
            throw new RuntimeException("Failed to introspect token via gRPC: " + e.getMessage(), e);
        }
    }

    /**
     * 檢查gRPC連接健康狀態
     */
    public boolean isHealthy() {
        try {
            // 嘗試一個簡單的調用來檢查連接
            // 使用一個空的請求來測試連接
            AuthRedirectRequest request = AuthRedirectRequest.newBuilder()
                    .setCode("test")
                    .setRedirectUri("test")
                    .build();
            
            blockingStub.processAuthRedirect(request);
            return true;
        } catch (Exception e) {
            logger.error("❌ gRPC Keycloak 健康檢查失敗", e);
            return false;
        }
    }
}
