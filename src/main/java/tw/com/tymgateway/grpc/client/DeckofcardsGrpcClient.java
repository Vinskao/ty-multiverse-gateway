package tw.com.tymgateway.grpc.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tw.com.tymgateway.grpc.deckofcards.*;
import tw.com.tymgateway.dto.DeckofcardsDTO;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * gRPC Deckofcards Service Client
 *
 * <p>用於調用後端的 Deckofcards gRPC 服務</p>
 *
 * @author TY Team
 * @version 1.0
 */
@Service
@ConditionalOnProperty(name = "grpc.client.enabled", havingValue = "true")
public class DeckofcardsGrpcClient {

    private static final Logger logger = LoggerFactory.getLogger(DeckofcardsGrpcClient.class);

    @Value("${grpc.client.backend.host:localhost}")
    private String backendHost;

    @Value("${grpc.client.backend.port:50051}")
    private int backendPort;

    private ManagedChannel channel;

    @PostConstruct
    public void init() {
        logger.info("🚀 初始化 gRPC Deckofcards Client，連接後端: {}:{}", backendHost, backendPort);

        channel = ManagedChannelBuilder.forAddress(backendHost, backendPort)
                .usePlaintext()  // 開發環境使用明文連接
                .build();

        logger.info("✅ gRPC Deckofcards Client 初始化完成");
    }

    @PreDestroy
    public void destroy() {
        logger.info("⏹️ 關閉 gRPC Deckofcards Client");
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
     * 開始新遊戲
     */
    public DeckofcardsDTO startGame() {
        logger.info("📥 gRPC Client: 請求開始新遊戲，連接 {}:{}", backendHost, backendPort);

        try {
            if (channel == null || channel.isShutdown()) {
                logger.error("❌ gRPC Client: 連接未初始化或已關閉");
                throw new RuntimeException("gRPC channel is not available");
            }

            logger.info("🔄 gRPC Client: 發送請求到後端...");

            StartGameRequest request = StartGameRequest.newBuilder().build();
            DeckofcardsServiceGrpc.DeckofcardsServiceBlockingStub stub =
                DeckofcardsServiceGrpc.newBlockingStub(channel);

            GameResponse response = stub.startGame(request);

            if (response.getSuccess()) {
                DeckofcardsDTO result = convertProtobufToGateway(response.getGameState());
                logger.info("✅ gRPC Client: 成功開始新遊戲");
                return result;
            } else {
                logger.error("❌ gRPC Client: 開始遊戲失敗: {}", response.getMessage());
                throw new RuntimeException("Failed to start game: " + response.getMessage());
            }

        } catch (Exception e) {
            logger.error("❌ gRPC Client: 開始遊戲失敗 - 錯誤類型: {}, 錯誤信息: {}", e.getClass().getSimpleName(), e.getMessage(), e);

            if (e.getMessage() != null && e.getMessage().contains("UNAVAILABLE")) {
                logger.error("❌ gRPC Client: 連接不可用，請檢查後端 gRPC 服務器是否運行在 {}:{}", backendHost, backendPort);
            }

            throw new RuntimeException("Failed to start game via gRPC: " + e.getMessage(), e);
        }
    }

    /**
     * 玩家要牌
     */
    public DeckofcardsDTO playerHit() {
        logger.info("📥 gRPC Client: 請求玩家要牌");

        try {
            PlayerHitRequest request = PlayerHitRequest.newBuilder().build();
            DeckofcardsServiceGrpc.DeckofcardsServiceBlockingStub stub =
                DeckofcardsServiceGrpc.newBlockingStub(channel);

            GameResponse response = stub.playerHit(request);

            if (response.getSuccess()) {
                DeckofcardsDTO result = convertProtobufToGateway(response.getGameState());
                logger.info("✅ gRPC Client: 成功執行玩家要牌");
                return result;
            } else {
                logger.error("❌ gRPC Client: 玩家要牌失敗: {}", response.getMessage());
                throw new RuntimeException("Failed to hit: " + response.getMessage());
            }

        } catch (Exception e) {
            logger.error("❌ gRPC Client: 玩家要牌失敗: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to hit via gRPC: " + e.getMessage(), e);
        }
    }

    /**
     * 玩家停牌
     */
    public DeckofcardsDTO playerStand() {
        logger.info("📥 gRPC Client: 請求玩家停牌");

        try {
            PlayerStandRequest request = PlayerStandRequest.newBuilder().build();
            DeckofcardsServiceGrpc.DeckofcardsServiceBlockingStub stub =
                DeckofcardsServiceGrpc.newBlockingStub(channel);

            GameResponse response = stub.playerStand(request);

            if (response.getSuccess()) {
                DeckofcardsDTO result = convertProtobufToGateway(response.getGameState());
                logger.info("✅ gRPC Client: 成功執行玩家停牌");
                return result;
            } else {
                logger.error("❌ gRPC Client: 玩家停牌失敗: {}", response.getMessage());
                throw new RuntimeException("Failed to stand: " + response.getMessage());
            }

        } catch (Exception e) {
            logger.error("❌ gRPC Client: 玩家停牌失敗: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to stand via gRPC: " + e.getMessage(), e);
        }
    }

    /**
     * 獲取遊戲狀態
     */
    public DeckofcardsDTO getGameStatus() {
        logger.info("📥 gRPC Client: 請求獲取遊戲狀態");

        try {
            GetGameStatusRequest request = GetGameStatusRequest.newBuilder().build();
            DeckofcardsServiceGrpc.DeckofcardsServiceBlockingStub stub =
                DeckofcardsServiceGrpc.newBlockingStub(channel);

            GameResponse response = stub.getGameStatus(request);

            if (response.getSuccess()) {
                DeckofcardsDTO result = convertProtobufToGateway(response.getGameState());
                logger.info("✅ gRPC Client: 成功獲取遊戲狀態");
                return result;
            } else {
                logger.error("❌ gRPC Client: 獲取遊戲狀態失敗: {}", response.getMessage());
                throw new RuntimeException("Failed to get game status: " + response.getMessage());
            }

        } catch (Exception e) {
            logger.error("❌ gRPC Client: 獲取遊戲狀態失敗: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get game status via gRPC: " + e.getMessage(), e);
        }
    }

    /**
     * 玩家加倍
     */
    public DeckofcardsDTO playerDouble() {
        logger.info("📥 gRPC Client: 請求玩家加倍");

        try {
            PlayerDoubleRequest request = PlayerDoubleRequest.newBuilder().build();
            DeckofcardsServiceGrpc.DeckofcardsServiceBlockingStub stub =
                DeckofcardsServiceGrpc.newBlockingStub(channel);

            GameResponse response = stub.playerDouble(request);

            if (response.getSuccess()) {
                DeckofcardsDTO result = convertProtobufToGateway(response.getGameState());
                logger.info("✅ gRPC Client: 成功執行玩家加倍");
                return result;
            } else {
                logger.error("❌ gRPC Client: 玩家加倍失敗: {}", response.getMessage());
                throw new RuntimeException("Failed to double: " + response.getMessage());
            }

        } catch (Exception e) {
            logger.error("❌ gRPC Client: 玩家加倍失敗: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to double via gRPC: " + e.getMessage(), e);
        }
    }

    /**
     * 玩家分牌
     */
    public DeckofcardsDTO playerSplit() {
        logger.info("📥 gRPC Client: 請求玩家分牌");

        try {
            PlayerSplitRequest request = PlayerSplitRequest.newBuilder().build();
            DeckofcardsServiceGrpc.DeckofcardsServiceBlockingStub stub =
                DeckofcardsServiceGrpc.newBlockingStub(channel);

            GameResponse response = stub.playerSplit(request);

            if (response.getSuccess()) {
                DeckofcardsDTO result = convertProtobufToGateway(response.getGameState());
                logger.info("✅ gRPC Client: 成功執行玩家分牌");
                return result;
            } else {
                logger.error("❌ gRPC Client: 玩家分牌失敗: {}", response.getMessage());
                throw new RuntimeException("Failed to split: " + response.getMessage());
            }

        } catch (Exception e) {
            logger.error("❌ gRPC Client: 玩家分牌失敗: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to split via gRPC: " + e.getMessage(), e);
        }
    }

    /**
     * 檢查gRPC連接健康狀態
     */
    public boolean isHealthy() {
        try {
            GetGameStatusRequest request = GetGameStatusRequest.newBuilder().build();
            DeckofcardsServiceGrpc.DeckofcardsServiceBlockingStub stub =
                DeckofcardsServiceGrpc.newBlockingStub(channel);
            GameResponse response = stub.getGameStatus(request);
            return response != null;
        } catch (Exception e) {
            logger.error("❌ gRPC Deckofcards 健康檢查失敗", e);
            return false;
        }
    }

    private DeckofcardsDTO convertProtobufToGateway(GameState protobufData) {
        DeckofcardsDTO gatewayData = new DeckofcardsDTO();

        gatewayData.setGameId(protobufData.getGameId());
        gatewayData.setPlayerScore(protobufData.getPlayerScore());
        gatewayData.setDealerScore(protobufData.getDealerScore());
        gatewayData.setGameStatus(protobufData.getGameStatus());
        gatewayData.setCanHit(protobufData.getCanHit());
        gatewayData.setCanStand(protobufData.getCanStand());
        gatewayData.setCanDouble(protobufData.getCanDouble());
        gatewayData.setCanSplit(protobufData.getCanSplit());
        gatewayData.setMessage(protobufData.getMessage());

        // 轉換撲克牌列表
        gatewayData.setPlayerCards(protobufData.getPlayerCardsList().stream()
            .map(this::convertCard)
            .toArray(DeckofcardsDTO.Card[]::new));

        gatewayData.setDealerCards(protobufData.getDealerCardsList().stream()
            .map(this::convertCard)
            .toArray(DeckofcardsDTO.Card[]::new));

        return gatewayData;
    }

    private DeckofcardsDTO.Card convertCard(Card protobufCard) {
        DeckofcardsDTO.Card card = new DeckofcardsDTO.Card();
        card.setSuit(protobufCard.getSuit());
        card.setRank(protobufCard.getRank());
        card.setValue(protobufCard.getValue());
        return card;
    }
}
