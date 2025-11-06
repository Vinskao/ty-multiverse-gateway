package tw.com.tymgateway.dto;

/**
 * Gateway專用的Token驗證響應DTO
 * 用於HTTP API響應，不依賴backend的generated classes
 */
public class IntrospectTokenResponse {
    private boolean active;
    private String message;
    private String newAccessToken;
    private String newRefreshToken;

    public IntrospectTokenResponse() {}

    public IntrospectTokenResponse(boolean active, String message) {
        this.active = active;
        this.message = message;
    }

    public IntrospectTokenResponse(boolean active, String message, String newAccessToken, String newRefreshToken) {
        this.active = active;
        this.message = message;
        this.newAccessToken = newAccessToken;
        this.newRefreshToken = newRefreshToken;
    }

    // Getters and Setters
    public boolean getActive() { return active; }
    public boolean isActive() { return active; }  // Add isActive() for consistency
    public void setActive(boolean active) { this.active = active; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getNewAccessToken() { return newAccessToken; }
    public void setNewAccessToken(String newAccessToken) { this.newAccessToken = newAccessToken; }

    public String getNewRefreshToken() { return newRefreshToken; }
    public void setNewRefreshToken(String newRefreshToken) { this.newRefreshToken = newRefreshToken; }
}
