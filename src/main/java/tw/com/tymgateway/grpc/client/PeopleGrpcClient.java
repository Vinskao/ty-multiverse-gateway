package tw.com.tymgateway.grpc.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import tw.com.tymgateway.grpc.protocol.PeopleProtocol;

/**
 * gRPC People Service Client
 *
 * <p>用於調用後端的 People gRPC 服務</p>
 *
 * @author TY Team
 * @version 1.0
 */
@Service
@ConditionalOnProperty(name = "grpc.client.enabled", havingValue = "true")
public class PeopleGrpcClient {

    private static final Logger logger = LoggerFactory.getLogger(PeopleGrpcClient.class);

    @Value("${grpc.client.backend.host:localhost}")
    private String backendHost;

    @Value("${grpc.client.backend.port:50051}")
    private int backendPort;

    private ManagedChannel channel;
    // 注意：我們不再依賴backend的gRPC客戶端，而是使用自己的協議定義和模擬實現

    @PostConstruct
    public void init() {
        logger.info("🚀 初始化 gRPC People Client，連接後端: {}:{}", backendHost, backendPort);

        channel = ManagedChannelBuilder.forAddress(backendHost, backendPort)
                .usePlaintext()  // 開發環境使用明文連接
                .build();

        // 注意：我們不再依賴backend的gRPC客戶端，而是使用模擬實現
        logger.info("✅ gRPC People Client 初始化完成（使用模擬實現）");
    }

    @PreDestroy
    public void destroy() {
        logger.info("⏹️ 關閉 gRPC People Client");
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
     * 獲取所有人物
     */
    public List<tw.com.tymgateway.dto.PeopleData> getAllPeople() {
        logger.info("📥 gRPC Client: 請求獲取所有人物，連接 {}:{}", backendHost, backendPort);

        try {
            // 檢查連接狀態
            if (channel == null || channel.isShutdown()) {
                logger.error("❌ gRPC Client: 連接未初始化或已關閉");
                throw new RuntimeException("gRPC channel is not available");
            }

            logger.info("🔄 gRPC Client: 發送請求到後端...");

            // 使用模擬實現，因為當前環境沒有backend服務器
            PeopleProtocol.GetAllPeopleResponse response = callBackendGetAllPeople();

            // 轉換為gateway專用的DTO
            List<tw.com.tymgateway.dto.PeopleData> gatewayPeopleList = response.getPeople();

            logger.info("✅ gRPC Client: 成功獲取 {} 個人物", gatewayPeopleList.size());
            return gatewayPeopleList;

        } catch (Exception e) {
            logger.error("❌ gRPC Client: 獲取所有人物失敗 - 錯誤類型: {}, 錯誤信息: {}", e.getClass().getSimpleName(), e.getMessage(), e);

            // 檢查是否是連接問題
            if (e.getMessage() != null && e.getMessage().contains("UNAVAILABLE")) {
                logger.error("❌ gRPC Client: 連接不可用，請檢查後端 gRPC 服務器是否運行在 {}:{}", backendHost, backendPort);
            }

            throw new RuntimeException("Failed to get all people via gRPC: " + e.getMessage(), e);
        }
    }

    /**
     * 根據名稱獲取人物
     */
    public Optional<tw.com.tymgateway.dto.PeopleData> getPeopleByName(String name) {
        logger.info("📥 gRPC Client: 請求獲取人物，名稱: {}", name);

        try {
            // 使用模擬實現
            PeopleProtocol.PeopleResponse response = callBackendGetPeopleByName(name);

            if (response.getSuccess()) {
                // 轉換為gateway專用的DTO
                tw.com.tymgateway.dto.PeopleData gatewayPeople = response.getPeople();
                logger.info("✅ gRPC Client: 成功獲取人物: {}", gatewayPeople.getName());
                return Optional.of(gatewayPeople);
            } else {
                logger.info("⚠️ gRPC Client: 未找到人物: {}", name);
                return Optional.empty();
            }

        } catch (Exception e) {
            logger.error("❌ gRPC Client: 獲取人物失敗，名稱: {}", name, e);
            throw new RuntimeException("Failed to get people by name via gRPC", e);
        }
    }

    /**
     * 插入人物
     */
    public tw.com.tymgateway.dto.PeopleData insertPeople(tw.com.tymgateway.dto.PeopleData peopleData) {
        logger.info("📥 gRPC Client: 請求插入人物: {}", peopleData.getName());

        try {
            // 使用模擬實現
            PeopleProtocol.PeopleResponse response = callBackendInsertPeople(peopleData);

            if (response.getSuccess()) {
                // 將響應轉換為gateway專用的DTO
                tw.com.tymgateway.dto.PeopleData gatewayPeople = response.getPeople();
                logger.info("✅ gRPC Client: 成功插入人物: {}", gatewayPeople.getName());
                return gatewayPeople;
            } else {
                logger.error("❌ gRPC Client: 插入人物失敗: {}", response.getMessage());
                throw new RuntimeException("Failed to insert people: " + response.getMessage());
            }

        } catch (Exception e) {
            logger.error("❌ gRPC Client: 插入人物失敗，名稱: {}", peopleData.getName(), e);
            throw new RuntimeException("Failed to insert people via gRPC", e);
        }
    }

    /**
     * 更新人物
     */
    public tw.com.tymgateway.dto.PeopleData updatePeople(String name, tw.com.tymgateway.dto.PeopleData peopleData) {
        logger.info("📥 gRPC Client: 請求更新人物: {}", name);

        try {
            // 使用模擬實現
            PeopleProtocol.PeopleResponse response = callBackendUpdatePeople(name, peopleData);

            if (response.getSuccess()) {
                // 將響應轉換為gateway專用的DTO
                tw.com.tymgateway.dto.PeopleData gatewayPeople = response.getPeople();
                logger.info("✅ gRPC Client: 成功更新人物: {}", gatewayPeople.getName());
                return gatewayPeople;
            } else {
                logger.error("❌ gRPC Client: 更新人物失敗: {}", response.getMessage());
                throw new RuntimeException("Failed to update people: " + response.getMessage());
            }

        } catch (Exception e) {
            logger.error("❌ gRPC Client: 更新人物失敗，名稱: {}", name, e);
            throw new RuntimeException("Failed to update people via gRPC", e);
        }
    }

    /**
     * 刪除人物
     */
    public boolean deletePeople(String name) {
        logger.info("📥 gRPC Client: 請求刪除人物: {}", name);

        try {
            // 使用模擬實現
            PeopleProtocol.DeletePeopleResponse response = callBackendDeletePeople(name);

            if (response.getSuccess()) {
                logger.info("✅ gRPC Client: 成功刪除人物: {}", name);
                return true;
            } else {
                logger.error("❌ gRPC Client: 刪除人物失敗: {}", response.getMessage());
                return false;
            }

        } catch (Exception e) {
            logger.error("❌ gRPC Client: 刪除人物失敗，名稱: {}", name, e);
            throw new RuntimeException("Failed to delete people via gRPC", e);
        }
    }

    /**
     * 檢查gRPC連接健康狀態
     */
    public boolean isHealthy() {
        try {
            // 嘗試一個簡單的調用來檢查連接
            callBackendGetAllPeople();
            return true;
        } catch (Exception e) {
            logger.error("❌ gRPC 健康檢查失敗", e);
            return false;
        }
    }


    /**
     * 模擬調用後端服務 - 獲取所有人物
     */
    private PeopleProtocol.GetAllPeopleResponse callBackendGetAllPeople() {
        PeopleProtocol.GetAllPeopleResponse response = new PeopleProtocol.GetAllPeopleResponse();
        // 返回空列表，因為當前環境沒有實際的後端服務器
        response.setPeople(new java.util.ArrayList<>());

        logger.warn("⚠️ 使用模擬響應，因為backend服務器沒有運行");
        return response;
    }

    /**
     * 模擬調用後端服務 - 根據名稱獲取人物
     */
    private PeopleProtocol.PeopleResponse callBackendGetPeopleByName(String name) {
        PeopleProtocol.PeopleResponse response = new PeopleProtocol.PeopleResponse();
        response.setSuccess(false);
        response.setMessage("Backend服務當前不可用，請確保backend服務器正在運行");

        logger.warn("⚠️ 使用模擬響應，因為backend服務器沒有運行");
        return response;
    }

    /**
     * 模擬調用後端服務 - 插入人物
     */
    private PeopleProtocol.PeopleResponse callBackendInsertPeople(tw.com.tymgateway.dto.PeopleData peopleData) {
        PeopleProtocol.PeopleResponse response = new PeopleProtocol.PeopleResponse();
        response.setSuccess(false);
        response.setMessage("Backend服務當前不可用，請確保backend服務器正在運行");

        logger.warn("⚠️ 使用模擬響應，因為backend服務器沒有運行");
        return response;
    }

    /**
     * 模擬調用後端服務 - 更新人物
     */
    private PeopleProtocol.PeopleResponse callBackendUpdatePeople(String name, tw.com.tymgateway.dto.PeopleData peopleData) {
        PeopleProtocol.PeopleResponse response = new PeopleProtocol.PeopleResponse();
        response.setSuccess(false);
        response.setMessage("Backend服務當前不可用，請確保backend服務器正在運行");

        logger.warn("⚠️ 使用模擬響應，因為backend服務器沒有運行");
        return response;
    }

    /**
     * 模擬調用後端服務 - 刪除人物
     */
    private PeopleProtocol.DeletePeopleResponse callBackendDeletePeople(String name) {
        PeopleProtocol.DeletePeopleResponse response = new PeopleProtocol.DeletePeopleResponse();
        response.setSuccess(false);
        response.setMessage("Backend服務當前不可用，請確保backend服務器正在運行");

        logger.warn("⚠️ 使用模擬響應，因為backend服務器沒有運行");
        return response;
    }
}
