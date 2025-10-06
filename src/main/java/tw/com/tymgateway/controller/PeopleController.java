package tw.com.tymgateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tw.com.tymgateway.grpc.client.PeopleGrpcClient;
import tw.com.tymgateway.grpc.people.PeopleData;
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
     */
    @GetMapping("/get-all")
    public ResponseEntity<Map<String, Object>> getAllPeople() {
        try {
            logger.info("Gateway: Received HTTP request to get all people, calling Backend via gRPC...");
            List<PeopleData> people = peopleGrpcClient.getAllPeople();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "People retrieved successfully via gRPC");
            response.put("people", people);
            response.put("count", people.size());
            
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
        PeopleData.Builder builder = PeopleData.newBuilder();
        
        // 基本信息
        if (map.containsKey("name")) builder.setName((String) map.get("name"));
        if (map.containsKey("name_original")) builder.setNameOriginal((String) map.get("name_original"));
        if (map.containsKey("code_name")) builder.setCodeName((String) map.get("code_name"));
        
        // 力量屬性
        if (map.containsKey("physic_power")) builder.setPhysicPower(((Number) map.get("physic_power")).intValue());
        if (map.containsKey("magic_power")) builder.setMagicPower(((Number) map.get("magic_power")).intValue());
        if (map.containsKey("utility_power")) builder.setUtilityPower(((Number) map.get("utility_power")).intValue());
        
        // 其他基本信息
        if (map.containsKey("dob")) builder.setDob((String) map.get("dob"));
        if (map.containsKey("race")) builder.setRace((String) map.get("race"));
        if (map.containsKey("attributes")) builder.setAttributes((String) map.get("attributes"));
        if (map.containsKey("gender")) builder.setGender((String) map.get("gender"));
        if (map.containsKey("profession")) builder.setProfession((String) map.get("profession"));
        if (map.containsKey("age")) builder.setAge(((Number) map.get("age")).intValue());
        
        // 更多字段根據需要添加...
        return builder.build();
    }
}

