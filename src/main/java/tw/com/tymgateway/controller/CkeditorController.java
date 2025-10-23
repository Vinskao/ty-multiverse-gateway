package tw.com.tymgateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tw.com.tymgateway.grpc.client.CkeditorGrpcClient;
import tw.com.tymgateway.dto.CkeditorDTO;
import tw.com.tymgateway.util.JwtUtil;
import tw.com.tymgateway.dto.GetContentDTO;
import tw.com.tymgateway.dto.EditContentVO;

import java.util.Map;

/**
 * CKEditor Controller
 *
 * <p>處理 CKEditor (富文本編輯器) 的 HTTP 請求，通過 gRPC 調用 Backend</p>
 * <p>架構：Frontend (HTTP) → Gateway (gRPC) → Backend (Redis)</p>
 *
 * @author TY Team
 * @version 1.0
 */
@RestController
@RequestMapping("/ckeditor")
@ConditionalOnProperty(name = "grpc.client.enabled", havingValue = "true")
public class CkeditorController {

    private static final Logger logger = LoggerFactory.getLogger(CkeditorController.class);

    @Autowired
    private CkeditorGrpcClient ckeditorGrpcClient;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 保存編輯器內容
     *
     * @param editorContent 編輯器內容
     * @param token JWT token
     * @return 保存結果
     */
    @PostMapping("/save-content")
    public ResponseEntity<?> saveContent(@RequestBody EditContentVO editorContent,
                                       @RequestHeader("Authorization") String token) {
        logger.info("📝 HTTP 請求: 保存內容，編輯器={}, 內容長度={}",
                   editorContent.getEditor(), editorContent.getContent().length());

        try {
            String userId = jwtUtil.extractUserId(token);

            CkeditorDTO result = ckeditorGrpcClient.saveContent(
                userId,
                editorContent.getEditor(),
                editorContent.getContent(),
                token
            );

            logger.info("✅ 內容保存成功: {}", result.getMessage());
            return ResponseEntity.ok(result.getMessage());

        } catch (Exception e) {
            logger.error("❌ 保存內容失敗: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body("Failed to save content: " + e.getMessage());
        }
    }

    /**
     * 獲取編輯器內容
     *
     * @param editor 編輯器名稱
     * @param token JWT token
     * @return 內容數據
     */
    @PostMapping("/get-content")
    public ResponseEntity<?> getContent(@RequestBody GetContentDTO editor,
                                      @RequestHeader("Authorization") String token) {
        logger.info("📖 HTTP 請求: 獲取內容，編輯器={}", editor.getEditor());

        try {
            CkeditorDTO result = ckeditorGrpcClient.getContent(editor.getEditor(), token);

            if (result.isSuccess()) {
                EditContentVO content = new EditContentVO(result.getEditor(), result.getMessage());
                logger.info("✅ 內容獲取成功");
                return ResponseEntity.ok(content);
            } else {
                logger.warn("⚠️ 內容不存在，返回空內容");
                return ResponseEntity.ok(new EditContentVO(editor.getEditor(), ""));
            }

        } catch (Exception e) {
            logger.error("❌ 獲取內容失敗: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error: " + e.getMessage());
        }
    }

    /**
     * 保存草稿
     *
     * @param editorContent 編輯器內容
     * @param token JWT token
     * @return 保存結果
     */
    @PostMapping("/save-draft")
    public ResponseEntity<?> saveDraft(@RequestBody EditContentVO editorContent,
                                     @RequestHeader("Authorization") String token) {
        logger.info("💾 HTTP 請求: 保存草稿，編輯器={}, 內容長度={}",
                   editorContent.getEditor(), editorContent.getContent().length());

        try {
            String userId = jwtUtil.extractUserId(token);

            CkeditorDTO result = ckeditorGrpcClient.saveDraft(
                userId,
                editorContent.getEditor(),
                editorContent.getContent(),
                token
            );

            logger.info("✅ 草稿保存成功: {}", result.getMessage());
            return ResponseEntity.ok(result.getMessage());

        } catch (Exception e) {
            logger.error("❌ 保存草稿失敗: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body("Failed to save draft: " + e.getMessage());
        }
    }

    /**
     * 獲取草稿
     *
     * @param request 包含編輯器名稱的請求
     * @param token JWT token
     * @return 草稿內容
     */
    @PostMapping("/get-draft")
    public ResponseEntity<?> getDraft(@RequestBody Map<String, String> request,
                                    @RequestHeader("Authorization") String token) {
        logger.info("📝 HTTP 請求: 獲取草稿，編輯器={}", request.get("editor"));

        try {
            String userId = jwtUtil.extractUserId(token);
            String editor = request.get("editor");

            CkeditorDTO result = ckeditorGrpcClient.getDraft(userId, editor, token);

            if (result.isSuccess()) {
                EditContentVO content = new EditContentVO(result.getEditor(), result.getMessage());
                logger.info("✅ 草稿獲取成功");
                return ResponseEntity.ok(content);
            } else {
                logger.warn("⚠️ 草稿不存在，返回空內容");
                return ResponseEntity.ok(new EditContentVO(editor, ""));
            }

        } catch (Exception e) {
            logger.error("❌ 獲取草稿失敗: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error: " + e.getMessage());
        }
    }
}
