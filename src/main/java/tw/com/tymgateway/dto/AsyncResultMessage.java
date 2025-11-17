package tw.com.tymgateway.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 異步結果消息 DTO
 *
 * 用於接收來自 Consumer 的異步處理結果
 *
 * @author TY Gateway Team
 * @version 1.0
 * @since 2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsyncResultMessage {
    
    /**
     * 請求ID
     */
    @JsonProperty("requestId")
    private String requestId;
    
    /**
     * 處理狀態: completed, failed
     */
    @JsonProperty("status")
    private String status;
    
    /**
     * 處理結果數據
     */
    @JsonProperty("data")
    private Object data;
    
    /**
     * 錯誤信息（當 status=failed 時）
     */
    @JsonProperty("error")
    private String error;
    
    /**
     * 消息來源
     */
    @JsonProperty("source")
    private String source;
    
    /**
     * 時間戳
     */
    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private String timestamp;
}

