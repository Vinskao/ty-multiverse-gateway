package tw.com.tymgateway.grpc.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tw.com.tymgateway.grpc.ckeditor.*;
import tw.com.tymgateway.dto.CkeditorDTO;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * gRPC CKEditor Service Client
 *
 * <p>用於調用後端的 CKEditor gRPC 服務</p>
 *
 * @author TY Team
 * @version 1.0
 */
@Service
@ConditionalOnProperty(name = "grpc.client.enabled", havingValue = "true")
public class CkeditorGrpcClient {

    private static final Logger logger = LoggerFactory.getLogger(CkeditorGrpcClient.class);

    @Value("${grpc.client.backend.host:localhost}")
    private String backendHost;

    @Value("${grpc.client.backend.port:50051}")
    private int backendPort;

    private ManagedChannel channel;

    @PostConstruct
    public void init() {
        logger.info("🚀 初始化 gRPC CKEditor Client，連接後端: {}:{}", backendHost, backendPort);

        channel = ManagedChannelBuilder.forAddress(backendHost, backendPort)
                .usePlaintext()  // 開發環境使用明文連接
                .build();

        logger.info("✅ gRPC CKEditor Client 初始化完成");
    }

    @PreDestroy
    public void destroy() {
        logger.info("⏹️ 關閉 gRPC CKEditor Client");
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
     * 保存內容
     */
    public CkeditorDTO saveContent(String userId, String editor, String content, String token) {
        logger.info("📝 gRPC Client: 請求保存內容，userId={}, editor={}", userId, editor);

        try {
            if (channel == null || channel.isShutdown()) {
                logger.error("❌ gRPC Client: 連接未初始化或已關閉");
                throw new RuntimeException("gRPC channel is not available");
            }

            SaveContentRequest request = SaveContentRequest.newBuilder()
                .setUserId(userId)
                .setEditor(editor)
                .setContent(content)
                .setToken(token)
                .build();

            CkeditorServiceGrpc.CkeditorServiceBlockingStub stub =
                CkeditorServiceGrpc.newBlockingStub(channel);

            SaveContentResponse response = stub.saveContent(request);

            if (response.getSuccess()) {
                CkeditorDTO result = new CkeditorDTO(true, response.getMessage(), response.getEditor(), null);
                logger.info("✅ gRPC Client: 成功保存內容");
                return result;
            } else {
                logger.error("❌ gRPC Client: 保存內容失敗: {}", response.getMessage());
                throw new RuntimeException("Failed to save content: " + response.getMessage());
            }

        } catch (Exception e) {
            logger.error("❌ gRPC Client: 保存內容失敗 - 錯誤類型: {}, 錯誤信息: {}",
                        e.getClass().getSimpleName(), e.getMessage(), e);

            if (e.getMessage() != null && e.getMessage().contains("UNAVAILABLE")) {
                logger.error("❌ gRPC Client: 連接不可用，請檢查後端 gRPC 服務器是否運行在 {}:{}", backendHost, backendPort);
            }

            throw new RuntimeException("Failed to save content via gRPC: " + e.getMessage(), e);
        }
    }

    /**
     * 獲取內容
     */
    public CkeditorDTO getContent(String editor, String token) {
        logger.info("📖 gRPC Client: 請求獲取內容，editor={}", editor);

        try {
            GetContentRequest request = GetContentRequest.newBuilder()
                .setEditor(editor)
                .setToken(token)
                .build();

            CkeditorServiceGrpc.CkeditorServiceBlockingStub stub =
                CkeditorServiceGrpc.newBlockingStub(channel);

            GetContentResponse response = stub.getContent(request);

            if (response.getSuccess()) {
                CkeditorDTO result = new CkeditorDTO(true, response.getContent(), response.getEditor(), null);
                logger.info("✅ gRPC Client: 成功獲取內容");
                return result;
            } else {
                logger.error("❌ gRPC Client: 獲取內容失敗");
                throw new RuntimeException("Failed to get content");
            }

        } catch (Exception e) {
            logger.error("❌ gRPC Client: 獲取內容失敗: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get content via gRPC: " + e.getMessage(), e);
        }
    }

    /**
     * 獲取草稿
     */
    public CkeditorDTO getDraft(String userId, String editor, String token) {
        logger.info("📝 gRPC Client: 請求獲取草稿，userId={}, editor={}", userId, editor);

        try {
            GetDraftRequest request = GetDraftRequest.newBuilder()
                .setUserId(userId)
                .setEditor(editor)
                .setToken(token)
                .build();

            CkeditorServiceGrpc.CkeditorServiceBlockingStub stub =
                CkeditorServiceGrpc.newBlockingStub(channel);

            GetDraftResponse response = stub.getDraft(request);

            if (response.getSuccess()) {
                CkeditorDTO result = new CkeditorDTO(true, response.getContent(),
                                                   response.getEditor(), response.getLastModified());
                logger.info("✅ gRPC Client: 成功獲取草稿");
                return result;
            } else {
                logger.error("❌ gRPC Client: 獲取草稿失敗");
                throw new RuntimeException("Failed to get draft");
            }

        } catch (Exception e) {
            logger.error("❌ gRPC Client: 獲取草稿失敗: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get draft via gRPC: " + e.getMessage(), e);
        }
    }

    /**
     * 保存草稿
     */
    public CkeditorDTO saveDraft(String userId, String editor, String content, String token) {
        logger.info("💾 gRPC Client: 請求保存草稿，userId={}, editor={}", userId, editor);

        try {
            SaveDraftRequest request = SaveDraftRequest.newBuilder()
                .setUserId(userId)
                .setEditor(editor)
                .setContent(content)
                .setToken(token)
                .build();

            CkeditorServiceGrpc.CkeditorServiceBlockingStub stub =
                CkeditorServiceGrpc.newBlockingStub(channel);

            SaveDraftResponse response = stub.saveDraft(request);

            if (response.getSuccess()) {
                CkeditorDTO result = new CkeditorDTO(true, response.getMessage(), editor, null);
                logger.info("✅ gRPC Client: 成功保存草稿");
                return result;
            } else {
                logger.error("❌ gRPC Client: 保存草稿失敗: {}", response.getMessage());
                throw new RuntimeException("Failed to save draft: " + response.getMessage());
            }

        } catch (Exception e) {
            logger.error("❌ gRPC Client: 保存草稿失敗: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save draft via gRPC: " + e.getMessage(), e);
        }
    }

    /**
     * 檢查 gRPC 連接健康狀態
     */
    public boolean isHealthy() {
        try {
            GetContentRequest request = GetContentRequest.newBuilder()
                .setEditor("health-check")
                .setToken("")
                .build();
            CkeditorServiceGrpc.CkeditorServiceBlockingStub stub =
                CkeditorServiceGrpc.newBlockingStub(channel);
            GetContentResponse response = stub.getContent(request);
            return response != null;
        } catch (Exception e) {
            logger.error("❌ gRPC CKEditor 健康檢查失敗", e);
            return false;
        }
    }
}
