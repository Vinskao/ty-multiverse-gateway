package tw.com.tymgateway.dto;

/**
 * Gateway專用的登出響應DTO
 * 用於HTTP API響應，不依賴backend的generated classes
 */
public class LogoutResponse {
    private boolean success;
    private String message;

    public LogoutResponse() {}

    public LogoutResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // Getters and Setters
    public boolean getSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
