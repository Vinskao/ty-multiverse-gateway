package tw.com.tymgateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tw.com.tymgateway.grpc.client.PeopleGrpcClient;
import tw.com.tymgateway.dto.PeopleData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * People 模組的 gRPC Gateway Controller
 *
 * <p>接收 HTTP 請求，透過 gRPC 呼叫 Backend 服務</p>
 *
 * @author TY Team
 * @version 1.0
 */
@RestController
@RequestMapping("/tymgateway/tymb/people")
@ConditionalOnProperty(name = "grpc.client.enabled", havingValue = "true")
public class PeopleController {

    private static final Logger logger = LoggerFactory.getLogger(PeopleController.class);

    @Autowired
    private PeopleGrpcClient peopleGrpcClient;

    /**
     * 透過 gRPC 取得所有 People
     *
     * API 端點: GET /tymgateway/tymb/people/get-all
     *
     * 回應格式:
     * {
     *   "success": true,
     *   "message": "People retrieved successfully via gRPC",
     *   "people": [...],  // 人物列表
     *   "count": 153      // 總數
     * }
     */
    @GetMapping("/get-all")
    public ResponseEntity<Map<String, Object>> getAllPeople() {
        try {
            logger.info("Gateway: Received HTTP request to get all people, calling Backend via gRPC...");
            List<PeopleData> people = peopleGrpcClient.getAllPeople();
            
            // 將 Protobuf 對象轉換為 Map，避免序列化問題
            List<Map<String, Object>> peopleList = people.stream()
                    .map(this::convertToMap)
                    .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "People retrieved successfully via gRPC");
            response.put("people", peopleList);
            response.put("count", peopleList.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Gateway: Error calling gRPC service", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve people: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 透過 gRPC 根據名稱取得 People
     *
     * API 端點: POST /tymgateway/tymb/people/get-by-name
     *
     * 請求格式:
     * {
     *   "name": "人物名稱"
     * }
     *
     * 回應格式:
     * {
     *   "success": true,
     *   "message": "People retrieved successfully via gRPC",
     *   "people": {...}  // 人物數據
     * }
     */
    @PostMapping("/get-by-name")
    public ResponseEntity<Map<String, Object>> getPeopleByName(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            logger.info("Gateway: Received HTTP request to get people by name: {}, calling Backend via gRPC...", name);
            
            PeopleData people = peopleGrpcClient.getPeopleByName(name)
                    .orElse(null);
            
            Map<String, Object> response = new HashMap<>();
            if (people != null) {
                response.put("success", true);
                response.put("message", "People retrieved successfully via gRPC");
                response.put("people", convertToMap(people));
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "People not found: " + name);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            logger.error("Gateway: Error calling gRPC service", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve people: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 透過 gRPC 新增 People
     */
    @PostMapping("/insert")
    public ResponseEntity<Map<String, Object>> insertPeople(@RequestBody Map<String, Object> request) {
        try {
            logger.info("Gateway: Received HTTP request to insert people, calling Backend via gRPC...");
            
            PeopleData peopleData = convertToPeopleData(request);
            PeopleData insertedPeople = peopleGrpcClient.insertPeople(peopleData);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "People inserted successfully via gRPC");
            response.put("people", convertToMap(insertedPeople));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Gateway: Error calling gRPC service", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to insert people: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 透過 gRPC 更新 People
     */
    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updatePeople(@RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            logger.info("Gateway: Received HTTP request to update people: {}, calling Backend via gRPC...", name);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> peopleMap = (Map<String, Object>) request.get("people");
            PeopleData peopleData = convertToPeopleData(peopleMap);
            
            PeopleData updatedPeople = peopleGrpcClient.updatePeople(name, peopleData);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "People updated successfully via gRPC");
            response.put("people", convertToMap(updatedPeople));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Gateway: Error calling gRPC service", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to update people: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 透過 gRPC 刪除 People
     */
    @PostMapping("/delete")
    public ResponseEntity<Map<String, Object>> deletePeople(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            logger.info("Gateway: Received HTTP request to delete people: {}, calling Backend via gRPC...", name);
            
            boolean success = peopleGrpcClient.deletePeople(name);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "People deleted successfully via gRPC" : "Failed to delete people");
            
            return success ? ResponseEntity.ok(response) : 
                           ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            logger.error("Gateway: Error calling gRPC service", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to delete people: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * API 文檔
     */
    @GetMapping("/docs")
    public ResponseEntity<Map<String, Object>> getApiDocs() {
        Map<String, Object> docs = new HashMap<>();
        docs.put("title", "TY Multiverse Gateway - People API");
        docs.put("description", "透過 gRPC 調用後端 People 服務的 API Gateway");

        List<Map<String, Object>> endpoints = new ArrayList<>();

        // get-all endpoint
        Map<String, Object> getAll = new HashMap<>();
        getAll.put("method", "GET");
        getAll.put("path", "/tymgateway/tymb/people/get-all");
        getAll.put("description", "獲取所有人物數據");
        getAll.put("response", Map.of(
            "success", true,
            "message", "People retrieved successfully via gRPC",
            "people", "Array of people objects",
            "count", "Total count of people"
        ));
        endpoints.add(getAll);

        // get-by-name endpoint
        Map<String, Object> getByName = new HashMap<>();
        getByName.put("method", "POST");
        getByName.put("path", "/tymgateway/tymb/people/get-by-name");
        getByName.put("description", "根據名稱獲取特定人物");
        getByName.put("request", Map.of("name", "人物名稱"));
        getByName.put("response", Map.of(
            "success", true,
            "message", "People retrieved successfully via gRPC",
            "people", "Single people object"
        ));
        endpoints.add(getByName);

        // insert endpoint
        Map<String, Object> insert = new HashMap<>();
        insert.put("method", "POST");
        insert.put("path", "/tymgateway/tymb/people/insert");
        insert.put("description", "新增人物");
        endpoints.add(insert);

        docs.put("endpoints", endpoints);
        docs.put("baseUrl", "http://localhost:8082");

        return ResponseEntity.ok(docs);
    }

    /**
     * 將 PeopleData 轉換成 Map
     */
    private Map<String, Object> convertToMap(PeopleData peopleData) {
        Map<String, Object> map = new HashMap<>();
        // 基本信息
        map.put("name", peopleData.getName());
        map.put("name_original", peopleData.getNameOriginal());
        map.put("code_name", peopleData.getCodeName());
        
        // 力量屬性
        map.put("physic_power", peopleData.getPhysicPower());
        map.put("magic_power", peopleData.getMagicPower());
        map.put("utility_power", peopleData.getUtilityPower());
        
        // 其他基本信息
        map.put("dob", peopleData.getDob());
        map.put("race", peopleData.getRace());
        map.put("attributes", peopleData.getAttributes());
        map.put("gender", peopleData.getGender());
        map.put("profession", peopleData.getProfession());
        map.put("age", peopleData.getAge());
        
        // 更多字段根據需要添加...
        return map;
    }

    /**
     * 將 Map 轉換成 PeopleData
     */
    private PeopleData convertToPeopleData(Map<String, Object> map) {
        PeopleData peopleData = new PeopleData();

        // 基本信息
        if (map.containsKey("name")) peopleData.setName((String) map.get("name"));
        if (map.containsKey("name_original")) peopleData.setNameOriginal((String) map.get("name_original"));
        if (map.containsKey("code_name")) peopleData.setCodeName((String) map.get("code_name"));

        // 力量屬性
        if (map.containsKey("physic_power")) peopleData.setPhysicPower(((Number) map.get("physic_power")).intValue());
        if (map.containsKey("magic_power")) peopleData.setMagicPower(((Number) map.get("magic_power")).intValue());
        if (map.containsKey("utility_power")) peopleData.setUtilityPower(((Number) map.get("utility_power")).intValue());

        // 其他基本信息
        if (map.containsKey("dob")) peopleData.setDob((String) map.get("dob"));
        if (map.containsKey("race")) peopleData.setRace((String) map.get("race"));
        if (map.containsKey("attributes")) peopleData.setAttributes((String) map.get("attributes"));
        if (map.containsKey("gender")) peopleData.setGender((String) map.get("gender"));
        if (map.containsKey("profession")) peopleData.setProfession((String) map.get("profession"));
        if (map.containsKey("age")) peopleData.setAge(((Number) map.get("age")).intValue());

        // 更多字段根據需要添加...
        return peopleData;
    }
}

