package tw.com.tymgateway.dto;

/**
 * Gateway專用的認證重定向響應DTO
 * 用於HTTP API響應，不依賴backend的generated classes
 */
public class AuthRedirectResponse {
    private boolean success;
    private String message;
    private UserInfo userInfo;
    private String accessToken;
    private String refreshToken;

    public AuthRedirectResponse() {}

    public AuthRedirectResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public AuthRedirectResponse(boolean success, String message, UserInfo userInfo, String accessToken, String refreshToken) {
        this.success = success;
        this.message = message;
        this.userInfo = userInfo;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    // Getters and Setters
    public boolean getSuccess() { return success; }
    public boolean isSuccess() { return success; }  // Add isSuccess() for consistency
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public UserInfo getUserInfo() { return userInfo; }
    public void setUserInfo(UserInfo userInfo) { this.userInfo = userInfo; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
