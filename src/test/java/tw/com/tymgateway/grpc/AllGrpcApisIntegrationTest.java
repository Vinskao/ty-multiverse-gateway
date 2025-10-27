package tw.com.tymgateway.grpc;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import tw.com.tymgateway.grpc.client.*;
import tw.com.tymgateway.dto.*;

import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 所有 gRPC APIs 集成測試
 *
 * <p>全面測試 Gateway 與 Backend 之間所有 gRPC API 的連通性</p>
 */
@SpringBootTest
@TestPropertySource(properties = {
    "grpc.client.enabled=true",
    "grpc.client.backend.host=localhost",
    "grpc.client.backend.port=50051"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AllGrpcApisIntegrationTest {

    @Autowired
    private PeopleGrpcClient peopleGrpcClient;

    @Autowired
    private WeaponGrpcClient weaponGrpcClient;

    @Autowired
    private GalleryGrpcClient galleryGrpcClient;

    @Autowired
    private CkeditorGrpcClient ckeditorGrpcClient;

    @Autowired
    private DeckofcardsGrpcClient deckofcardsGrpcClient;

    @Autowired
    private KeycloakGrpcClient keycloakGrpcClient;

    private boolean isBackendRunning() {
        try (Socket socket = new Socket("localhost", 50051)) {
            return socket.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    @BeforeAll
    static void beforeAll() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🚀 開始 Gateway → Backend gRPC API 連通性測試");
        System.out.println("=".repeat(80) + "\n");
    }

    @AfterAll
    static void afterAll() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("✅ Gateway → Backend gRPC API 連通性測試完成");
        System.out.println("=".repeat(80) + "\n");
    }

    @Test
    @Order(1)
    void testBackendConnection() {
        System.out.println("🔌 測試 Backend 連接狀態...");
        boolean isRunning = isBackendRunning();
        System.out.println("   Backend 運行狀態: " + (isRunning ? "✅ 運行中" : "❌ 未運行"));
        
        if (!isRunning) {
            System.out.println("\n⚠️  警告: Backend 服務器未運行！");
            System.out.println("   請先啟動 Backend 服務器: cd ty-multiverse-backend && ./mvnw spring-boot:run");
            System.out.println("   集成測試將被跳過。\n");
        }
    }

    @Test
    @Order(2)
    @EnabledIf("isBackendRunning")
    void testPeopleApi() {
        System.out.println("\n👥 測試 People API...");
        
        try {
            List<PeopleData> peopleList = peopleGrpcClient.getAllPeople();
            assertNotNull(peopleList, "People API 應該返回數據");
            System.out.println("   ✅ People API 連通正常 - 獲取到 " + peopleList.size() + " 條記錄");
        } catch (Exception e) {
            fail("❌ People API 連通失敗: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @EnabledIf("isBackendRunning")
    void testWeaponApi() {
        System.out.println("\n🔫 測試 Weapon API...");
        
        try {
            List<WeaponData> weaponList = weaponGrpcClient.getAllWeapons();
            assertNotNull(weaponList, "Weapon API 應該返回數據");
            System.out.println("   ✅ Weapon API 連通正常 - 獲取到 " + weaponList.size() + " 條記錄");
        } catch (Exception e) {
            fail("❌ Weapon API 連通失敗: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @EnabledIf("isBackendRunning")
    void testGalleryApi() {
        System.out.println("\n🖼️  測試 Gallery API...");
        
        try {
            List<GalleryData> imageList = galleryGrpcClient.getAllImages();
            assertNotNull(imageList, "Gallery API 應該返回數據");
            System.out.println("   ✅ Gallery API 連通正常 - 獲取到 " + imageList.size() + " 條記錄");
        } catch (Exception e) {
            fail("❌ Gallery API 連通失敗: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    @EnabledIf("isBackendRunning")
    void testCkeditorApi() {
        System.out.println("\n📝 測試 CKEditor API...");
        
        try {
            CkeditorDTO.GetContentDTO result = ckeditorGrpcClient.getContent("test-page");
            assertNotNull(result, "CKEditor API 應該返回數據");
            System.out.println("   ✅ CKEditor API 連通正常");
        } catch (Exception e) {
            fail("❌ CKEditor API 連通失敗: " + e.getMessage());
        }
    }

    @Test
    @Order(6)
    @EnabledIf("isBackendRunning")
    void testDeckofcardsApi() {
        System.out.println("\n🃏 測試 DeckOfCards API...");
        
        try {
            String testPlayerId = "test-player-" + System.currentTimeMillis();
            DeckofcardsDTO.GameResponseDTO result = deckofcardsGrpcClient.startGame(testPlayerId);
            assertNotNull(result, "DeckOfCards API 應該返回數據");
            assertTrue(result.isSuccess(), "遊戲應該成功開始");
            System.out.println("   ✅ DeckOfCards API 連通正常");
        } catch (Exception e) {
            fail("❌ DeckOfCards API 連通失敗: " + e.getMessage());
        }
    }

    @Test
    @Order(7)
    @EnabledIf("isBackendRunning")
    void testKeycloakApi() {
        System.out.println("\n🔐 測試 Keycloak API...");
        
        try {
            // 測試 Token 驗證（預期會失敗，但連接應該正常）
            IntrospectTokenResponse result = keycloakGrpcClient.introspectToken("test-token", "test-refresh");
            assertNotNull(result, "Keycloak API 應該返回響應");
            System.out.println("   ✅ Keycloak API 連通正常");
        } catch (Exception e) {
            // Token 驗證失敗是預期的，但如果能收到響應說明連接正常
            if (e.getMessage().contains("Failed to call gRPC service")) {
                fail("❌ Keycloak API 連通失敗: " + e.getMessage());
            } else {
                System.out.println("   ✅ Keycloak API 連通正常（Token 驗證失敗是預期行為）");
            }
        }
    }

    @Test
    @Order(8)
    @EnabledIf("isBackendRunning")
    void testAllApisHealthCheck() {
        System.out.println("\n🏥 執行所有 API 健康檢查...");
        
        int totalApis = 6;
        int healthyApis = 0;
        
        // People API
        try {
            peopleGrpcClient.getAllPeople();
            healthyApis++;
            System.out.println("   ✅ People API: 健康");
        } catch (Exception e) {
            System.out.println("   ❌ People API: 異常 - " + e.getMessage());
        }
        
        // Weapon API
        try {
            weaponGrpcClient.getAllWeapons();
            healthyApis++;
            System.out.println("   ✅ Weapon API: 健康");
        } catch (Exception e) {
            System.out.println("   ❌ Weapon API: 異常 - " + e.getMessage());
        }
        
        // Gallery API
        try {
            galleryGrpcClient.getAllImages();
            healthyApis++;
            System.out.println("   ✅ Gallery API: 健康");
        } catch (Exception e) {
            System.out.println("   ❌ Gallery API: 異常 - " + e.getMessage());
        }
        
        // CKEditor API
        try {
            ckeditorGrpcClient.getContent("test-page");
            healthyApis++;
            System.out.println("   ✅ CKEditor API: 健康");
        } catch (Exception e) {
            System.out.println("   ❌ CKEditor API: 異常 - " + e.getMessage());
        }
        
        // DeckOfCards API
        try {
            deckofcardsGrpcClient.startGame("health-check-" + System.currentTimeMillis());
            healthyApis++;
            System.out.println("   ✅ DeckOfCards API: 健康");
        } catch (Exception e) {
            System.out.println("   ❌ DeckOfCards API: 異常 - " + e.getMessage());
        }
        
        // Keycloak API
        try {
            keycloakGrpcClient.introspectToken("test", "test");
            healthyApis++;
            System.out.println("   ✅ Keycloak API: 健康");
        } catch (Exception e) {
            // Keycloak 可能因為無效 token 返回錯誤，但連接正常
            if (!e.getMessage().contains("Failed to call gRPC service")) {
                healthyApis++;
                System.out.println("   ✅ Keycloak API: 健康");
            } else {
                System.out.println("   ❌ Keycloak API: 異常 - " + e.getMessage());
            }
        }
        
        System.out.println("\n📊 健康檢查結果: " + healthyApis + "/" + totalApis + " APIs 正常");
        
        // 至少 80% 的 API 應該正常
        assertTrue(healthyApis >= (totalApis * 0.8), 
                  "至少 " + (totalApis * 0.8) + " 個 API 應該正常，實際: " + healthyApis);
    }
}


