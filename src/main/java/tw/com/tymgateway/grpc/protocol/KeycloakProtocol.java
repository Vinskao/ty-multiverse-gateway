package tw.com.tymgateway.grpc.protocol;

/**
 * Gateway專用的Keycloak協議類型定義
 * 簡化的協議定義，用於gRPC通訊，不依賴backend
 */
public class KeycloakProtocol {

    // 請求類型
    public static class AuthRedirectRequest {
        private String code;
        private String redirectUri;

        public AuthRedirectRequest() {}

        public AuthRedirectRequest(String code, String redirectUri) {
            this.code = code;
            this.redirectUri = redirectUri;
        }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    }

    public static class LogoutRequest {
        private String refreshToken;

        public LogoutRequest() {}

        public LogoutRequest(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }

    public static class IntrospectTokenRequest {
        private String accessToken;
        private String refreshToken;

        public IntrospectTokenRequest() {}

        public IntrospectTokenRequest(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }

    // 響應類型
    public static class AuthRedirectResponse {
        private boolean success;
        private String message;
        private UserInfo userInfo;
        private String accessToken;
        private String refreshToken;

        public AuthRedirectResponse() {}

        public boolean getSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public UserInfo getUserInfo() { return userInfo; }
        public void setUserInfo(UserInfo userInfo) { this.userInfo = userInfo; }

        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

        public boolean hasUserInfo() { return userInfo != null; }
    }

    public static class UserInfo {
        private String username;
        private String email;
        private String name;
        private String firstName;
        private String lastName;

        public UserInfo() {}

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
    }

    public static class LogoutResponse {
        private boolean success;
        private String message;

        public LogoutResponse() {}

        public boolean getSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class IntrospectTokenResponse {
        private boolean active;
        private String message;
        private String newAccessToken;
        private String newRefreshToken;

        public IntrospectTokenResponse() {}

        public boolean getActive() { return active; }
        public void setActive(boolean active) { this.active = active; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getNewAccessToken() { return newAccessToken; }
        public void setNewAccessToken(String newAccessToken) { this.newAccessToken = newAccessToken; }

        public String getNewRefreshToken() { return newRefreshToken; }
        public void setNewRefreshToken(String newRefreshToken) { this.newRefreshToken = newRefreshToken; }
    }
}
