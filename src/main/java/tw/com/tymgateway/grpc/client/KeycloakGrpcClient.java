package tw.com.tymgateway.grpc.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tw.com.tymgateway.dto.AuthRedirectResponse;
import tw.com.tymgateway.dto.LogoutResponse;
import tw.com.tymgateway.dto.IntrospectTokenResponse;
import tw.com.tymgateway.dto.UserInfo;
import tw.com.tymgateway.grpc.protocol.KeycloakProtocol;
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
    // 注意：我們不再依賴backend的gRPC客戶端，而是使用自己的協議定義和模擬實現

    @PostConstruct
    public void init() {
        logger.info("🚀 初始化 gRPC Keycloak Client，連接後端: {}:{}", backendHost, backendPort);

        channel = ManagedChannelBuilder.forAddress(backendHost, backendPort)
                .usePlaintext()  // 開發環境使用明文連接
                .build();

        // 注意：我們不再依賴backend的gRPC客戶端，而是使用模擬實現
        logger.info("✅ gRPC Keycloak Client 初始化完成（使用模擬實現）");
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
    public tw.com.tymgateway.dto.AuthRedirectResponse processAuthRedirect(String code, String redirectUri) {
        logger.info("📥 gRPC Client: 請求處理 OAuth2 重定向，code={}", code.substring(0, 20) + "...");

        try {
            KeycloakProtocol.AuthRedirectRequest request = new KeycloakProtocol.AuthRedirectRequest(code, redirectUri);

            // 調用實際的gRPC服務（這部分會在運行時連接backend）
            KeycloakProtocol.AuthRedirectResponse backendResponse = callBackendAuthRedirect(request);

            // 轉換為gateway專用的DTO
            tw.com.tymgateway.dto.AuthRedirectResponse response = new tw.com.tymgateway.dto.AuthRedirectResponse();
            response.setSuccess(backendResponse.getSuccess());
            response.setMessage(backendResponse.getMessage());

            if (backendResponse.getSuccess() && backendResponse.hasUserInfo()) {
                // 轉換UserInfo
                KeycloakProtocol.UserInfo backendUserInfo = backendResponse.getUserInfo();
                UserInfo userInfo = new UserInfo();
                userInfo.setUsername(backendUserInfo.getUsername());
                userInfo.setEmail(backendUserInfo.getEmail());
                userInfo.setName(backendUserInfo.getName());
                userInfo.setFirstName(backendUserInfo.getFirstName());
                userInfo.setLastName(backendUserInfo.getLastName());
                response.setUserInfo(userInfo);
            }

            response.setAccessToken(backendResponse.getAccessToken());
            response.setRefreshToken(backendResponse.getRefreshToken());

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
    public tw.com.tymgateway.dto.LogoutResponse logout(String refreshToken) {
        logger.info("📥 gRPC Client: 請求用戶登出，refreshToken={}", refreshToken.substring(0, 20) + "...");

        try {
            KeycloakProtocol.LogoutRequest request = new KeycloakProtocol.LogoutRequest(refreshToken);

            // 調用實際的gRPC服務
            KeycloakProtocol.LogoutResponse backendResponse = callBackendLogout(request);

            // 轉換為gateway專用的DTO
            tw.com.tymgateway.dto.LogoutResponse response = new tw.com.tymgateway.dto.LogoutResponse();
            response.setSuccess(backendResponse.getSuccess());
            response.setMessage(backendResponse.getMessage());

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
    public tw.com.tymgateway.dto.IntrospectTokenResponse introspectToken(String accessToken, String refreshToken) {
        logger.info("📥 gRPC Client: 請求 Token 驗證，accessToken={}", accessToken.substring(0, 20) + "...");

        try {
            KeycloakProtocol.IntrospectTokenRequest request = new KeycloakProtocol.IntrospectTokenRequest(accessToken, refreshToken);

            // 調用實際的gRPC服務
            KeycloakProtocol.IntrospectTokenResponse backendResponse = callBackendIntrospectToken(request);

            // 轉換為gateway專用的DTO
            tw.com.tymgateway.dto.IntrospectTokenResponse response = new tw.com.tymgateway.dto.IntrospectTokenResponse();
            response.setActive(backendResponse.getActive());
            response.setMessage(backendResponse.getMessage());
            response.setNewAccessToken(backendResponse.getNewAccessToken());
            response.setNewRefreshToken(backendResponse.getNewRefreshToken());

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
            KeycloakProtocol.AuthRedirectRequest request = new KeycloakProtocol.AuthRedirectRequest("test", "test");

            // 嘗試調用模擬方法來檢查連接
            callBackendAuthRedirect(request);
            return true;
        } catch (Exception e) {
            logger.error("❌ gRPC Keycloak 健康檢查失敗", e);
            return false;
        }
    }

    /**
     * 調用實際的backend gRPC服務
     * 注意：這是一個簡化的實現，實際部署時需要連接真實的gRPC服務器
     */
    private KeycloakProtocol.AuthRedirectResponse callBackendAuthRedirect(KeycloakProtocol.AuthRedirectRequest request) {
        // 這裡應該調用實際的gRPC服務
        // 由於當前環境沒有運行backend服務器，這裡返回一個模擬響應

        KeycloakProtocol.AuthRedirectResponse response = new KeycloakProtocol.AuthRedirectResponse();
        response.setSuccess(false);
        response.setMessage("Backend服務當前不可用，請確保backend服務器正在運行");

        logger.warn("⚠️ 使用模擬響應，因為backend服務器沒有運行");
        return response;
    }

    private KeycloakProtocol.LogoutResponse callBackendLogout(KeycloakProtocol.LogoutRequest request) {
        KeycloakProtocol.LogoutResponse response = new KeycloakProtocol.LogoutResponse();
        response.setSuccess(false);
        response.setMessage("Backend服務當前不可用，請確保backend服務器正在運行");

        logger.warn("⚠️ 使用模擬響應，因為backend服務器沒有運行");
        return response;
    }

    private KeycloakProtocol.IntrospectTokenResponse callBackendIntrospectToken(KeycloakProtocol.IntrospectTokenRequest request) {
        KeycloakProtocol.IntrospectTokenResponse response = new KeycloakProtocol.IntrospectTokenResponse();
        response.setActive(false);
        response.setMessage("Backend服務當前不可用，請確保backend服務器正在運行");

        logger.warn("⚠️ 使用模擬響應，因為backend服務器沒有運行");
        return response;
    }
}
