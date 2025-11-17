package tw.com.tymgateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    /**
     * 監聽異步結果隊列
     *
     * Spring AMQP 會自動使用配置的 Jackson2JsonMessageConverter 將 JSON 消息轉換為 AsyncResultMessage 對象
     *
     * @param resultMessage 自動反序列化的結果消息對象
     */
    @RabbitListener(queues = RabbitMQConfig.ASYNC_RESULT_QUEUE)
    public void handleAsyncResult(AsyncResultMessage resultMessage) {
        logger.info("📥 Gateway 收到異步結果消息: requestId={}, status={}, source={}",
            resultMessage.getRequestId(), resultMessage.getStatus(), resultMessage.getSource());

        try {
            logger.info("✅ 消息解析成功，數據內容: {}", resultMessage.getData());

            // 通知等待中的請求
            asyncResultRegistry.complete(resultMessage);

            logger.info("✅ 已發送異步結果到註冊中心: requestId={}, status={}",
                resultMessage.getRequestId(), resultMessage.getStatus());

        } catch (Exception e) {
            logger.error("❌ 處理異步結果失敗: requestId={}, error={}",
                resultMessage.getRequestId(), e.getMessage(), e);
        }
    }
}
