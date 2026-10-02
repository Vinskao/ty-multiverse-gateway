package tw.com.tymgateway.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import tw.com.tymgateway.dto.People;
import tw.com.tymgateway.dto.PeopleNameRequestDTO;
import tw.com.tymgateway.service.AsyncResultRegistry;
import org.springframework.beans.factory.annotation.Value;

/**
 * People 模組異步代理 Controller
 *
 * <p>
 * 前端請求 /tymg/people/** 時，Gateway 會：
 * <ol>
 * <li>向 Backend 發送請求，獲得 requestId</li>
 * <li>於 Gateway 端等待 Consumer 實際處理結果</li>
 * <li>將最終資料以 HTTP 200 回傳給前端</li>
 * </ol>
 * </p>
 *
 * <p>
 * 統一使用 /tymg/people/** 路徑，保持 gateway -> backend -> consumer 的流程
 * </p>
 */
@RestController
@RequestMapping("/tymg/people")
public class AsyncPeopleProxyController extends BaseAsyncProxyController {

    public AsyncPeopleProxyController(
            WebClient backendWebClient,
            AsyncResultRegistry asyncResultRegistry,
            @Value("${gateway.async.timeout:30}") long waitTimeoutSeconds) {
        super(backendWebClient, asyncResultRegistry, waitTimeoutSeconds);
    }

    /**
     * 同步獲取所有角色名稱
     *
     * @param authorization Authorization header (可為空)
     * @return 實際角色名稱列表
     */
    @GetMapping(value = "/names", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> getAllPeopleNames(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        logger.info("🔁 Gateway 同步代理請求: /people/names");
        return proxyAsyncBackendCall(
                backendWebClient.get().uri("/people/names"),
                authorization);
    }

    /**
     * 同步插入單個角色
     *
     * @param person        角色數據
     * @param authorization Authorization header (可為空)
     * @return 插入後的角色數據
     */
    @PostMapping(value = "/insert", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> insertPerson(
            @RequestBody People person,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        logger.info("🔁 Gateway 同步代理請求: /people/insert");
        return proxyAsyncBackendCall(
                backendWebClient.post().uri("/people/insert").bodyValue(person),
                authorization);
    }

    /**
     * 同步更新角色
     *
     * @param person        角色數據
     * @param authorization Authorization header (可為空)
     * @return 更新後的角色數據
     */
    @PostMapping(value = "/update", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> updatePerson(
            @RequestBody People person,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        logger.info("🔁 Gateway 同步代理請求: /people/update");
        return proxyAsyncBackendCall(
                backendWebClient.post().uri("/people/update").bodyValue(person),
                authorization);
    }

    /**
     * 同步批量插入角色
     *
     * @param peopleList    角色列表
     * @param authorization Authorization header (可為空)
     * @return 插入後的角色列表
     */
    @PostMapping(value = "/insert-multiple", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> insertMultiplePeople(
            @RequestBody List<People> peopleList,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        logger.info("🔁 Gateway 同步代理請求: /people/insert-multiple, 接收到 {} 個角色", peopleList.size());
        if (!peopleList.isEmpty()) {
            People first = peopleList.get(0);
            logger.info("🔍 第一個角色數據: name={}, codeName={}, dob={}, race={}, gender={}, job={}, email={}, age={}",
                    first.getName(), first.getCodeName(), first.getDob(), first.getRace(),
                    first.getGender(), first.getJob(), first.getEmail(), first.getAge());
        }
        return proxyAsyncBackendCall(
                backendWebClient.post().uri("/people/insert-multiple").bodyValue(peopleList),
                authorization);
    }

    /**
     * 同步獲取所有角色
     *
     * @param authorization Authorization header (可為空)
     * @return 所有角色列表
     */
    @PostMapping(value = "/get-all", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> getAllPeople(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        logger.info("🔁 Gateway 同步代理請求: /people/get-all");
        return proxyAsyncBackendCall(
                backendWebClient.post().uri("/people/get-all"),
                authorization);
    }

    /**
     * 同步根據名稱獲取角色
     *
     * @param request       包含角色名稱的請求體
     * @param authorization Authorization header (可為空)
     * @return 匹配的角色數據
     */
    @PostMapping(value = "/get-by-name", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> getPeopleByName(
            @RequestBody PeopleNameRequestDTO request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        logger.info("🔁 Gateway 同步代理請求: /people/get-by-name");
        return proxyAsyncBackendCall(
                backendWebClient.post().uri("/people/get-by-name").bodyValue(request),
                authorization);
    }

    /**
     * 同步刪除所有角色
     *
     * @param authorization Authorization header (可為空)
     * @return 無內容響應
     */
    @PostMapping(value = "/delete-all", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> deleteAllPeople(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        logger.info("🔁 Gateway 同步代理請求: /people/delete-all");
        return proxyAsyncBackendCall(
                backendWebClient.post().uri("/people/delete-all"),
                authorization);
    }

    /**
     * 同步批量計算傷害
     *
     * @param request       包含名稱列表的請求
     * @param authorization Authorization header (可為空)
     * @return 傷害計算結果列表
     */
    @PostMapping(value = "/batchDamageWithWeapon", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> batchDamageWithWeapon(
            @RequestBody Object request, // 使用 Object 以保持靈活性，直接轉發 payload
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        logger.info("🔁 Gateway 同步代理請求: /people/batchDamageWithWeapon");
        return proxyAsyncBackendCall(
                backendWebClient.post().uri("/people/batchDamageWithWeapon").bodyValue(request),
                authorization);
    }

    /**
     * 同步計算傷害
     *
     * @param name          角色名稱
     * @param authorization Authorization header (可為空)
     * @return 傷害計算結果
     */
    @GetMapping(value = "/damage", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> calculateDamage(
            @RequestParam String name,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        logger.info("🔁 Gateway 同步代理請求: /people/damage?name={}", name);
        return proxyAsyncBackendCall(
                backendWebClient.get().uri(
                        uriBuilder -> uriBuilder.path("/people/damageWithWeapon").queryParam("name", name).build()),
                authorization);
    }

    /**
     * 直接同步計算傷害（不走異步流程）
     * 此端點直接代理到後端的同步 API，立即返回結果
     * 
     * Gateway → Backend 路徑：
     * - Gateway 接收: /tymg/people/damageWithWeapon?name={name}
     * - Gateway 轉發到 Backend: {PUBLIC_TYMB_URL}/people/damageWithWeapon?name={name}
     * - Backend 完整路徑:
     * http://localhost:8080/tymb/people/damageWithWeapon?name={name}
     *
     * @param name          角色名稱
     * @param authorization Authorization header (可為空)
     * @return 傷害計算結果
     */
    @GetMapping(value = "/damageWithWeapon", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> calculateDamageWithWeapon(
            @RequestParam String name,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        // 構建後端完整路徑：/people/damageWithWeapon（WebClient baseUrl 已包含 /tymb）
        String backendPath = "/people/damageWithWeapon";
        String fullBackendUrl = backendPath + "?name=" + name;
        logger.info("🔁 Gateway → Backend 同步代理請求: {} (完整路徑: {})", fullBackendUrl, fullBackendUrl);

        return backendWebClient
                .get()
                .uri(uriBuilder -> uriBuilder.path(backendPath).queryParam("name", name).build())
                .headers(headers -> {
                    if (authorization != null && !authorization.isBlank()) {
                        headers.set(HttpHeaders.AUTHORIZATION, authorization);
                        logger.debug("✅ 已設置 Authorization header");
                    }
                })
                .retrieve()
                .bodyToMono(new ParameterizedBackendResponse())
                .map(response -> {
                    if (response.isSuccess() && response.getData() != null) {
                        logger.info("✅ Gateway → Backend 成功: name={}, damage={}", name, response.getData());
                        // 直接返回數據部分，前端期望的是數字值
                        return ResponseEntity.ok(response.getData());
                    } else {
                        logger.warn("⚠️ Backend 返回錯誤響應: name={}, code={}, message={}", name, response.getCode(),
                                response.getMessage());
                        return ResponseEntity.status(response.getCode())
                                .body((Object) response);
                    }
                })
                .onErrorResume(throwable -> {
                    logger.error("❌ Gateway → Backend 調用失敗: name={}, path={}, error={}", name, backendPath,
                            throwable.getMessage());
                    if (throwable instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                        org.springframework.web.reactive.function.client.WebClientResponseException ex = (org.springframework.web.reactive.function.client.WebClientResponseException) throwable;
                        logger.error("❌ Backend HTTP 錯誤: status={}, body={}", ex.getStatusCode(),
                                ex.getResponseBodyAsString());

                        // 嘗試解析 Backend 返回的錯誤響應
                        try {
                            String responseBody = ex.getResponseBodyAsString();
                            if (responseBody != null && !responseBody.trim().isEmpty()) {
                                // 嘗試解析為 BackendApiResponse 格式
                                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                                java.util.Map<String, Object> errorResponse = mapper.readValue(responseBody,
                                        java.util.Map.class);
                                if (errorResponse.containsKey("message")) {
                                    String backendMessage = (String) errorResponse.get("message");
                                    return Mono.just(ResponseEntity.status(ex.getStatusCode())
                                            .body((Object) backendMessage));
                                }
                            }
                        } catch (Exception parseError) {
                            logger.warn("無法解析 Backend 錯誤響應: {}", parseError.getMessage());
                        }

                        // 如果無法解析，使用原始響應體
                        if (ex.getResponseBodyAsString() != null && !ex.getResponseBodyAsString().trim().isEmpty()) {
                            return Mono.just(ResponseEntity.status(ex.getStatusCode())
                                    .body((Object) ex.getResponseBodyAsString()));
                        }
                    }
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body((Object) "傷害計算失敗"));
                });
    }

}
