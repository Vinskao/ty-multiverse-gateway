package tw.com.tymgateway.grpc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import tw.com.tymgateway.grpc.client.WeaponGrpcClient;
import tw.com.tymgateway.dto.WeaponData;

import java.net.Socket;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Weapon gRPC Client 測試類
 *
 * <p>測試 Gateway 與 Backend 的 Weapon gRPC 通信</p>
 */
@SpringBootTest
@TestPropertySource(properties = {
    "grpc.client.enabled=true",
    "grpc.client.backend.host=localhost",
    "grpc.client.backend.port=50051"
})
class WeaponGrpcClientTest {

    @Autowired
    private WeaponGrpcClient weaponGrpcClient;

    private boolean isBackendRunning() {
        try (Socket socket = new Socket("localhost", 50051)) {
            return socket.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void testGrpcClientBeanExists() {
        assertNotNull(weaponGrpcClient, "WeaponGrpcClient 應該被正確注入");
    }

    @Test
    @EnabledIf("isBackendRunning")
    void testGetAllWeapons() {
        System.out.println("🔫 測試獲取所有武器...");
        List<WeaponData> weaponList = weaponGrpcClient.getAllWeapons();
        assertNotNull(weaponList, "應該返回武器列表");
        System.out.println("✅ 獲取所有武器成功，數量: " + weaponList.size());
        
        if (!weaponList.isEmpty()) {
            WeaponData firstWeapon = weaponList.get(0);
            System.out.println("   第一個武器: " + firstWeapon.getName());
        }
    }

    @Test
    @EnabledIf("isBackendRunning")
    void testGetWeaponByName() {
        System.out.println("🔫 測試根據名稱獲取武器...");
        
        // 先獲取所有武器，找一個存在的武器名稱
        List<WeaponData> weaponList = weaponGrpcClient.getAllWeapons();
        if (!weaponList.isEmpty()) {
            String weaponName = weaponList.get(0).getName();
            System.out.println("   測試武器名稱: " + weaponName);
            
            Optional<WeaponData> result = weaponGrpcClient.getWeaponById(weaponName);
            assertNotNull(result, "應該返回Optional結果");
            assertTrue(result.isPresent(), "應該找到武器");
            assertEquals(weaponName, result.get().getName(), "武器名稱應該匹配");
            System.out.println("✅ 根據名稱獲取武器成功");
        } else {
            System.out.println("⚠️  數據庫中沒有武器，跳過測試");
        }
    }

    @Test
    void testConnectionFailureHandling() {
        if (!isBackendRunning()) {
            System.out.println("⚠️  Backend 未運行，測試錯誤處理...");
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                weaponGrpcClient.getAllWeapons();
            });
            assertTrue(exception.getMessage().contains("Failed to call gRPC service"),
                      "錯誤消息應該包含失敗信息");
            System.out.println("✅ 錯誤處理正確");
        }
    }
}


