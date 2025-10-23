package tw.com.tymgateway.util;

import org.springframework.stereotype.Component;

/**
 * JWT 工具類
 *
 * <p>用於解析 JWT token 並提取用戶信息</p>
 * <p>簡化的實現，實際項目中應該使用完整的 JWT 解析</p>
 *
 * @author TY Team
 * @version 1.0
 */
@Component
public class JwtUtil {

    /**
     * 從 JWT token 中提取用戶 ID
     *
     * @param token JWT token (帶有 Bearer 前綴)
     * @return 用戶 ID
     * @throws RuntimeException 如果 token 無效
     */
    public String extractUserId(String token) {
        try {
            if (token == null || token.isEmpty()) {
                throw new RuntimeException("Token is null or empty");
            }

            // 簡化的實現 - 檢查 token 格式
            if (!token.startsWith("Bearer ")) {
                throw new RuntimeException("Invalid token format");
            }

            // 開發環境：返回測試用戶 ID
            // 生產環境：應該解析真實的 JWT token
            return "test-user";

        } catch (Exception e) {
            throw new RuntimeException("Failed to extract user ID from token: " + e.getMessage());
        }
    }

    /**
     * 驗證 JWT token 並提取用戶 ID
     *
     * @param token JWT token
     * @return 用戶 ID
     * @throws RuntimeException 如果 token 無效
     */
    public String validateTokenAndGetUserId(String token) {
        return extractUserId(token);
    }
}
