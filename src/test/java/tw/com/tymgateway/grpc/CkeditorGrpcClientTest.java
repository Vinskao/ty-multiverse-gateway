package tw.com.tymgateway.grpc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import tw.com.tymgateway.grpc.client.CkeditorGrpcClient;
import tw.com.tymgateway.dto.CkeditorDTO;

import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CKEditor gRPC Client 測試類
 *
 * <p>測試 Gateway 與 Backend 的 CKEditor gRPC 通信</p>
 */
@SpringBootTest
@TestPropertySource(properties = {
    "grpc.client.enabled=true",
    "grpc.client.backend.host=localhost",
    "grpc.client.backend.port=50051"
})
class CkeditorGrpcClientTest {

    @Autowired
    private CkeditorGrpcClient ckeditorGrpcClient;

    private boolean isBackendRunning() {
        try (Socket socket = new Socket("localhost", 50051)) {
            return socket.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void testGrpcClientBeanExists() {
        assertNotNull(ckeditorGrpcClient, "CkeditorGrpcClient 應該被正確注入");
    }

    @Test
    @EnabledIf("isBackendRunning")
    void testGetContent() {
        System.out.println("📝 測試獲取內容...");
        
        String testPageId = "test-page";
        CkeditorDTO.GetContentDTO result = ckeditorGrpcClient.getContent(testPageId);
        
        assertNotNull(result, "應該返回內容結果");
        System.out.println("✅ 獲取內容成功");
        System.out.println("   頁面 ID: " + testPageId);
        System.out.println("   成功狀態: " + result.isSuccess());
    }

    @Test
    @EnabledIf("isBackendRunning")
    void testSaveContent() {
        System.out.println("📝 測試保存內容...");
        
        String testPageId = "test-page";
        String testContent = "<p>這是測試內容</p>";
        
        CkeditorDTO.EditContentVO editContent = new CkeditorDTO.EditContentVO();
        editContent.setPageId(testPageId);
        editContent.setContent(testContent);
        
        boolean result = ckeditorGrpcClient.saveContent(editContent);
        
        assertTrue(result, "保存應該成功");
        System.out.println("✅ 保存內容成功");
    }

    @Test
    @EnabledIf("isBackendRunning")
    void testGetDraft() {
        System.out.println("📝 測試獲取草稿...");
        
        String testPageId = "test-page";
        CkeditorDTO.GetContentDTO result = ckeditorGrpcClient.getDraft(testPageId);
        
        assertNotNull(result, "應該返回草稿結果");
        System.out.println("✅ 獲取草稿成功");
        System.out.println("   成功狀態: " + result.isSuccess());
    }

    @Test
    @EnabledIf("isBackendRunning")
    void testSaveDraft() {
        System.out.println("📝 測試保存草稿...");
        
        String testPageId = "test-page";
        String testContent = "<p>這是草稿內容</p>";
        
        CkeditorDTO.EditContentVO editContent = new CkeditorDTO.EditContentVO();
        editContent.setPageId(testPageId);
        editContent.setContent(testContent);
        
        boolean result = ckeditorGrpcClient.saveDraft(editContent);
        
        assertTrue(result, "保存草稿應該成功");
        System.out.println("✅ 保存草稿成功");
    }

    @Test
    void testConnectionFailureHandling() {
        if (!isBackendRunning()) {
            System.out.println("⚠️  Backend 未運行，測試錯誤處理...");
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                ckeditorGrpcClient.getContent("test-page");
            });
            assertTrue(exception.getMessage().contains("Failed to call gRPC service"),
                      "錯誤消息應該包含失敗信息");
            System.out.println("✅ 錯誤處理正確");
        }
    }
}


