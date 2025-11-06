package tw.com.tymgateway.grpc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import tw.com.tymgateway.grpc.client.GalleryGrpcClient;
import tw.com.tymgateway.dto.GalleryData;

import java.net.Socket;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gallery gRPC Client 測試類
 *
 * <p>測試 Gateway 與 Backend 的 Gallery gRPC 通信</p>
 */
@SpringBootTest
@TestPropertySource(properties = {
    "grpc.client.enabled=true",
    "grpc.client.backend.host=localhost",
    "grpc.client.backend.port=50051"
})
class GalleryGrpcClientTest {

    @Autowired
    private GalleryGrpcClient galleryGrpcClient;

    private boolean isBackendRunning() {
        try (Socket socket = new Socket("localhost", 50051)) {
            return socket.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void testGrpcClientBeanExists() {
        assertNotNull(galleryGrpcClient, "GalleryGrpcClient 應該被正確注入");
    }

    @Test
    @EnabledIf("isBackendRunning")
    void testGetAllImages() {
        System.out.println("🖼️  測試獲取所有圖片...");
        List<GalleryData> imageList = galleryGrpcClient.getAllImages();
        assertNotNull(imageList, "應該返回圖片列表");
        System.out.println("✅ 獲取所有圖片成功，數量: " + imageList.size());
        
        if (!imageList.isEmpty()) {
            GalleryData firstImage = imageList.get(0);
            System.out.println("   第一張圖片 ID: " + firstImage.getId());
        }
    }

    @Test
    @EnabledIf("isBackendRunning")
    void testGetImageById() {
        System.out.println("🖼️  測試根據 ID 獲取圖片...");
        
        // 先獲取所有圖片，找一個存在的圖片 ID
        List<GalleryData> imageList = galleryGrpcClient.getAllImages();
        if (!imageList.isEmpty()) {
            Integer imageId = imageList.get(0).getId();
            System.out.println("   測試圖片 ID: " + imageId);
            
            Optional<GalleryData> result = galleryGrpcClient.getImageById(imageId);
            assertNotNull(result, "應該返回Optional結果");
            assertTrue(result.isPresent(), "應該找到圖片");
            assertEquals(imageId, result.get().getId(), "圖片 ID 應該匹配");
            System.out.println("✅ 根據 ID 獲取圖片成功");
        } else {
            System.out.println("⚠️  數據庫中沒有圖片，跳過測試");
        }
    }

    @Test
    void testConnectionFailureHandling() {
        if (!isBackendRunning()) {
            System.out.println("⚠️  Backend 未運行，測試錯誤處理...");
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                galleryGrpcClient.getAllImages();
            });
            assertTrue(exception.getMessage().contains("Failed to call gRPC service"),
                      "錯誤消息應該包含失敗信息");
            System.out.println("✅ 錯誤處理正確");
        }
    }
}


