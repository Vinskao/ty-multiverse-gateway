package tw.com.tymgateway.grpc.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tw.com.tymgateway.grpc.gallery.*;
import tw.com.tymgateway.dto.GalleryData;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * gRPC Gallery Service Client
 *
 * <p>用於調用後端的 Gallery gRPC 服務</p>
 *
 * @author TY Team
 * @version 1.0
 */
@Service
@ConditionalOnProperty(name = "grpc.client.enabled", havingValue = "true")
public class GalleryGrpcClient {

    private static final Logger logger = LoggerFactory.getLogger(GalleryGrpcClient.class);

    @Value("${grpc.client.backend.host:localhost}")
    private String backendHost;

    @Value("${grpc.client.backend.port:50051}")
    private int backendPort;

    private ManagedChannel channel;

    @PostConstruct
    public void init() {
        logger.info("🚀 初始化 gRPC Gallery Client，連接後端: {}:{}", backendHost, backendPort);

        channel = ManagedChannelBuilder.forAddress(backendHost, backendPort)
                .usePlaintext()  // 開發環境使用明文連接
                .build();

        logger.info("✅ gRPC Gallery Client 初始化完成");
    }

    @PreDestroy
    public void destroy() {
        logger.info("⏹️ 關閉 gRPC Gallery Client");
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
     * 獲取所有圖片
     */
    public List<GalleryData> getAllImages() {
        logger.info("📥 gRPC Client: 請求獲取所有圖片，連接 {}:{}", backendHost, backendPort);

        try {
            if (channel == null || channel.isShutdown()) {
                logger.error("❌ gRPC Client: 連接未初始化或已關閉");
                throw new RuntimeException("gRPC channel is not available");
            }

            logger.info("🔄 gRPC Client: 發送請求到後端...");

            GetAllImagesResponse response = callBackendGetAllImages();

            List<GalleryData> gatewayGalleryList = response.getGalleriesList().stream()
                .map(this::convertProtobufToGateway)
                .collect(java.util.stream.Collectors.toList());

            logger.info("✅ gRPC Client: 成功獲取 {} 個圖片", gatewayGalleryList.size());
            return gatewayGalleryList;

        } catch (Exception e) {
            logger.error("❌ gRPC Client: 獲取所有圖片失敗 - 錯誤類型: {}, 錯誤信息: {}", e.getClass().getSimpleName(), e.getMessage(), e);

            if (e.getMessage() != null && e.getMessage().contains("UNAVAILABLE")) {
                logger.error("❌ gRPC Client: 連接不可用，請檢查後端 gRPC 服務器是否運行在 {}:{}", backendHost, backendPort);
            }

            throw new RuntimeException("Failed to get all images via gRPC: " + e.getMessage(), e);
        }
    }

    /**
     * 根據ID獲取圖片
     */
    public Optional<GalleryData> getImageById(Integer id) {
        logger.info("📥 gRPC Client: 請求獲取圖片，ID: {}", id);

        try {
            GalleryResponse response = callBackendGetImageById(id);

            if (response.getSuccess()) {
                GalleryData gatewayGallery = convertProtobufToGateway(response.getGallery());
                logger.info("✅ gRPC Client: 成功獲取圖片: {}", gatewayGallery.getId());
                return Optional.of(gatewayGallery);
            } else {
                logger.info("⚠️ gRPC Client: 未找到圖片: {}", id);
                return Optional.empty();
            }

        } catch (Exception e) {
            logger.error("❌ gRPC Client: 獲取圖片失敗，ID: {}", id, e);
            throw new RuntimeException("Failed to get image by id via gRPC", e);
        }
    }

    /**
     * 檢查gRPC連接健康狀態
     */
    public boolean isHealthy() {
        try {
            GetAllImagesResponse response = callBackendGetAllImages();
            return response != null;
        } catch (Exception e) {
            logger.error("❌ gRPC Gallery 健康檢查失敗", e);
            return false;
        }
    }

    private GetAllImagesResponse callBackendGetAllImages() {
        try {
            GalleryServiceGrpc.GalleryServiceBlockingStub stub = GalleryServiceGrpc.newBlockingStub(channel);
            GetAllImagesRequest request = GetAllImagesRequest.newBuilder().build();
            GetAllImagesResponse response = stub.getAllImages(request);
            logger.info("✅ gRPC 調用成功，獲取 {} 個圖片", response.getGalleriesCount());
            return response;
        } catch (Exception e) {
            logger.error("❌ gRPC 調用失敗: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call gRPC service: " + e.getMessage(), e);
        }
    }

    private GalleryResponse callBackendGetImageById(Integer id) {
        try {
            GalleryServiceGrpc.GalleryServiceBlockingStub stub = GalleryServiceGrpc.newBlockingStub(channel);
            GetImageByIdRequest request = GetImageByIdRequest.newBuilder().setId(id).build();
            GalleryResponse response = stub.getImageById(request);
            return response;
        } catch (Exception e) {
            logger.error("❌ gRPC 調用失敗: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call gRPC service: " + e.getMessage(), e);
        }
    }

    private GalleryData convertProtobufToGateway(tw.com.tymgateway.grpc.gallery.GalleryData protobufData) {
        GalleryData gatewayData = new GalleryData();

        gatewayData.setId(protobufData.getId());
        gatewayData.setImageBase64(protobufData.getImageBase64());
        gatewayData.setUploadTime(protobufData.getUploadTime());
        gatewayData.setVersion(protobufData.getVersion());

        return gatewayData;
    }
}
