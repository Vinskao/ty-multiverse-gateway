package tw.com.tymgateway.grpc.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tw.com.tymgateway.grpc.weapons.*;
import tw.com.tymgateway.dto.WeaponData;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * gRPC Weapon Service Client
 *
 * <p>用於調用後端的 Weapon gRPC 服務</p>
 *
 * @author TY Team
 * @version 1.0
 */
@Service
@ConditionalOnProperty(name = "grpc.client.enabled", havingValue = "true")
public class WeaponGrpcClient {

    private static final Logger logger = LoggerFactory.getLogger(WeaponGrpcClient.class);

    @Value("${grpc.client.backend.host:localhost}")
    private String backendHost;

    @Value("${grpc.client.backend.port:50051}")
    private int backendPort;

    private ManagedChannel channel;

    @PostConstruct
    public void init() {
        logger.info("🚀 初始化 gRPC Weapon Client，連接後端: {}:{}", backendHost, backendPort);

        channel = ManagedChannelBuilder.forAddress(backendHost, backendPort)
                .usePlaintext()  // 開發環境使用明文連接
                .build();

        logger.info("✅ gRPC Weapon Client 初始化完成");
    }

    @PreDestroy
    public void destroy() {
        logger.info("⏹️ 關閉 gRPC Weapon Client");
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
     * 獲取所有武器
     */
    public List<WeaponData> getAllWeapons() {
        logger.info("📥 gRPC Client: 請求獲取所有武器，連接 {}:{}", backendHost, backendPort);

        try {
            if (channel == null || channel.isShutdown()) {
                logger.error("❌ gRPC Client: 連接未初始化或已關閉");
                throw new RuntimeException("gRPC channel is not available");
            }

            logger.info("🔄 gRPC Client: 發送請求到後端...");

            GetAllWeaponsResponse response = callBackendGetAllWeapons();

            List<WeaponData> gatewayWeaponList = response.getWeaponsList().stream()
                .map(this::convertProtobufToGateway)
                .collect(java.util.stream.Collectors.toList());

            logger.info("✅ gRPC Client: 成功獲取 {} 個武器", gatewayWeaponList.size());
            return gatewayWeaponList;

        } catch (Exception e) {
            logger.error("❌ gRPC Client: 獲取所有武器失敗 - 錯誤類型: {}, 錯誤信息: {}", e.getClass().getSimpleName(), e.getMessage(), e);

            if (e.getMessage() != null && e.getMessage().contains("UNAVAILABLE")) {
                logger.error("❌ gRPC Client: 連接不可用，請檢查後端 gRPC 服務器是否運行在 {}:{}", backendHost, backendPort);
            }

            throw new RuntimeException("Failed to get all weapons via gRPC: " + e.getMessage(), e);
        }
    }

    /**
     * 根據名稱獲取武器
     */
    public Optional<WeaponData> getWeaponById(String name) {
        logger.info("📥 gRPC Client: 請求獲取武器，名稱: {}", name);

        try {
            WeaponResponse response = callBackendGetWeaponById(name);

            if (response.getSuccess()) {
                WeaponData gatewayWeapon = convertProtobufToGateway(response.getWeapon());
                logger.info("✅ gRPC Client: 成功獲取武器: {}", gatewayWeapon.getName());
                return Optional.of(gatewayWeapon);
            } else {
                logger.info("⚠️ gRPC Client: 未找到武器: {}", name);
                return Optional.empty();
            }

        } catch (Exception e) {
            logger.error("❌ gRPC Client: 獲取武器失敗，名稱: {}", name, e);
            throw new RuntimeException("Failed to get weapon by id via gRPC", e);
        }
    }

    /**
     * 檢查gRPC連接健康狀態
     */
    public boolean isHealthy() {
        try {
            GetAllWeaponsResponse response = callBackendGetAllWeapons();
            return response != null;
        } catch (Exception e) {
            logger.error("❌ gRPC Weapon 健康檢查失敗", e);
            return false;
        }
    }

    private GetAllWeaponsResponse callBackendGetAllWeapons() {
        try {
            WeaponServiceGrpc.WeaponServiceBlockingStub stub = WeaponServiceGrpc.newBlockingStub(channel);
            GetAllWeaponsRequest request = GetAllWeaponsRequest.newBuilder().build();
            GetAllWeaponsResponse response = stub.getAllWeapons(request);
            logger.info("✅ gRPC 調用成功，獲取 {} 個武器", response.getWeaponsCount());
            return response;
        } catch (Exception e) {
            logger.error("❌ gRPC 調用失敗: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call gRPC service: " + e.getMessage(), e);
        }
    }

    private WeaponResponse callBackendGetWeaponById(String name) {
        try {
            WeaponServiceGrpc.WeaponServiceBlockingStub stub = WeaponServiceGrpc.newBlockingStub(channel);
            GetWeaponByIdRequest request = GetWeaponByIdRequest.newBuilder().setName(name).build();
            WeaponResponse response = stub.getWeaponById(request);
            return response;
        } catch (Exception e) {
            logger.error("❌ gRPC 調用失敗: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call gRPC service: " + e.getMessage(), e);
        }
    }

    private WeaponData convertProtobufToGateway(tw.com.tymgateway.grpc.weapons.WeaponData protobufData) {
        WeaponData gatewayData = new WeaponData();

        gatewayData.setName(protobufData.getName());
        gatewayData.setOwner(protobufData.getOwner());
        gatewayData.setAttributes(protobufData.getAttributes());
        gatewayData.setBaseDamage(protobufData.getBaseDamage());
        gatewayData.setBonusDamage(protobufData.getBonusDamage());

        gatewayData.setBonusAttributes(protobufData.getBonusAttributesList());
        gatewayData.setStateAttributes(protobufData.getStateAttributesList());

        gatewayData.setCreatedAt(protobufData.getCreatedAt());
        gatewayData.setUpdatedAt(protobufData.getUpdatedAt());
        gatewayData.setVersion(protobufData.getVersion());

        return gatewayData;
    }
}
