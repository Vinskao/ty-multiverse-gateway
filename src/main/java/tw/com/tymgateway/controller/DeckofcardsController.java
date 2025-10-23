package tw.com.tymgateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tw.com.tymgateway.grpc.client.DeckofcardsGrpcClient;
import tw.com.tymgateway.dto.DeckofcardsDTO;

/**
 * Deckofcards Controller
 *
 * <p>處理 Deckofcards (21點) 遊戲的 HTTP 請求，通過 gRPC 調用 Backend</p>
 * <p>架構：Frontend (HTTP) -> Gateway (gRPC) -> Backend -> MQ -> Consumer</p>
 *
 * @author TY Team
 * @version 1.0
 */
@RestController
@RequestMapping("/deckofcards")
@ConditionalOnProperty(name = "grpc.client.enabled", havingValue = "true")
public class DeckofcardsController {

    private static final Logger logger = LoggerFactory.getLogger(DeckofcardsController.class);

    @Autowired
    private DeckofcardsGrpcClient deckofcardsGrpcClient;

    @PostMapping("/blackjack/start")
    public ResponseEntity<?> startGame() {
        logger.info("🎮 HTTP 請求: 開始新21點遊戲");

        try {
            DeckofcardsDTO gameState = deckofcardsGrpcClient.startGame();
            logger.info("✅ 成功開始新遊戲，遊戲ID: {}", gameState.getGameId());
            return ResponseEntity.ok(gameState);
        } catch (Exception e) {
            logger.error("❌ 開始新遊戲失敗", e);
            return ResponseEntity.badRequest()
                .body("Failed to start new game: " + e.getMessage());
        }
    }

    @PostMapping("/blackjack/hit")
    public ResponseEntity<?> hit() {
        logger.info("🎮 HTTP 請求: 玩家要牌");

        try {
            DeckofcardsDTO gameState = deckofcardsGrpcClient.playerHit();
            logger.info("✅ 玩家要牌成功");
            return ResponseEntity.ok(gameState);
        } catch (IllegalStateException e) {
            logger.error("❌ 無效的要牌操作: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body("Invalid hit action: " + e.getMessage());
        } catch (Exception e) {
            logger.error("❌ 玩家要牌失敗", e);
            return ResponseEntity.internalServerError()
                .body("Error during hit action: " + e.getMessage());
        }
    }

    @PostMapping("/blackjack/stand")
    public ResponseEntity<?> stand() {
        logger.info("🎮 HTTP 請求: 玩家停牌");

        try {
            DeckofcardsDTO gameState = deckofcardsGrpcClient.playerStand();
            logger.info("✅ 玩家停牌成功");
            return ResponseEntity.ok(gameState);
        } catch (IllegalStateException e) {
            logger.error("❌ 無效的停牌操作: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body("Invalid stand action: " + e.getMessage());
        } catch (Exception e) {
            logger.error("❌ 玩家停牌失敗", e);
            return ResponseEntity.internalServerError()
                .body("Error during stand action: " + e.getMessage());
        }
    }

    @GetMapping("/blackjack/status")
    public ResponseEntity<?> getGameStatus() {
        logger.info("🎮 HTTP 請求: 獲取遊戲狀態");

        try {
            DeckofcardsDTO gameState = deckofcardsGrpcClient.getGameStatus();
            logger.info("✅ 成功獲取遊戲狀態");
            return ResponseEntity.ok(gameState);
        } catch (IllegalStateException e) {
            logger.error("❌ 無法獲取遊戲狀態: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body("Cannot get game status: " + e.getMessage());
        } catch (Exception e) {
            logger.error("❌ 獲取遊戲狀態失敗", e);
            return ResponseEntity.internalServerError()
                .body("Error getting game status: " + e.getMessage());
        }
    }

    @PostMapping("/blackjack/double")
    public ResponseEntity<?> doubleDown() {
        logger.info("🎮 HTTP 請求: 玩家加倍");

        try {
            DeckofcardsDTO gameState = deckofcardsGrpcClient.playerDouble();
            logger.info("✅ 玩家加倍成功");
            return ResponseEntity.ok(gameState);
        } catch (IllegalStateException e) {
            logger.error("❌ 無效的加倍操作: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body("Invalid double down action: " + e.getMessage());
        } catch (Exception e) {
            logger.error("❌ 玩家加倍失敗", e);
            return ResponseEntity.internalServerError()
                .body("Error during double down: " + e.getMessage());
        }
    }

    @PostMapping("/blackjack/split")
    public ResponseEntity<?> split() {
        logger.info("🎮 HTTP 請求: 玩家分牌");

        try {
            DeckofcardsDTO gameState = deckofcardsGrpcClient.playerSplit();
            logger.info("✅ 玩家分牌成功");
            return ResponseEntity.ok(gameState);
        } catch (IllegalStateException e) {
            logger.error("❌ 無效的分牌操作: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body("Invalid split action: " + e.getMessage());
        } catch (Exception e) {
            logger.error("❌ 玩家分牌失敗", e);
            return ResponseEntity.internalServerError()
                .body("Error during split: " + e.getMessage());
        }
    }
}
