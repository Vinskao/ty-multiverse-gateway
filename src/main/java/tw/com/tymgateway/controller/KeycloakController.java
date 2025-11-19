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

    @Autowired
    private WebClient.Builder webClientBuilder;

    private WebClient getWebClient() {
        return webClientBuilder.build();
    }

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    @Value("${keycloak.auth-server-url}")
    private String ssoUrl;

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

        // 使用 WebClient 發送 POST 請求給 Keycloak 的 token endpoint
        return getWebClient()
                .post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(tokenParams))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                    log.error("Keycloak token 請求失敗: HTTP {}", response.statusCode());
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

                    log.info("Access Token: {}", accessToken);
                    log.info("Refresh Token: {}", refreshToken);

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
    public Mono<org.springframework.http.ResponseEntity<String>> logout(@RequestParam("refreshToken") String refreshToken) {
        // 組合 Keycloak 的登出 endpoint URL
        String logoutUrl = ssoUrl + "/realms/" + realm + "/protocol/openid-connect/logout";

        MultiValueMap<String, String> bodyParams = new LinkedMultiValueMap<>();
        bodyParams.add("client_id", clientId);
        bodyParams.add("client_secret", clientSecret);
        bodyParams.add("refresh_token", refreshToken);

        return getWebClient()
                .post()
                .uri(logoutUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(bodyParams))
                .retrieve()
                .bodyToMono(String.class)
                .then(Mono.just(org.springframework.http.ResponseEntity.ok(MessageKey.LOGOUT_SUCCESS.getMessage())))
                .onErrorResume(e -> {
                    log.error("登出失敗", e);
                    return Mono.just(org.springframework.http.ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ErrorCode.LOGOUT_FAILED.getMessage()));
                });
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
    @PostMapping("/introspect")
    public Mono<org.springframework.http.ResponseEntity<?>> introspectToken(
            @RequestParam("token") String token,
            @RequestParam(value = "refreshToken", required = false) String refreshToken) {

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
                    log.error("內省失敗", e);
                    return Mono.just(org.springframework.http.ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ErrorCode.TOKEN_CHECK_FAILED.getMessage()));
                });
    }
}

