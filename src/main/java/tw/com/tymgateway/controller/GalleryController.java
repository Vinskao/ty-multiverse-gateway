package tw.com.tymgateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tw.com.tymgateway.grpc.client.GalleryGrpcClient;
import tw.com.tymgateway.dto.GalleryData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gallery 模組的 gRPC Gateway Controller
 *
 * <p>接收 HTTP 請求，透過 gRPC 呼叫 Backend 服務</p>
 *
 * @author TY Team
 * @version 1.0
 */
@RestController
@RequestMapping("/gallery")
@ConditionalOnProperty(name = "grpc.client.enabled", havingValue = "true")
public class GalleryController {

    private static final Logger logger = LoggerFactory.getLogger(GalleryController.class);

    @Autowired
    private GalleryGrpcClient galleryGrpcClient;

    /**
     * 透過 gRPC 取得所有圖片
     *
     * API 端點: GET /gallery/get-all
     *
     * 回應格式:
     * {
     *   "success": true,
     *   "message": "Images retrieved successfully via gRPC",
     *   "images": [...],  // 圖片列表
     *   "count": 25       // 總數
     * }
     */
    @GetMapping("/get-all")
    public ResponseEntity<Map<String, Object>> getAllImages() {
        try {
            logger.info("Gateway: Received HTTP request to get all images, calling Backend via gRPC...");
            List<GalleryData> images = galleryGrpcClient.getAllImages();

            // 將 Protobuf 對象轉換為 Map，避免序列化問題
            List<Map<String, Object>> imagesList = images.stream()
                    .map(this::convertToMap)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Images retrieved successfully via gRPC");
            response.put("images", imagesList);
            response.put("count", imagesList.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Gateway: Error calling gRPC service", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve images: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 透過 gRPC 根據ID取得圖片
     *
     * API 端點: POST /gallery/get-by-id
     *
     * 請求格式:
     * {
     *   "id": 123
     * }
     *
     * 回應格式:
     * {
     *   "success": true,
     *   "message": "Image retrieved successfully via gRPC",
     *   "image": {...}  // 圖片數據
     * }
     */
    @PostMapping("/get-by-id")
    public ResponseEntity<Map<String, Object>> getImageById(@RequestBody Map<String, Integer> request) {
        try {
            Integer id = request.get("id");
            logger.info("Gateway: Received HTTP request to get image by id: {}, calling Backend via gRPC...", id);

            GalleryData image = galleryGrpcClient.getImageById(id)
                    .orElse(null);

            Map<String, Object> response = new HashMap<>();
            if (image != null) {
                response.put("success", true);
                response.put("message", "Image retrieved successfully via gRPC");
                response.put("image", convertToMap(image));
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Image not found: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            logger.error("Gateway: Error calling gRPC service", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve image: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * API 文檔
     */
    @GetMapping("/docs")
    public ResponseEntity<Map<String, Object>> getApiDocs() {
        Map<String, Object> docs = new HashMap<>();
        docs.put("title", "TY Multiverse Gateway - Gallery API");
        docs.put("description", "透過 gRPC 調用後端 Gallery 服務的 API Gateway");

        List<Map<String, Object>> endpoints = new ArrayList<>();

        // get-all endpoint
        Map<String, Object> getAll = new HashMap<>();
        getAll.put("method", "GET");
        getAll.put("path", "/gallery/get-all");
        getAll.put("description", "獲取所有圖片數據");
        getAll.put("response", Map.of(
            "success", true,
            "message", "Images retrieved successfully via gRPC",
            "images", "Array of image objects",
            "count", "Total count of images"
        ));
        endpoints.add(getAll);

        // get-by-id endpoint
        Map<String, Object> getById = new HashMap<>();
        getById.put("method", "POST");
        getById.put("path", "/gallery/get-by-id");
        getById.put("description", "根據ID獲取特定圖片");
        getById.put("request", Map.of("id", "圖片ID"));
        getById.put("response", Map.of(
            "success", true,
            "message", "Image retrieved successfully via gRPC",
            "image", "Single image object"
        ));
        endpoints.add(getById);

        docs.put("endpoints", endpoints);
        docs.put("baseUrl", "http://localhost:8082");

        return ResponseEntity.ok(docs);
    }

    /**
     * 將 GalleryData 轉換成 Map
     */
    private Map<String, Object> convertToMap(GalleryData galleryData) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", galleryData.getId());
        map.put("image_base64", galleryData.getImageBase64());
        map.put("upload_time", galleryData.getUploadTime());
        map.put("version", galleryData.getVersion());
        return map;
    }
}
