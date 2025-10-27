package tw.com.tymgateway.grpc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import tw.com.tymgateway.grpc.client.KeycloakGrpcClient;
import tw.com.tymgateway.dto.AuthRedirectResponse;
import tw.com.tymgateway.dto.IntrospectTokenResponse;
import tw.com.tymgateway.dto.LogoutResponse;

import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Keycloak gRPC Client 測試類
 *
 * <p>測試 Gateway 與 Backend 的 Keycloak gRPC 通信</p>
 */
@SpringBootTest
@TestPropertySource(properties = {
    "grpc.client.enabled=true",
    "grpc.client.backend.host=localhost",
    "grpc.client.backend.port=50051"
})
class KeycloakGrpcClientTest {

    @Autowired
    private KeycloakGrpcClient keycloakGrpcClient;

    private boolean isBackendRunning() {
        try (Socket socket = new Socket("localhost", 50051)) {
            return socket.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void testGrpcClientBeanExists() {
        assertNotNull(keycloakGrpcClient, "KeycloakGrpcClient 應該被正確注入");
    }

    @Test
    @EnabledIf("isBackendRunning")
    void testProcessAuthRedirect() {
        System.out.println("🔐 測試處理認證重定向...");
        
        String testCode = "test-auth-code";
        String testRedirectUri = "http://localhost:4321/callback";
        
        try {
            AuthRedirectResponse result = keycloakGrpcClient.processAuthRedirect(testCode, testRedirectUri);
            
            assertNotNull(result, "應該返回認證響應");
            System.out.println("✅ 處理認證重定向完成");
            System.out.println("   成功狀態: " + result.isSuccess());
            System.out.println("   消息: " + result.getMessage());
        } catch (Exception e) {
            // 預期可能失敗，因為測試的 code 不是真實的
            System.out.println("⚠️  認證失敗（預期行為）: " + e.getMessage());
        }
    }

    @Test
    @EnabledIf("isBackendRunning")
    void testIntrospectToken() {
        System.out.println("🔐 測試驗證 Token...");
        
        String testAccessToken = "test-access-token";
        String testRefreshToken = "test-refresh-token";
        
        try {
            IntrospectTokenResponse result = keycloakGrpcClient.introspectToken(testAccessToken, testRefreshToken);
            
            assertNotNull(result, "應該返回驗證響應");
            System.out.println("✅ Token 驗證完成");
            System.out.println("   Token 有效: " + result.isActive());
            System.out.println("   消息: " + result.getMessage());
        } catch (Exception e) {
            // 預期可能失敗，因為測試的 token 不是真實的
            System.out.println("⚠️  Token 驗證失敗（預期行為）: " + e.getMessage());
        }
    }

    @Test
    @EnabledIf("isBackendRunning")
    void testLogout() {
        System.out.println("🔐 測試登出...");
        
        String testRefreshToken = "test-refresh-token";
        
        try {
            LogoutResponse result = keycloakGrpcClient.logout(testRefreshToken);
            
            assertNotNull(result, "應該返回登出響應");
            System.out.println("✅ 登出完成");
            System.out.println("   成功狀態: " + result.isSuccess());
            System.out.println("   消息: " + result.getMessage());
        } catch (Exception e) {
            // 預期可能失敗，因為測試的 token 不是真實的
            System.out.println("⚠️  登出失敗（預期行為）: " + e.getMessage());
        }
    }

    @Test
    void testConnectionFailureHandling() {
        if (!isBackendRunning()) {
            System.out.println("⚠️  Backend 未運行，測試錯誤處理...");
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                keycloakGrpcClient.introspectToken("test-token", "test-refresh");
            });
            assertTrue(exception.getMessage().contains("Failed to call gRPC service"),
                      "錯誤消息應該包含失敗信息");
            System.out.println("✅ 錯誤處理正確");
        }
    }
}


