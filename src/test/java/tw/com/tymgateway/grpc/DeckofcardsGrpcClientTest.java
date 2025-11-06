package tw.com.tymgateway.grpc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import tw.com.tymgateway.grpc.client.DeckofcardsGrpcClient;
import tw.com.tymgateway.dto.DeckofcardsDTO;

import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DeckOfCards gRPC Client 測試類
 *
 * <p>測試 Gateway 與 Backend 的 DeckOfCards gRPC 通信</p>
 */
@SpringBootTest
@TestPropertySource(properties = {
    "grpc.client.enabled=true",
    "grpc.client.backend.host=localhost",
    "grpc.client.backend.port=50051"
})
class DeckofcardsGrpcClientTest {

    @Autowired
    private DeckofcardsGrpcClient deckofcardsGrpcClient;

    private boolean isBackendRunning() {
        try (Socket socket = new Socket("localhost", 50051)) {
            return socket.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void testGrpcClientBeanExists() {
        assertNotNull(deckofcardsGrpcClient, "DeckofcardsGrpcClient 應該被正確注入");
    }

    @Test
    @EnabledIf("isBackendRunning")
    void testStartGame() {
        System.out.println("🃏 測試開始遊戲...");
        
        DeckofcardsDTO result = deckofcardsGrpcClient.startGame();
        
        assertNotNull(result, "應該返回遊戲響應");
        assertNotNull(result.getGameStatus(), "應該返回遊戲狀態");
        
        System.out.println("✅ 開始遊戲成功");
        System.out.println("   遊戲狀態: " + result.getGameStatus());
    }

    @Test
    @EnabledIf("isBackendRunning")
    void testPlayerHit() {
        System.out.println("🃏 測試玩家要牌...");
        
        // 先開始一個遊戲
        DeckofcardsDTO startResult = deckofcardsGrpcClient.startGame();
        assertNotNull(startResult, "遊戲應該成功開始");
        
        // 玩家要牌
        DeckofcardsDTO hitResult = deckofcardsGrpcClient.playerHit();
        
        assertNotNull(hitResult, "應該返回要牌響應");
        assertNotNull(hitResult.getGameStatus(), "應該返回遊戲狀態");
        
        System.out.println("✅ 玩家要牌成功");
        System.out.println("   遊戲狀態: " + hitResult.getGameStatus());
    }

    @Test
    @EnabledIf("isBackendRunning")
    void testPlayerStand() {
        System.out.println("🃏 測試玩家停牌...");
        
        // 先開始一個遊戲
        DeckofcardsDTO startResult = deckofcardsGrpcClient.startGame();
        assertNotNull(startResult, "遊戲應該成功開始");
        
        // 玩家停牌
        DeckofcardsDTO standResult = deckofcardsGrpcClient.playerStand();
        
        assertNotNull(standResult, "應該返回停牌響應");
        assertNotNull(standResult.getGameStatus(), "應該返回遊戲狀態");
        
        System.out.println("✅ 玩家停牌成功");
        System.out.println("   遊戲狀態: " + standResult.getGameStatus());
    }

    @Test
    @EnabledIf("isBackendRunning")
    void testGetGameStatus() {
        System.out.println("🃏 測試獲取遊戲狀態...");
        
        // 先開始一個遊戲
        DeckofcardsDTO startResult = deckofcardsGrpcClient.startGame();
        assertNotNull(startResult, "遊戲應該成功開始");
        
        // 獲取遊戲狀態
        DeckofcardsDTO gameState = deckofcardsGrpcClient.getGameStatus();
        
        assertNotNull(gameState, "應該返回遊戲狀態");
        assertNotNull(gameState.getGameStatus(), "遊戲狀態應該存在");
        
        System.out.println("✅ 獲取遊戲狀態成功");
        System.out.println("   遊戲狀態: " + gameState.getGameStatus());
    }

    @Test
    void testConnectionFailureHandling() {
        if (!isBackendRunning()) {
            System.out.println("⚠️  Backend 未運行，測試錯誤處理...");
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                deckofcardsGrpcClient.startGame();
            });
            assertTrue(exception.getMessage().contains("Failed to call gRPC service"),
                      "錯誤消息應該包含失敗信息");
            System.out.println("✅ 錯誤處理正確");
        }
    }
}


