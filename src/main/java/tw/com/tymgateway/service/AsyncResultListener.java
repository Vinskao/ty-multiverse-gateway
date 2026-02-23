package tw.com.tymgateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tw.com.tymgateway.config.RabbitMQConfig;
import tw.com.tymgateway.dto.AsyncResultMessage;

/**
 * 異步結果監聽器
 *
 * Gateway 監聽 async-result 隊列，接收 Consumer 發送的處理結果
 * 收到結果後，通知等待中的請求並回傳給前端
 */
@Service
public class AsyncResultListener {

    private static final Logger logger = LoggerFactory.getLogger(AsyncResultListener.class);

    @Autowired
    private AsyncResultRegistry asyncResultRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 監聽異步結果隊列
     *
     * 直接接收 raw Message 並手動用 Jackson 反序列化，
     * 避免因 Consumer 未帶 __TypeId__ header 導致的 MessageConversionException。
     */
    @RabbitListener(queues = RabbitMQConfig.ASYNC_RESULT_QUEUE)
    public void handleAsyncResult(Message rawMessage) {
        String body = new String(rawMessage.getBody());
        logger.debug("📥 Gateway 收到原始異步結果訊息: {}", body);

        try {
            AsyncResultMessage resultMessage = objectMapper.readValue(body, AsyncResultMessage.class);

            logger.info("📥 Gateway 收到異步結果: requestId={}, status={}, source={}",
                    resultMessage.getRequestId(), resultMessage.getStatus(), resultMessage.getSource());

            asyncResultRegistry.complete(resultMessage);

            logger.info("✅ 已發送異步結果到註冊中心: requestId={}, status={}",
                    resultMessage.getRequestId(), resultMessage.getStatus());

        } catch (Exception e) {
            logger.error("❌ 解析或處理異步結果訊息失敗: body={}, error={}", body, e.getMessage(), e);
        }
    }
}
