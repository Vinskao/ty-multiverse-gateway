package tw.com.tymgateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tw.com.tymgateway.grpc.client.KeycloakGrpcClient;
import tw.com.tymgateway.dto.AuthRedirectResponse;
import tw.com.tymgateway.dto.LogoutResponse;
import tw.com.tymgateway.dto.IntrospectTokenResponse;
import tw.com.tymgateway.dto.UserInfo;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Keycloak 模組的 gRPC Gateway Controller
 *
 * <p>接收 HTTP 請求，透過 gRPC 呼叫 Backend 服務</p>
 *
 * @author TY Team
 * @version 1.0
 */
@RestController
@RequestMapping("/keycloak")
@ConditionalOnProperty(name = "grpc.client.enabled", havingValue = "true")
public class KeycloakController {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakController.class);

    @Autowired
    private KeycloakGrpcClient keycloakGrpcClient;

    @org.springframework.beans.factory.annotation.Value("${url.frontend}")
    private String frontendUrl;

    /**
     * 透過 gRPC 處理 OAuth2 重定向
     *
     * API 端點: GET /tymgateway/keycloak/redirect
     *
     * 請求參數:
     * - code: 授權碼
     * - redirect_uri: 重定向 URI
     *
     * 回應格式:
     * {
     *   "success": true,
     *   "message": "認證成功",
     *   "user_info": {...},  // 用戶資訊
     *   "access_token": "...",
     *   "refresh_token": "..."
     * }
     */
    @GetMapping("/redirect")
    public ResponseEntity<Map<String, Object>> processAuthRedirect(
            @RequestParam("code") String code,
            @RequestParam("redirect_uri") String redirectUri) {
        try {
            logger.info("Gateway: Received HTTP request to process auth redirect, calling Backend via gRPC...");
            
            AuthRedirectResponse response = keycloakGrpcClient.processAuthRedirect(code, redirectUri);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", response.getSuccess());
            result.put("message", response.getMessage());
            
            if (response.getSuccess()) {
                result.put("user_info", convertUserInfoToMap(response.getUserInfo()));
                result.put("access_token", response.getAccessToken());
                result.put("refresh_token", response.getRefreshToken());
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Gateway: Error calling gRPC service", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to process auth redirect: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 透過 gRPC 處理用戶登出
     *
     * API 端點: POST /tymgateway/keycloak/logout
     *
     * 請求格式:
     * {
     *   "refresh_token": "刷新令牌"
     * }
     *
     * 回應格式:
     * {
     *   "success": true,
     *   "message": "登出成功"
     * }
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refresh_token");
            logger.info("Gateway: Received HTTP request to logout, calling Backend via gRPC...");
            
            LogoutResponse response = keycloakGrpcClient.logout(refreshToken);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", response.getSuccess());
            result.put("message", response.getMessage());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Gateway: Error calling gRPC service", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to logout: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 透過 gRPC 處理 Token 驗證與刷新
     *
     * API 端點: POST /tymgateway/keycloak/introspect
     *
     * 請求格式:
     * {
     *   "access_token": "存取令牌",
     *   "refresh_token": "刷新令牌" (可選)
     * }
     *
     * 回應格式:
     * {
     *   "active": true,
     *   "message": "Token 有效",
     *   "new_access_token": "...", (如果刷新了)
     *   "new_refresh_token": "..." (如果刷新了)
     * }
     */
    @PostMapping("/introspect")
    public ResponseEntity<Map<String, Object>> introspectToken(@RequestBody Map<String, String> request) {
        try {
            String accessToken = request.get("access_token");
            String refreshToken = request.get("refresh_token");
            logger.info("Gateway: Received HTTP request to introspect token, calling Backend via gRPC...");
            
            IntrospectTokenResponse response = keycloakGrpcClient.introspectToken(accessToken, refreshToken);
            
            Map<String, Object> result = new HashMap<>();
            result.put("active", response.getActive());
            result.put("message", response.getMessage());
            
            if (response.getActive() && !response.getNewAccessToken().isEmpty()) {
                result.put("new_access_token", response.getNewAccessToken());
                result.put("new_refresh_token", response.getNewRefreshToken());
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Gateway: Error calling gRPC service", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("active", false);
            errorResponse.put("message", "Failed to introspect token: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * API 文檔
     */
    @GetMapping("/docs")
    public ResponseEntity<Map<String, Object>> getApiDocs() {
        Map<String, Object> docs = new HashMap<>();
        docs.put("title", "TY Multiverse Gateway - Keycloak API");
        docs.put("description", "透過 gRPC 調用後端 Keycloak 服務的 API Gateway");

        Map<String, Object> endpoints = new HashMap<>();
        
        // redirect endpoint
        Map<String, Object> redirect = new HashMap<>();
        redirect.put("method", "GET");
        redirect.put("path", "/tymgateway/keycloak/redirect");
        redirect.put("description", "處理 OAuth2 重定向");
        redirect.put("parameters", Map.of(
            "code", "授權碼",
            "redirect_uri", "重定向 URI"
        ));
        redirect.put("response", Map.of(
            "success", true,
            "message", "認證成功",
            "user_info", "用戶資訊",
            "access_token", "存取令牌",
            "refresh_token", "刷新令牌"
        ));
        endpoints.put("redirect", redirect);

        // logout endpoint
        Map<String, Object> logout = new HashMap<>();
        logout.put("method", "POST");
        logout.put("path", "/tymgateway/keycloak/logout");
        logout.put("description", "用戶登出");
        logout.put("request", Map.of("refresh_token", "刷新令牌"));
        logout.put("response", Map.of(
            "success", true,
            "message", "登出成功"
        ));
        endpoints.put("logout", logout);

        // introspect endpoint
        Map<String, Object> introspect = new HashMap<>();
        introspect.put("method", "POST");
        introspect.put("path", "/tymgateway/keycloak/introspect");
        introspect.put("description", "Token 驗證與刷新");
        introspect.put("request", Map.of(
            "access_token", "存取令牌",
            "refresh_token", "刷新令牌 (可選)"
        ));
        introspect.put("response", Map.of(
            "active", true,
            "message", "Token 有效",
            "new_access_token", "新的存取令牌 (如果刷新了)",
            "new_refresh_token", "新的刷新令牌 (如果刷新了)"
        ));
        endpoints.put("introspect", introspect);

        docs.put("endpoints", endpoints);
        docs.put("baseUrl", "http://localhost:8082");

        return ResponseEntity.ok(docs);
    }

    /**
     * 處理從 Keycloak 認證後重導向回來的請求（舊的路由，為了兼容前端）
     *
     * 此方法是為了兼容前端現有的路由結構，將請求轉發到 gRPC 服務處理。
     * 最終會將使用者資訊與 tokens 附加至前端 URL 並進行重導向。
     *
     * @param code Keycloak 返回的授權碼
     * @param redirectUri 重定向 URI（從請求參數獲取）
     * @return ResponseEntity 包含重導向響應
     */
    @GetMapping("/redirect-legacy")
    public ResponseEntity<Void> keycloakRedirect(@RequestParam("code") String code, @RequestParam(value = "redirect_uri", required = false) String redirectUri) {
        logger.info("Gateway: 收到舊路由授權碼重定向請求，code={}", code.substring(0, 20) + "...");

        try {
            logger.info("Gateway: 處理授權碼: {}", code);

            // 使用請求中的 redirect_uri，如果沒有則構造一個
            String actualRedirectUri = redirectUri != null ? redirectUri : "http://localhost:8082/tymgateway/keycloak/redirect";
            logger.info("Gateway: 使用 redirectUri: {}", actualRedirectUri);

            // 通過 gRPC 調用 backend 的認證服務
            AuthRedirectResponse grpcResponse = keycloakGrpcClient.processAuthRedirect(code, actualRedirectUri);

            if (!grpcResponse.getSuccess()) {
                logger.error("Gateway: gRPC 認證失敗: {}", grpcResponse.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            // 從 gRPC 響應中獲取用戶資訊和令牌
            UserInfo userInfo = grpcResponse.getUserInfo();
            String accessToken = grpcResponse.getAccessToken();
            String refreshToken = grpcResponse.getRefreshToken();

            logger.info("Gateway: 成功獲取用戶資訊和令牌，用戶名: {}", userInfo.getUsername());

            // 組合重導向 URL，將使用者資訊與 tokens 附加至 query string 中
            String redirectTarget = frontendUrl
                + "?username=" + URLEncoder.encode(userInfo.getUsername(), "UTF-8")
                + "&email=" + URLEncoder.encode(userInfo.getEmail(), "UTF-8")
                + "&name=" + URLEncoder.encode(userInfo.getName(), "UTF-8")
                + "&firstName=" + URLEncoder.encode(userInfo.getFirstName(), "UTF-8")
                + "&lastName=" + URLEncoder.encode(userInfo.getLastName(), "UTF-8")
                + "&token=" + URLEncoder.encode(accessToken, "UTF-8")
                + "&refreshToken=" + URLEncoder.encode(refreshToken, "UTF-8");

            // 添加詳細日誌
            logger.info("=== Gateway 重定向診斷 ===");
            logger.info("前端URL: {}", frontendUrl);
            logger.info("用戶名: {}", userInfo.getUsername());
            logger.info("Token長度: {}", accessToken.length());
            logger.info("Token前20字符: {}", accessToken.substring(0, Math.min(20, accessToken.length())));
            logger.info("完整重定向URL: {}", redirectTarget);

            // 返回重導向響應
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", redirectTarget);
            return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();

        } catch (Exception e) {
            // 若有任何錯誤，記錄錯誤並回傳 500 錯誤碼
            logger.error("Gateway: 處理 OAuth 重定向時發生錯誤", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 將 UserInfo 轉換成 Map
     */
    private Map<String, Object> convertUserInfoToMap(UserInfo userInfo) {
        Map<String, Object> map = new HashMap<>();
        map.put("username", userInfo.getUsername());
        map.put("email", userInfo.getEmail());
        map.put("name", userInfo.getName());
        map.put("first_name", userInfo.getFirstName());
        map.put("last_name", userInfo.getLastName());
        return map;
    }
}
