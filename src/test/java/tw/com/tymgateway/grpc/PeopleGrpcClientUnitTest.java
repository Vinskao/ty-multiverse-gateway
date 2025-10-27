package tw.com.tymgateway.grpc;

import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tw.com.tymgateway.grpc.client.PeopleGrpcClient;
import tw.com.tymgateway.dto.PeopleData;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * People gRPC Client 單元測試
 *
 * <p>使用 Mock 測試 gRPC client 的邏輯，不依賴運行中的服務器</p>
 */
class PeopleGrpcClientUnitTest {

    @Mock
    private ManagedChannel mockChannel;

    private PeopleGrpcClient peopleGrpcClient;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);

        // 創建 PeopleGrpcClient 實例並注入 mock channel
        peopleGrpcClient = new PeopleGrpcClient();

        // 使用反射設置私有字段 (為了測試目的)
        Field channelField = PeopleGrpcClient.class.getDeclaredField("channel");
        channelField.setAccessible(true);
        channelField.set(peopleGrpcClient, mockChannel);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void testClientInitialization() {
        assertNotNull(peopleGrpcClient, "PeopleGrpcClient 應該被正確創建");
    }

    @Test
    void testIsHealthyReturnsFalseWhenChannelFails() {
        // 模擬 channel 失敗
        doThrow(new RuntimeException("Connection failed")).when(mockChannel).getState(any());

        // 測試健康檢查
        boolean isHealthy = peopleGrpcClient.isHealthy();
        assertFalse(isHealthy, "當 channel 失敗時應該返回 false");
    }

    @Test
    void testShutdown() {
        // 測試關閉功能
        peopleGrpcClient.shutdown();

        // 驗證 channel 被關閉
        verify(mockChannel, times(1)).shutdown();
    }

    @Test
    void testErrorHandlingInGetAllPeople() {
        // 當調用失敗時應該拋出 RuntimeException
        // 注意：由於我們使用的是真實的 client 邏輯，這裡會實際嘗試連接
        // 所以這個測試在沒有服務器時會失敗，這是預期的

        // 我們可以測試異常處理邏輯是否正確
        // 但由於集成測試的性質，這裡我們主要測試錯誤消息格式

        if (!isBackendRunning()) {
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                peopleGrpcClient.getAllPeople();
            });
            assertNotNull(exception.getMessage(), "異常應該有錯誤消息");
        }
    }

    /**
     * 檢查 Backend 是否運行（用於條件測試）
     */
    private boolean isBackendRunning() {
        try (java.net.Socket socket = new java.net.Socket("localhost", 50051)) {
            return socket.isConnected();
        } catch (Exception e) {
            return false;
        }
    }
}
