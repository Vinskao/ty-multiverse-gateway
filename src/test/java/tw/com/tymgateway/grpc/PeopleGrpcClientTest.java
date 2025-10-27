package tw.com.tymgateway.grpc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import tw.com.tymgateway.grpc.client.PeopleGrpcClient;
import tw.com.tymgateway.dto.PeopleData;

import java.net.Socket;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * People gRPC Client 測試類
 *
 * <p>測試 Gateway 與 Backend 的 gRPC 通信</p>
 * <p>注意：集成測試只在 Backend 服務器運行時執行</p>
 */
@SpringBootTest
@TestPropertySource(properties = {
    "grpc.client.enabled=true",
    "grpc.client.backend.host=localhost",
    "grpc.client.backend.port=50051"
})
class PeopleGrpcClientTest {

    @Autowired
    private PeopleGrpcClient peopleGrpcClient;

    /**
     * 檢查 Backend gRPC 服務器是否運行
     */
    private boolean isBackendRunning() {
        try (Socket socket = new Socket("localhost", 50051)) {
            return socket.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void testGrpcClientBeanExists() {
        assertNotNull(peopleGrpcClient, "PeopleGrpcClient 應該被正確注入");
    }

    @Test
    @EnabledIf("isBackendRunning")
    void testGrpcClientHealth() {
        // 只在 Backend 服務器運行時執行
        boolean isHealthy = peopleGrpcClient.isHealthy();
        assertTrue(isHealthy, "gRPC 服務應該健康運行");
    }

    @Test
    @EnabledIf("isBackendRunning")
    void testGetAllPeople() {
        // 只在 Backend 服務器運行時執行
        List<PeopleData> peopleList = peopleGrpcClient.getAllPeople();
        assertNotNull(peopleList, "應該返回人物列表");
        // 可以添加更多斷言來驗證數據
    }

    @Test
    @EnabledIf("isBackendRunning")
    void testGetPeopleByName() {
        // 只在 Backend 服務器運行時執行
        Optional<PeopleData> result = peopleGrpcClient.getPeopleByName("test-person");
        assertNotNull(result, "應該返回Optional結果");
        // 可以添加更多斷言來驗證具體數據
    }

    @Test
    @EnabledIf("isBackendRunning")
    void testInsertPeople() {
        // 創建測試數據
        PeopleData testPeople = new PeopleData();
        testPeople.setName("Maya");
        testPeople.setNameOriginal("Maya");
        testPeople.setRace("人類");
        testPeople.setGender("女");
        testPeople.setAge(25);
        testPeople.setHeightCm(165);
        testPeople.setWeightKg(55);

        PeopleData result = peopleGrpcClient.insertPeople(testPeople);
        assertNotNull(result, "應該返回插入後的人物數據");
        assertEquals("Maya", result.getName(), "名稱應該匹配");
    }

    @Test
    void testGrpcClientWithoutBackend() {
        // 這個測試總是運行，用來驗證當 Backend 不運行時的行為
        if (!isBackendRunning()) {
            // 如果 Backend 沒有運行，應該拋出異常
            assertThrows(RuntimeException.class, () -> {
                peopleGrpcClient.getAllPeople();
            }, "當 Backend 沒有運行時應該拋出異常");
        }
    }

    @Test
    void testConnectionFailureHandling() {
        // 測試連接失敗時的錯誤處理
        if (!isBackendRunning()) {
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                peopleGrpcClient.getAllPeople();
            });
            assertTrue(exception.getMessage().contains("Failed to call gRPC service"),
                      "錯誤消息應該包含失敗信息: " + exception.getMessage());
        }
    }
}
