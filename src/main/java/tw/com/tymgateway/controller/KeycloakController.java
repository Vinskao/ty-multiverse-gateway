package tw.com.tymgateway.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;
import tw.com.ty.common.response.ErrorCode;
import tw.com.ty.common.response.MessageKey;
import jakarta.annotation.PostConstruct;

/**
 * Keycloak 控制器
 * 
 * 負責處理與 Keycloak 的 OAuth2 認證流程，包括重定向處理、登出和 Token 驗證等功能。
 * Gateway 版本使用 WebFlux (reactive) 實現。
 */
@RestController
@RequestMapping("/tymg/keycloak")
public class KeycloakController {

    @Value("${url.frontend}")
    private String frontendUrl;

    private static final Logger log = LoggerFactory.getLogger(KeycloakController.class);
    private static final String[] DEBUG_LOG_PATHS = new String[] {
            "f:\\002-workspace\\ty-multiverse\\.cursor\\debug.log", // workspace absolute (Windows)
            ".cursor/debug.log" // relative fallback (e.g., different cwd/platform)
    };

    private String safe(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void agentLog(String hypothesisId, String location, String message, String dataJson) {
        // #region agent log
        boolean written = false;
        for (String path : DEBUG_LOG_PATHS) {
            try {
                java.io.File file = new java.io.File(path);
                file.getParentFile().mkdirs();
                try (java.io.FileWriter fw = new java.io.FileWriter(file, true)) {
                    fw.write("{\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\""
                            + hypothesisId + "\",\"location\":\"" + location + "\",\"message\":\"" + message
                            + "\",\"data\":" + dataJson + ",\"timestamp\":" + System.currentTimeMillis() + "}\n");
                }
                written = true;
                break;
            } catch (Exception ignored) {
                // try next path
            }
        }
        if (!written) {
            log.warn("agentLog failed to write debug entry for {} {}", hypothesisId, location);
        }
        // #endregion
    }

    @Autowired
    private WebClient.Builder webClientBuilder;

    private WebClient getWebClient() {
        return webClientBuilder.build();
    }

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.clientId:${keycloak.resource}}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    @Value("${keycloak.auth-server-url}")
    private String ssoUrl;

    @PostConstruct
    public void init() {
        log.info("=== Keycloak Configuration ===");
        log.info("Auth Server URL: {}", ssoUrl);
        log.info("Realm: {}", realm);
        log.info("Client ID: {}", clientId);
        log.info("Client Secret configured: {}", clientSecret != null && !clientSecret.isEmpty());
        log.info("Frontend URL: {}", frontendUrl);
    }

    /**
     * 處理從 Keycloak 認證後重導向回來的請求
     * 
     * 本方法使用授權碼向 Keycloak 取得存取憑證 (access token) 與更新憑證 (refresh token)，
     * 並呼叫 userinfo 端點以獲取使用者資訊。成功取得資料後，會將使用者名稱、電子郵件、
     * access token 以及 refresh token 附加至前端 URL 並進行重導向。
     *
     * @param code Keycloak 返回的授權碼
     * @param exchange ServerWebExchange 用於獲取請求 URL 和進行重導向
     * @return Mono<Void> 重導向響應
     */
    @GetMapping("/redirect")
    public Mono<Void> keycloakRedirect(
            @RequestParam("code") String code,
            ServerWebExchange exchange) {
        // 構建 redirect_uri：只包含基礎 URL，不包含查詢參數
        // Keycloak 要求 redirect_uri 必須與授權請求時完全一致（不包含查詢參數）
        java.net.URI requestUri = exchange.getRequest().getURI();
        String redirectUri = requestUri.getScheme() + "://" + requestUri.getAuthority() + requestUri.getPath();
        log.info("後端使用的 redirectUri: {}", redirectUri);
        
        // 組合 token 請求 URL：Keycloak Token Endpoint
        String tokenUrl = ssoUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        log.info("收到授權碼: {}", code);
        log.info("Client ID: {}", clientId);
        log.info("Client Secret 長度: {}", clientSecret != null ? clientSecret.length() : 0);
        log.info("Token URL: {}", tokenUrl);
        log.info("Redirect URI: {}", redirectUri);

        // 建立存放 token 請求參數的 MultiValueMap
        MultiValueMap<String, String> tokenParams = new LinkedMultiValueMap<>();
        tokenParams.add("client_id", clientId);
        tokenParams.add("client_secret", clientSecret);
        tokenParams.add("code", code);
        tokenParams.add("grant_type", "authorization_code");
        tokenParams.add("redirect_uri", redirectUri);

        log.info("Token 請求參數: client_id={}, grant_type=authorization_code, redirect_uri={}", 
                clientId, redirectUri);

        agentLog("H1", "KeycloakController.keycloakRedirect", "token_request_params",
                "{\"realm\":\"" + safe(realm) + "\","
                        + "\"clientId\":\"" + safe(clientId) + "\","
                        + "\"secretLength\":" + (clientSecret == null ? 0 : clientSecret.length()) + ","
                        + "\"redirectUri\":\"" + safe(redirectUri) + "\","
                        + "\"ssoUrl\":\"" + safe(ssoUrl) + "\","
                        + "\"grantType\":\"authorization_code\","
                        + "\"codeLength\":" + (code != null ? code.length() : 0) + "}");

        // 使用 WebClient 發送 POST 請求給 Keycloak 的 token endpoint
        return getWebClient()
                .post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(tokenParams))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                    log.error("Keycloak token 請求失敗: HTTP {}", response.statusCode());
                    agentLog("H2", "KeycloakController.keycloakRedirect", "token_error_status",
                            "{\"status\":\"" + response.statusCode() + "\"}");
                    return response.bodyToMono(String.class)
                            .doOnNext(errorBody -> log.error("Keycloak 錯誤響應: {}", errorBody))
                            .then(Mono.error(new RuntimeException("Keycloak token 請求失敗: " + response.statusCode())));
                })
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .flatMap(tokenBody -> {
                    if (tokenBody == null) {
                        return Mono.error(new RuntimeException("無法取得 token 響應"));
                    }

                    // 從回傳內容中取得 access token
                    String accessToken = (String) tokenBody.get("access_token");
                    // 取得 refresh token
                    String refreshToken = (String) tokenBody.get("refresh_token");
                    // 取得 id token（用於登出時清除 session）
                    String idToken = (String) tokenBody.get("id_token");

                    log.info("Access Token: {}", accessToken);
                    log.info("Refresh Token: {}", refreshToken);
                    log.info("ID Token: {}", idToken != null ? "存在" : "不存在");

                    // 若其中任一 token 為 null，表示取得失敗，則拋出異常
                    if (accessToken == null || refreshToken == null) {
                        return Mono.error(new RuntimeException("無法取得 access token"));
                    }

                    // 呼叫 Keycloak userinfo endpoint 取得使用者資訊
                    String userInfoUrl = ssoUrl + "/realms/" + realm + "/protocol/openid-connect/userinfo";
                    
                    return getWebClient()
                            .get()
                            .uri(userInfoUrl)
                            .header("Authorization", "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                            .flatMap(userInfo -> {
                                if (userInfo == null) {
                                    return Mono.error(new RuntimeException("無法取得使用者資訊"));
                                }

                                log.info("使用者資訊: {}", userInfo);

                                // 從使用者資訊中取得使用者名稱
                                String preferredUsername = (String) userInfo.get("preferred_username");
                                if (preferredUsername == null) {
                                    return Mono.error(new RuntimeException("無法取得使用者資訊"));
                                }

                                // 從使用者資訊中取得電子郵件
                                String email = userInfo.get("email") != null ? (String) userInfo.get("email") : "未知";
                                String name = userInfo.get("name") != null ? (String) userInfo.get("name") : "未知";
                                String firstName = userInfo.get("given_name") != null ? (String) userInfo.get("given_name") : "未知";
                                String lastName = userInfo.get("family_name") != null ? (String) userInfo.get("family_name") : "未知";

                                // 組合重導向 URL，將使用者資訊與 tokens 附加至 query string 中
                                try {
                                    String redirectTarget = frontendUrl
                                            + "?username=" + URLEncoder.encode(preferredUsername, StandardCharsets.UTF_8)
                                            + "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                                            + "&name=" + URLEncoder.encode(name, StandardCharsets.UTF_8)
                                            + "&firstName=" + URLEncoder.encode(firstName, StandardCharsets.UTF_8)
                                            + "&lastName=" + URLEncoder.encode(lastName, StandardCharsets.UTF_8)
                                            + "&token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
                                            + "&refreshToken=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);
                                    
                                    // 如果有 id_token，也傳遞給前端（用於登出時清除 session）
                                    if (idToken != null) {
                                        redirectTarget += "&id_token=" + URLEncoder.encode(idToken, StandardCharsets.UTF_8);
                                    }

                                    // 添加詳細日誌
                                    log.info("=== 重定向診斷 ===");
                                    log.info("前端URL: {}", frontendUrl);
                                    log.info("用戶名: {}", preferredUsername);
                                    log.info("Token長度: {}", accessToken.length());
                                    log.info("Token前20字符: {}", accessToken.substring(0, Math.min(20, accessToken.length())));
                                    log.info("完整重定向URL: {}", redirectTarget);

                                    // 執行 HTTP 重導向
                                    exchange.getResponse().setStatusCode(HttpStatus.FOUND);
                                    exchange.getResponse().getHeaders().setLocation(java.net.URI.create(redirectTarget));
                                    return exchange.getResponse().setComplete();
                                } catch (Exception e) {
                                    return Mono.error(e);
                                }
                            });
                })
                .onErrorResume(e -> {
                    agentLog("H3", "KeycloakController.keycloakRedirect", "token_error_caught",
                            "{\"exception\":\"" + safe(e.getClass().getSimpleName()) + "\","
                                    + "\"message\":\"" + safe(e.getMessage()) + "\"}");
                    // 若有任何錯誤，記錄詳細錯誤信息
                    if (e instanceof org.springframework.web.reactive.function.client.WebClientRequestException) {
                        log.error("Keycloak 請求錯誤: {}", e.getMessage());
                        log.error("請求 URL: {}", tokenUrl);
                        log.error("錯誤類型: {}", e.getClass().getSimpleName());
                    } else if (e instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                        org.springframework.web.reactive.function.client.WebClientResponseException responseEx = 
                            (org.springframework.web.reactive.function.client.WebClientResponseException) e;
                        log.error("Keycloak 響應錯誤: HTTP {} - {}", 
                                responseEx.getStatusCode(), responseEx.getResponseBodyAsString());
                    } else {
                        log.error("處理 OAuth 重定向時發生錯誤", e);
                    }
                    exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    return exchange.getResponse().setComplete();
                });
    }

    /**
     * 使用提供的 refresh token 呼叫 Keycloak 的登出 API，撤銷更新憑證
     * 
     * 此方法將 refresh token 與 client 資訊作為參數傳遞至 Keycloak 登出端點，
     * 若成功則回傳登出成功訊息；若失敗則回傳錯誤訊息。
     *
     * @param refreshToken 用於登出的更新憑證
     * @return Mono<ResponseEntity> 包含登出操作結果的訊息與狀態碼
     */
    @CrossOrigin
    @PostMapping("/logout")
    public Mono<org.springframework.http.ResponseEntity<String>> logout(
            @RequestParam("refreshToken") String refreshToken,
            @RequestParam(value = "idToken", required = false) String idToken) {
        log.info("收到登出請求，refreshToken 長度: {}, idToken: {}", 
                refreshToken != null ? refreshToken.length() : 0, 
                idToken != null ? "存在" : "不存在");
        
        // Step 1: 撤銷 refresh token（使用 /logout 端點）
        // 端點: POST /realms/{realm}/protocol/openid-connect/logout
        // 參數: client_id, client_secret, refresh_token
        String logoutUrl = ssoUrl + "/realms/" + realm + "/protocol/openid-connect/logout";
        log.info("Step 1: 調用 Keycloak logout 端點撤銷 refresh token: {}", logoutUrl);

        MultiValueMap<String, String> bodyParams = new LinkedMultiValueMap<>();
        bodyParams.add("client_id", clientId);
        bodyParams.add("client_secret", clientSecret);
        bodyParams.add("refresh_token", refreshToken);

        Mono<String> revokeTokenResult = getWebClient()
                .post()
                .uri(logoutUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(bodyParams))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                    log.warn("Keycloak logout 端點返回錯誤: HTTP {} (token 可能已過期)", response.statusCode());
                    return response.bodyToMono(String.class)
                            .doOnNext(errorBody -> log.warn("Keycloak logout 錯誤響應: {}", errorBody))
                            .flatMap(errorBody -> Mono.error(new RuntimeException("Keycloak logout failed: " + response.statusCode())));
                })
                .bodyToMono(String.class)
                .doOnSuccess(result -> {
                    log.info("✅ Step 1 完成: Refresh token 已撤銷，響應: {}", result);
                })
                .doOnError(error -> {
                    log.warn("⚠️ Step 1 警告: Keycloak logout 端點調用失敗 (token 可能已過期)", error);
                })
                .onErrorReturn("Token revocation completed (may have been already invalid)");

        // Step 2: 如果有 id_token，調用 end_session_endpoint 清除服務器端 session
        if (idToken != null && !idToken.isEmpty()) {
            // 端點: GET /realms/{realm}/protocol/openid-connect/logout (end_session_endpoint)
            // 參數: id_token_hint, post_logout_redirect_uri (可選)
            String endSessionUrl = ssoUrl + "/realms/" + realm + "/protocol/openid-connect/logout" +
                    "?id_token_hint=" + java.net.URLEncoder.encode(idToken, StandardCharsets.UTF_8) +
                    "&post_logout_redirect_uri=" + java.net.URLEncoder.encode(frontendUrl, StandardCharsets.UTF_8);
            
            log.info("Step 2: 調用 Keycloak end_session_endpoint 清除服務器端 session: {}", endSessionUrl);
            
            Mono<String> endSessionResult = getWebClient()
                    .get()
                    .uri(endSessionUrl)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                        log.warn("Keycloak end_session_endpoint 返回錯誤: HTTP {}", response.statusCode());
                        return response.bodyToMono(String.class)
                                .doOnNext(errorBody -> log.warn("Keycloak end_session 錯誤響應: {}", errorBody))
                                .flatMap(errorBody -> Mono.error(new RuntimeException("Keycloak end_session failed: " + response.statusCode())));
                    })
                    .bodyToMono(String.class)
                    .doOnSuccess(result -> {
                        log.info("✅ Step 2 完成: 服務器端 session 已清除，響應: {}", result);
                    })
                    .doOnError(error -> {
                        log.warn("⚠️ Step 2 警告: Keycloak end_session_endpoint 調用失敗", error);
                    })
                    .onErrorReturn("End session completed (may have failed)");

            // 等待兩個操作都完成
            return Mono.zip(revokeTokenResult, endSessionResult)
                    .then(Mono.just(org.springframework.http.ResponseEntity.ok(MessageKey.LOGOUT_SUCCESS.getMessage())));
        } else {
            log.warn("⚠️ 沒有 id_token，無法調用 end_session_endpoint 清除服務器端 session");
            log.warn("⚠️ 只有 refresh token 被撤銷，服務器端 session 可能仍然存在");
            return revokeTokenResult
                    .then(Mono.just(org.springframework.http.ResponseEntity.ok(MessageKey.LOGOUT_SUCCESS.getMessage())));
        }
    }

    /**
     * 檢查指定的 access token 是否有效，並在必要時使用 refresh token 進行續期
     * 
     * 此方法會先呼叫 Keycloak 的 introspection 端點檢查存取憑證 (access token) 的有效性，
     * 若 token 有效則直接回傳檢查結果；若 token 無效且同時提供了 refresh token，則會嘗試透過 refresh token 來刷新存取憑證，
     * 若刷新成功，則回傳新取得的 token 資訊並增加 "refreshed" 標記；若刷新失敗，則回傳未授權狀態。
     *
     * @param token 要檢查的存取憑證 (access token)
     * @param refreshToken (可選) 用於刷新存取憑證的更新憑證 (refresh token)
     * @return Mono<ResponseEntity> 包含 token 檢查結果、刷新後的 token 資訊或錯誤訊息的回應
     */
    @CrossOrigin
    @PostMapping(value = "/introspect", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<org.springframework.http.ResponseEntity<?>> introspectToken(
            ServerWebExchange exchange) {
        
        // 從 form-urlencoded body 中讀取參數
        return exchange.getFormData()
                .flatMap(formData -> {
                    String token = formData.getFirst("token");
                    String refreshToken = formData.getFirst("refreshToken");
                    
                    if (token == null || token.isEmpty()) {
                        log.error("Token 參數缺失");
                        return Mono.just(org.springframework.http.ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body("Required parameter 'token' is not present."));
                    }
                    
                    return introspectTokenInternal(token, refreshToken);
                });
    }
    
    private Mono<org.springframework.http.ResponseEntity<?>> introspectTokenInternal(
            String token,
            String refreshToken) {

        String introspectUrl = ssoUrl + "/realms/" + realm + "/protocol/openid-connect/token/introspect";
        String tokenUrl = ssoUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        // Step 1: 驗證 access token 是否仍有效
        MultiValueMap<String, String> bodyParams = new LinkedMultiValueMap<>();
        bodyParams.add("client_id", clientId);
        bodyParams.add("client_secret", clientSecret);
        bodyParams.add("token", token);

        return getWebClient()
                .post()
                .uri(introspectUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(bodyParams))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                    log.error("Keycloak introspection 請求失敗: HTTP {}", response.statusCode());
                    return response.bodyToMono(String.class)
                            .doOnNext(errorBody -> log.error("Keycloak introspection 錯誤響應: {}", errorBody))
                            .flatMap(errorBody -> {
                                // 返回錯誤響應，保持原始狀態碼
                                org.springframework.web.reactive.function.client.WebClientResponseException exception = 
                                    org.springframework.web.reactive.function.client.WebClientResponseException.create(
                                        response.statusCode().value(),
                                        response.statusCode().toString(),
                                        response.headers().asHttpHeaders(),
                                        errorBody.getBytes(),
                                        java.nio.charset.StandardCharsets.UTF_8
                                    );
                                return Mono.error(exception);
                            });
                })
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .flatMap(result -> {
                    if (result == null) {
                        return Mono.just(org.springframework.http.ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(ErrorCode.TOKEN_INTROSPECT_FAILED.getMessage()));
                    }

                    // Step 2: 如果 token 還有效，直接回傳
                    if (Boolean.TRUE.equals(result.get("active"))) {
                        return Mono.just(org.springframework.http.ResponseEntity.ok(result));
                    }

                    // Step 3: token 無效，嘗試用 refresh token 取得新 token
                    if (refreshToken != null && !refreshToken.isEmpty()) {
                        MultiValueMap<String, String> refreshParams = new LinkedMultiValueMap<>();
                        refreshParams.add("grant_type", "refresh_token");
                        refreshParams.add("client_id", clientId);
                        refreshParams.add("client_secret", clientSecret);
                        refreshParams.add("refresh_token", refreshToken);

                        return getWebClient()
                                .post()
                                .uri(tokenUrl)
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .body(BodyInserters.fromFormData(refreshParams))
                                .retrieve()
                                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                                    log.error("Keycloak token refresh 請求失敗: HTTP {}", response.statusCode());
                                    return response.bodyToMono(String.class)
                                            .doOnNext(errorBody -> log.error("Keycloak token refresh 錯誤響應: {}", errorBody))
                                            .flatMap(errorBody -> {
                                                org.springframework.web.reactive.function.client.WebClientResponseException exception = 
                                                    org.springframework.web.reactive.function.client.WebClientResponseException.create(
                                                        response.statusCode().value(),
                                                        response.statusCode().toString(),
                                                        response.headers().asHttpHeaders(),
                                                        errorBody.getBytes(),
                                                        java.nio.charset.StandardCharsets.UTF_8
                                                    );
                                                return Mono.error(exception);
                                            });
                                })
                                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                                .flatMap(refreshResult -> {
                                    if (refreshResult == null || refreshResult.get("access_token") == null) {
                                        return Mono.just(org.springframework.http.ResponseEntity
                                                .status(HttpStatus.UNAUTHORIZED)
                                                .body(ErrorCode.TOKEN_REFRESH_FAILED.getMessage()));
                                    }
                                    // 回傳新的 access token 及相關資訊
                                    return Mono.just(org.springframework.http.ResponseEntity.ok(refreshResult));
                                });
                    }

                    // Step 4: 無法刷新，回傳 UNAUTHORIZED
                    return Mono.just(org.springframework.http.ResponseEntity
                            .status(HttpStatus.UNAUTHORIZED)
                            .body(ErrorCode.TOKEN_INVALID_OR_REFRESH_FAILED.getMessage()));
                })
                .onErrorResume(e -> {
                    if (e instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                        org.springframework.web.reactive.function.client.WebClientResponseException responseEx = 
                            (org.springframework.web.reactive.function.client.WebClientResponseException) e;
                        log.error("Keycloak introspection 錯誤: HTTP {} - {}", 
                                responseEx.getStatusCode(), responseEx.getResponseBodyAsString());
                        
                        // 返回原始錯誤狀態碼和消息
                        String errorBody = responseEx.getResponseBodyAsString();
                        return Mono.just(org.springframework.http.ResponseEntity
                                .status(responseEx.getStatusCode())
                                .body(errorBody != null ? errorBody : ErrorCode.TOKEN_CHECK_FAILED.getMessage()));
                    } else {
                        log.error("內省失敗", e);
                        return Mono.just(org.springframework.http.ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ErrorCode.TOKEN_CHECK_FAILED.getMessage()));
                    }
                });
    }
}

