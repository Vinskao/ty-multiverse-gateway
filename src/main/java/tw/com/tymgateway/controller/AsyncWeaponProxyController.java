package tw.com.tymgateway.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import tw.com.tymgateway.service.AsyncResultRegistry;
import org.springframework.beans.factory.annotation.Value;

/**
 * Weapon 模組異步代理 Controller
 *
 * <p>
 * 前端請求 /tymg/weapons/** 時，Gateway 會：
 * <ol>
 * <li>向 Backend 發送請求，獲得 requestId</li>
 * <li>於 Gateway 端等待 Consumer 實際處理結果</li>
 * <li>將最終資料以 HTTP 200 回傳給前端</li>
 * </ol>
 * </p>
 */
@RestController
@RequestMapping("/tymg/weapons")
public class AsyncWeaponProxyController extends BaseAsyncProxyController {

    public AsyncWeaponProxyController(
            WebClient backendWebClient,
            AsyncResultRegistry asyncResultRegistry,
            @Value("${gateway.async.timeout:30}") long waitTimeoutSeconds) {
        super(backendWebClient, asyncResultRegistry, waitTimeoutSeconds);
    }

    /**
     * 同步獲取所有武器
     *
     * @param authorization Authorization header (可為空)
     * @return 所有武器列表
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> getAllWeapons(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        logger.info("🔁 Gateway 同步代理請求: GET /weapons");
        return proxyAsyncBackendCall(
                backendWebClient.get().uri("/weapons"),
                authorization);
    }

    /**
     * 同步根據 ID 獲取武器
     *
     * @param weaponId      武器 ID
     * @param authorization Authorization header (可為空)
     * @return 武器數據
     */
    @GetMapping(value = "/{weaponId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> getWeaponById(
            @PathVariable String weaponId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        logger.info("🔁 Gateway 同步代理請求: GET /weapons/{}", weaponId);
        return proxyAsyncBackendCall(
                backendWebClient.get().uri("/weapons/{weaponId}", weaponId),
                authorization);
    }

    /**
     * 同步創建或更新武器
     *
     * @param weapon        武器數據
     * @param authorization Authorization header (可為空)
     * @return 儲存後的武器數據
     */
    @PostMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> saveWeapon(
            @RequestBody Object weapon,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        logger.info("🔁 Gateway 同步代理請求: POST /weapons");
        return proxyAsyncBackendCall(
                backendWebClient.post().uri("/weapons").bodyValue(weapon),
                authorization);
    }

    /**
     * Proxy a batch weapon upload as one request.
     */
    @PostMapping(value = "/insert-multiple", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> insertMultipleWeapons(
            @RequestBody Object weapons,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        logger.info("Gateway proxy request: POST /weapons/insert-multiple");
        return proxyAsyncBackendCall(
                backendWebClient.post().uri("/weapons/insert-multiple").bodyValue(weapons),
                authorization);
    }

    /**
     * 同步刪除單個武器
     *
     * @param weaponId      武器 ID
     * @param authorization Authorization header (可為空)
     * @return 無內容響應
     */
    @DeleteMapping(value = "/{weaponId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> deleteWeapon(
            @PathVariable String weaponId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        logger.info("🔁 Gateway 同步代理請求: DELETE /weapons/{}", weaponId);
        return proxyAsyncBackendCall(
                backendWebClient.delete().uri("/weapons/{weaponId}", weaponId),
                authorization);
    }

    /**
     * 同步刪除所有武器
     *
     * @param authorization Authorization header (可為空)
     * @return 無內容響應
     */
    @DeleteMapping(value = "/delete-all", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> deleteAllWeapons(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        logger.info("🔁 Gateway 同步代理請求: DELETE /weapons/delete-all");
        return proxyAsyncBackendCall(
                backendWebClient.delete().uri("/weapons/delete-all"),
                authorization);
    }
}
