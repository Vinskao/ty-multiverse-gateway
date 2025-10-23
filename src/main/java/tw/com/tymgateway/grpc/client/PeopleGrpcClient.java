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
import tw.com.tymgateway.grpc.people.*;

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

            // 調用真正的 gRPC 服務
            GetAllPeopleResponse response = callBackendGetAllPeople();

            // 轉換為gateway專用的DTO
            List<tw.com.tymgateway.dto.PeopleData> gatewayPeopleList = response.getPeopleList().stream()
                .map(this::convertProtobufToGateway)
                .collect(java.util.stream.Collectors.toList());

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
            // 調用真正的 gRPC 服務
            PeopleResponse response = callBackendGetPeopleByName(name);

            if (response.getSuccess()) {
                // 轉換為gateway專用的DTO
                tw.com.tymgateway.dto.PeopleData gatewayPeople = convertProtobufToGateway(response.getPeople());
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
            // 調用真正的 gRPC 服務
            PeopleResponse response = callBackendInsertPeople(peopleData);

            if (response.getSuccess()) {
                // 將響應轉換為gateway專用的DTO
                tw.com.tymgateway.dto.PeopleData gatewayPeople = convertProtobufToGateway(response.getPeople());
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
            // 調用真正的 gRPC 服務
            PeopleResponse response = callBackendUpdatePeople(name, peopleData);

            if (response.getSuccess()) {
                // 將響應轉換為gateway專用的DTO
                tw.com.tymgateway.dto.PeopleData gatewayPeople = convertProtobufToGateway(response.getPeople());
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
            // 調用真正的 gRPC 服務
            DeletePeopleResponse response = callBackendDeletePeople(name);

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
            GetAllPeopleResponse response = callBackendGetAllPeople();
            return response != null;
        } catch (Exception e) {
            logger.error("❌ gRPC 健康檢查失敗", e);
            return false;
        }
    }


    /**
     * 調用後端 gRPC 服務 - 獲取所有人物
     */
    private GetAllPeopleResponse callBackendGetAllPeople() {
        try {
            // 創建 gRPC stub
            PeopleServiceGrpc.PeopleServiceBlockingStub stub =
                PeopleServiceGrpc.newBlockingStub(channel);

            // 創建請求
            GetAllPeopleRequest request = GetAllPeopleRequest.newBuilder().build();

            // 調用 gRPC 服務
            GetAllPeopleResponse response = stub.getAllPeople(request);

            logger.info("✅ gRPC 調用成功，獲取 {} 個人物", response.getPeopleCount());
            return response;

        } catch (Exception e) {
            logger.error("❌ gRPC 調用失敗: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call gRPC service: " + e.getMessage(), e);
        }
    }

    /**
     * 調用後端 gRPC 服務 - 根據名稱獲取人物
     */
    private PeopleResponse callBackendGetPeopleByName(String name) {
        try {
            // 創建 gRPC stub
            PeopleServiceGrpc.PeopleServiceBlockingStub stub =
                PeopleServiceGrpc.newBlockingStub(channel);

            // 創建請求
            GetPeopleByNameRequest request = GetPeopleByNameRequest.newBuilder().setName(name).build();

            // 調用 gRPC 服務
            PeopleResponse response = stub.getPeopleByName(request);

            logger.info("✅ gRPC 調用成功，獲取人物: {}", name);
            return response;

        } catch (Exception e) {
            logger.error("❌ gRPC 調用失敗: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call gRPC service: " + e.getMessage(), e);
        }
    }

    /**
     * 調用後端 gRPC 服務 - 插入人物
     */
    private PeopleResponse callBackendInsertPeople(tw.com.tymgateway.dto.PeopleData peopleData) {
        try {
            // 創建 gRPC stub
            PeopleServiceGrpc.PeopleServiceBlockingStub stub =
                PeopleServiceGrpc.newBlockingStub(channel);

            // 轉換請求數據
            PeopleData protobufData = convertGatewayToProtobuf(peopleData);

            // 調用 gRPC 服務
            PeopleResponse response = stub.insertPeople(protobufData);

            logger.info("✅ gRPC 調用成功，插入人物: {}", peopleData.getName());
            return response;

        } catch (Exception e) {
            logger.error("❌ gRPC 調用失敗: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call gRPC service: " + e.getMessage(), e);
        }
    }

    /**
     * 調用後端 gRPC 服務 - 更新人物
     */
    private PeopleResponse callBackendUpdatePeople(String name, tw.com.tymgateway.dto.PeopleData peopleData) {
        try {
            // 創建 gRPC stub
            PeopleServiceGrpc.PeopleServiceBlockingStub stub =
                PeopleServiceGrpc.newBlockingStub(channel);

            // 轉換請求數據
            PeopleData protobufData = convertGatewayToProtobuf(peopleData);
            UpdatePeopleRequest request = UpdatePeopleRequest.newBuilder()
                .setName(name)
                .setPeople(protobufData)
                .build();

            // 調用 gRPC 服務
            PeopleResponse response = stub.updatePeople(request);

            logger.info("✅ gRPC 調用成功，更新人物: {}", name);
            return response;

        } catch (Exception e) {
            logger.error("❌ gRPC 調用失敗: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call gRPC service: " + e.getMessage(), e);
        }
    }

    /**
     * 調用後端 gRPC 服務 - 刪除人物
     */
    private DeletePeopleResponse callBackendDeletePeople(String name) {
        try {
            // 創建 gRPC stub
            PeopleServiceGrpc.PeopleServiceBlockingStub stub =
                PeopleServiceGrpc.newBlockingStub(channel);

            // 創建請求
            DeletePeopleRequest request = DeletePeopleRequest.newBuilder().setName(name).build();

            // 調用 gRPC 服務
            DeletePeopleResponse response = stub.deletePeople(request);

            logger.info("✅ gRPC 調用成功，刪除人物: {}", name);
            return response;

        } catch (Exception e) {
            logger.error("❌ gRPC 調用失敗: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call gRPC service: " + e.getMessage(), e);
        }
    }

    /**
     * 將 Protobuf PeopleData 轉換為 Gateway PeopleData
     */
    private tw.com.tymgateway.dto.PeopleData convertProtobufToGateway(PeopleData protobufData) {
        tw.com.tymgateway.dto.PeopleData gatewayData = new tw.com.tymgateway.dto.PeopleData();

        // 基本信息
        gatewayData.setName(protobufData.getName());
        gatewayData.setNameOriginal(protobufData.getNameOriginal());
        gatewayData.setCodeName(protobufData.getCodeName());

        // 力量屬性
        gatewayData.setPhysicPower(protobufData.getPhysicPower());
        gatewayData.setMagicPower(protobufData.getMagicPower());
        gatewayData.setUtilityPower(protobufData.getUtilityPower());

        // 基本信息
        gatewayData.setDob(protobufData.getDob());
        gatewayData.setRace(protobufData.getRace());
        gatewayData.setAttributes(protobufData.getAttributes());
        gatewayData.setGender(protobufData.getGender());
        gatewayData.setProfession(protobufData.getProfession());
        gatewayData.setAge(protobufData.getAge());

        // 身體特徵
        gatewayData.setAssSize(protobufData.getAssSize());
        gatewayData.setBoobsSize(protobufData.getBoobsSize());
        gatewayData.setHeightCm(protobufData.getHeightCm());
        gatewayData.setWeightKg(protobufData.getWeightKg());

        // 職業和技能
        gatewayData.setCombat(protobufData.getCombat());
        gatewayData.setJob(protobufData.getJob());
        gatewayData.setPhysics(protobufData.getPhysics());

        // 個性特徵
        gatewayData.setKnownAs(protobufData.getKnownAs());
        gatewayData.setPersonality(protobufData.getPersonality());
        gatewayData.setInterest(protobufData.getInterest());
        gatewayData.setLikes(protobufData.getLikes());
        gatewayData.setDislikes(protobufData.getDislikes());
        gatewayData.setFavoriteFoods(protobufData.getFavoriteFoods());

        // 關係和組織
        gatewayData.setConcubine(protobufData.getConcubine());
        gatewayData.setFaction(protobufData.getFaction());
        gatewayData.setArmyId(protobufData.getArmyId());
        gatewayData.setArmyName(protobufData.getArmyName());
        gatewayData.setDeptId(protobufData.getDeptId());
        gatewayData.setDeptName(protobufData.getDeptName());
        gatewayData.setOriginArmyId(protobufData.getOriginArmyId());
        gatewayData.setOriginArmyName(protobufData.getOriginArmyName());

        // 其他信息
        gatewayData.setGaveBirth(protobufData.getGaveBirth());
        gatewayData.setEmail(protobufData.getEmail());
        gatewayData.setProxy(protobufData.getProxy());

        // JSON屬性
        gatewayData.setBaseAttributes(protobufData.getBaseAttributes());
        gatewayData.setBonusAttributes(protobufData.getBonusAttributes());
        gatewayData.setStateAttributes(protobufData.getStateAttributes());

        // 元數據
        gatewayData.setCreatedAt(protobufData.getCreatedAt());
        gatewayData.setUpdatedAt(protobufData.getUpdatedAt());
        gatewayData.setVersion(protobufData.getVersion());

        return gatewayData;
    }

    /**
     * 將 Gateway PeopleData 轉換為 Protobuf PeopleData
     */
    private PeopleData convertGatewayToProtobuf(tw.com.tymgateway.dto.PeopleData gatewayData) {
        PeopleData.Builder builder = PeopleData.newBuilder();

        // 基本信息
        if (gatewayData.getName() != null) builder.setName(gatewayData.getName());
        if (gatewayData.getNameOriginal() != null) builder.setNameOriginal(gatewayData.getNameOriginal());
        if (gatewayData.getCodeName() != null) builder.setCodeName(gatewayData.getCodeName());

        // 力量屬性
        if (gatewayData.getPhysicPower() != null) builder.setPhysicPower(gatewayData.getPhysicPower());
        if (gatewayData.getMagicPower() != null) builder.setMagicPower(gatewayData.getMagicPower());
        if (gatewayData.getUtilityPower() != null) builder.setUtilityPower(gatewayData.getUtilityPower());

        // 基本信息
        if (gatewayData.getDob() != null) builder.setDob(gatewayData.getDob());
        if (gatewayData.getRace() != null) builder.setRace(gatewayData.getRace());
        if (gatewayData.getAttributes() != null) builder.setAttributes(gatewayData.getAttributes());
        if (gatewayData.getGender() != null) builder.setGender(gatewayData.getGender());
        if (gatewayData.getProfession() != null) builder.setProfession(gatewayData.getProfession());
        if (gatewayData.getAge() != null) builder.setAge(gatewayData.getAge());

        // 身體特徵
        if (gatewayData.getAssSize() != null) builder.setAssSize(gatewayData.getAssSize());
        if (gatewayData.getBoobsSize() != null) builder.setBoobsSize(gatewayData.getBoobsSize());
        if (gatewayData.getHeightCm() != null) builder.setHeightCm(gatewayData.getHeightCm());
        if (gatewayData.getWeightKg() != null) builder.setWeightKg(gatewayData.getWeightKg());

        // 職業和技能
        if (gatewayData.getCombat() != null) builder.setCombat(gatewayData.getCombat());
        if (gatewayData.getJob() != null) builder.setJob(gatewayData.getJob());
        if (gatewayData.getPhysics() != null) builder.setPhysics(gatewayData.getPhysics());

        // 個性特徵
        if (gatewayData.getKnownAs() != null) builder.setKnownAs(gatewayData.getKnownAs());
        if (gatewayData.getPersonality() != null) builder.setPersonality(gatewayData.getPersonality());
        if (gatewayData.getInterest() != null) builder.setInterest(gatewayData.getInterest());
        if (gatewayData.getLikes() != null) builder.setLikes(gatewayData.getLikes());
        if (gatewayData.getDislikes() != null) builder.setDislikes(gatewayData.getDislikes());
        if (gatewayData.getFavoriteFoods() != null) builder.setFavoriteFoods(gatewayData.getFavoriteFoods());

        // 關係和組織
        if (gatewayData.getConcubine() != null) builder.setConcubine(gatewayData.getConcubine());
        if (gatewayData.getFaction() != null) builder.setFaction(gatewayData.getFaction());
        if (gatewayData.getArmyId() != null) builder.setArmyId(gatewayData.getArmyId());
        if (gatewayData.getArmyName() != null) builder.setArmyName(gatewayData.getArmyName());
        if (gatewayData.getDeptId() != null) builder.setDeptId(gatewayData.getDeptId());
        if (gatewayData.getDeptName() != null) builder.setDeptName(gatewayData.getDeptName());
        if (gatewayData.getOriginArmyId() != null) builder.setOriginArmyId(gatewayData.getOriginArmyId());
        if (gatewayData.getOriginArmyName() != null) builder.setOriginArmyName(gatewayData.getOriginArmyName());

        // 其他信息
        if (gatewayData.getGaveBirth() != null) builder.setGaveBirth(gatewayData.getGaveBirth());
        if (gatewayData.getEmail() != null) builder.setEmail(gatewayData.getEmail());
        if (gatewayData.getProxy() != null) builder.setProxy(gatewayData.getProxy());

        // JSON屬性
        if (gatewayData.getBaseAttributes() != null) builder.setBaseAttributes(gatewayData.getBaseAttributes());
        if (gatewayData.getBonusAttributes() != null) builder.setBonusAttributes(gatewayData.getBonusAttributes());
        if (gatewayData.getStateAttributes() != null) builder.setStateAttributes(gatewayData.getStateAttributes());

        // 元數據
        if (gatewayData.getCreatedAt() != null) builder.setCreatedAt(gatewayData.getCreatedAt());
        if (gatewayData.getUpdatedAt() != null) builder.setUpdatedAt(gatewayData.getUpdatedAt());
        if (gatewayData.getVersion() != null) builder.setVersion(gatewayData.getVersion());

        return builder.build();
    }
}
