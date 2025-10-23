package tw.com.tymgateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tw.com.tymgateway.grpc.client.WeaponGrpcClient;
import tw.com.tymgateway.dto.WeaponData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Weapon 模組的 gRPC Gateway Controller
 *
 * <p>接收 HTTP 請求，透過 gRPC 呼叫 Backend 服務</p>
 *
 * @author TY Team
 * @version 1.0
 */
@RestController
@RequestMapping("/weapons")
@ConditionalOnProperty(name = "grpc.client.enabled", havingValue = "true")
public class WeaponController {

    private static final Logger logger = LoggerFactory.getLogger(WeaponController.class);

    @Autowired
    private WeaponGrpcClient weaponGrpcClient;

    /**
     * 透過 gRPC 取得所有 Weapons
     *
     * API 端點: GET /weapons/get-all
     *
     * 回應格式:
     * {
     *   "success": true,
     *   "message": "Weapons retrieved successfully via gRPC",
     *   "weapons": [...],  // 武器列表
     *   "count": 50        // 總數
     * }
     */
    @GetMapping("/get-all")
    public ResponseEntity<Map<String, Object>> getAllWeapons() {
        try {
            logger.info("Gateway: Received HTTP request to get all weapons, calling Backend via gRPC...");
            List<WeaponData> weapons = weaponGrpcClient.getAllWeapons();

            // 將 Protobuf 對象轉換為 Map，避免序列化問題
            List<Map<String, Object>> weaponsList = weapons.stream()
                    .map(this::convertToMap)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Weapons retrieved successfully via gRPC");
            response.put("weapons", weaponsList);
            response.put("count", weaponsList.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Gateway: Error calling gRPC service", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve weapons: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 透過 gRPC 根據名稱取得 Weapon
     *
     * API 端點: POST /weapons/get-by-name
     *
     * 請求格式:
     * {
     *   "name": "武器名稱"
     * }
     *
     * 回應格式:
     * {
     *   "success": true,
     *   "message": "Weapon retrieved successfully via gRPC",
     *   "weapon": {...}  // 武器數據
     * }
     */
    @PostMapping("/get-by-name")
    public ResponseEntity<Map<String, Object>> getWeaponByName(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            logger.info("Gateway: Received HTTP request to get weapon by name: {}, calling Backend via gRPC...", name);

            WeaponData weapon = weaponGrpcClient.getWeaponById(name)
                    .orElse(null);

            Map<String, Object> response = new HashMap<>();
            if (weapon != null) {
                response.put("success", true);
                response.put("message", "Weapon retrieved successfully via gRPC");
                response.put("weapon", convertToMap(weapon));
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Weapon not found: " + name);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            logger.error("Gateway: Error calling gRPC service", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve weapon: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * API 文檔
     */
    @GetMapping("/docs")
    public ResponseEntity<Map<String, Object>> getApiDocs() {
        Map<String, Object> docs = new HashMap<>();
        docs.put("title", "TY Multiverse Gateway - Weapon API");
        docs.put("description", "透過 gRPC 調用後端 Weapon 服務的 API Gateway");

        List<Map<String, Object>> endpoints = new ArrayList<>();

        // get-all endpoint
        Map<String, Object> getAll = new HashMap<>();
        getAll.put("method", "GET");
        getAll.put("path", "/weapons/get-all");
        getAll.put("description", "獲取所有武器數據");
        getAll.put("response", Map.of(
            "success", true,
            "message", "Weapons retrieved successfully via gRPC",
            "weapons", "Array of weapon objects",
            "count", "Total count of weapons"
        ));
        endpoints.add(getAll);

        // get-by-name endpoint
        Map<String, Object> getByName = new HashMap<>();
        getByName.put("method", "POST");
        getByName.put("path", "/weapons/get-by-name");
        getByName.put("description", "根據名稱獲取特定武器");
        getByName.put("request", Map.of("name", "武器名稱"));
        getByName.put("response", Map.of(
            "success", true,
            "message", "Weapon retrieved successfully via gRPC",
            "weapon", "Single weapon object"
        ));
        endpoints.add(getByName);

        docs.put("endpoints", endpoints);
        docs.put("baseUrl", "http://localhost:8082");

        return ResponseEntity.ok(docs);
    }

    /**
     * 將 WeaponData 轉換成 Map
     */
    private Map<String, Object> convertToMap(WeaponData weaponData) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", weaponData.getName());
        map.put("owner", weaponData.getOwner());
        map.put("attributes", weaponData.getAttributes());
        map.put("base_damage", weaponData.getBaseDamage());
        map.put("bonus_damage", weaponData.getBonusDamage());
        map.put("bonus_attributes", weaponData.getBonusAttributes());
        map.put("state_attributes", weaponData.getStateAttributes());
        map.put("created_at", weaponData.getCreatedAt());
        map.put("updated_at", weaponData.getUpdatedAt());
        map.put("version", weaponData.getVersion());
        return map;
    }
}
