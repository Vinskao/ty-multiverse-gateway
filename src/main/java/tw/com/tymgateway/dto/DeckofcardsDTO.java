package tw.com.tymgateway.dto;

/**
 * Deckofcards 數據傳輸對象
 *
 * <p>用於 Gateway 層與外部系統交換數據</p>
 *
 * @author TY Team
 * @version 1.0
 */
public class DeckofcardsDTO {

    private String gameId;
    private Card[] playerCards;
    private Card[] dealerCards;
    private int playerScore;
    private int dealerScore;
    private String gameStatus;
    private boolean canHit;
    private boolean canStand;
    private boolean canDouble;
    private boolean canSplit;
    private String message;

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public Card[] getPlayerCards() {
        return playerCards;
    }

    public void setPlayerCards(Card[] playerCards) {
        this.playerCards = playerCards;
    }

    public Card[] getDealerCards() {
        return dealerCards;
    }

    public void setDealerCards(Card[] dealerCards) {
        this.dealerCards = dealerCards;
    }

    public int getPlayerScore() {
        return playerScore;
    }

    public void setPlayerScore(int playerScore) {
        this.playerScore = playerScore;
    }

    public int getDealerScore() {
        return dealerScore;
    }

    public void setDealerScore(int dealerScore) {
        this.dealerScore = dealerScore;
    }

    public String getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(String gameStatus) {
        this.gameStatus = gameStatus;
    }

    public boolean isCanHit() {
        return canHit;
    }

    public void setCanHit(boolean canHit) {
        this.canHit = canHit;
    }

    public boolean isCanStand() {
        return canStand;
    }

    public void setCanStand(boolean canStand) {
        this.canStand = canStand;
    }

    public boolean isCanDouble() {
        return canDouble;
    }

    public void setCanDouble(boolean canDouble) {
        this.canDouble = canDouble;
    }

    public boolean isCanSplit() {
        return canSplit;
    }

    public void setCanSplit(boolean canSplit) {
        this.canSplit = canSplit;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 遊戲響應 DTO
     */
    public static class GameResponseDTO {
        private boolean success;
        private String message;
        private GameStateDTO gameState;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public GameStateDTO getGameState() { return gameState; }
        public void setGameState(GameStateDTO gameState) { this.gameState = gameState; }
    }

    /**
     * 遊戲狀態 DTO
     */
    public static class GameStateDTO {
        private String playerId;
        private String status;
        private Card[] playerCards;
        private Card[] dealerCards;
        private int playerScore;
        private int dealerScore;

        public String getPlayerId() { return playerId; }
        public void setPlayerId(String playerId) { this.playerId = playerId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Card[] getPlayerCards() { return playerCards; }
        public void setPlayerCards(Card[] playerCards) { this.playerCards = playerCards; }

        public Card[] getDealerCards() { return dealerCards; }
        public void setDealerCards(Card[] dealerCards) { this.dealerCards = dealerCards; }

        public int getPlayerScore() { return playerScore; }
        public void setPlayerScore(int playerScore) { this.playerScore = playerScore; }

        public int getDealerScore() { return dealerScore; }
        public void setDealerScore(int dealerScore) { this.dealerScore = dealerScore; }
    }

    /**
     * 撲克牌數據結構
     */
    public static class Card {
        private String suit;
        private String rank;
        private int value;

        public String getSuit() {
            return suit;
        }

        public void setSuit(String suit) {
            this.suit = suit;
        }

        public String getRank() {
            return rank;
        }

        public void setRank(String rank) {
            this.rank = rank;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }
}
