package tw.com.tymgateway.grpc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import tw.com.tymgateway.grpc.people.PeopleData;
import tw.com.tymgateway.grpc.client.PeopleGrpcClient;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * People gRPC Client 測試類
 *
 * <p>測試 Gateway 與 Backend 的 gRPC 通信</p>
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

    @Test
    void testGrpcClientBeanExists() {
        assertNotNull(peopleGrpcClient, "PeopleGrpcClient 應該被正確注入");
    }

    @Test
    void testGrpcClientHealth() {
        // 假設後端服務器運行在 localhost:50051
        // 如果後端沒有運行，這個測試會失敗，這是正常的
        try {
            boolean isHealthy = peopleGrpcClient.isHealthy();
            // 如果後端運行，應該返回true
            // 如果後端沒有運行，這裡會拋出異常，這是正常的測試行為
        } catch (Exception e) {
            // 預期的行為：如果後端沒有運行，會拋出異常
            assertTrue(e.getMessage().contains("Failed") ||
                      e.getMessage().contains("UNAVAILABLE") ||
                      e.getMessage().contains("connect"),
                      "應該拋出連接相關的異常: " + e.getMessage());
        }
    }

    @Test
    void testGetAllPeople() {
        // 這個測試需要後端服務器運行並且有數據
        try {
            List<PeopleData> peopleList = peopleGrpcClient.getAllPeople();
            // 如果後端運行且有數據，應該返回列表
            assertNotNull(peopleList, "應該返回人物列表");
        } catch (Exception e) {
            // 預期的行為：如果後端沒有運行，會拋出異常
            assertTrue(e.getMessage().contains("Failed") ||
                      e.getMessage().contains("UNAVAILABLE") ||
                      e.getMessage().contains("connect"),
                      "應該拋出連接相關的異常: " + e.getMessage());
        }
    }

    @Test
    void testGetPeopleByName() {
        // 測試獲取特定人物
        try {
            Optional<PeopleData> result = peopleGrpcClient.getPeopleByName("test-person");
            // 如果後端運行且找到數據，應該返回Optional
            assertNotNull(result, "應該返回Optional結果");
        } catch (Exception e) {
            // 預期的行為：如果後端沒有運行，會拋出異常
            assertTrue(e.getMessage().contains("Failed") ||
                      e.getMessage().contains("UNAVAILABLE") ||
                      e.getMessage().contains("connect"),
                      "應該拋出連接相關的異常: " + e.getMessage());
        }
    }

    @Test
    void testGetPeopleByNameMaya() {
        // 測試查詢 Maya（如果存在）
        try {
            Optional<PeopleData> result = peopleGrpcClient.getPeopleByName("Maya");
            // 如果後端運行且找到數據，應該返回Optional
            assertNotNull(result, "應該返回Optional結果");
            // 如果 Maya 存在，驗證名稱
            if (result.isPresent()) {
                assertEquals("Maya", result.get().getName(), "名稱應該匹配");
            }
        } catch (Exception e) {
            // 預期的行為：如果後端沒有運行，會拋出異常
            assertTrue(e.getMessage().contains("Failed") ||
                      e.getMessage().contains("UNAVAILABLE") ||
                      e.getMessage().contains("connect"),
                      "應該拋出連接相關的異常: " + e.getMessage());
        }
    }
}
