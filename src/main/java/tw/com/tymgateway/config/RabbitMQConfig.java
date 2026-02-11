package tw.com.tymgateway.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 * 
 * Gateway 只需要監聽 async-result 隊列
 * 
 * @author TY Gateway Team
 * @version 1.0
 * @since 2025
 */
@Configuration
public class RabbitMQConfig {

    /**
     * 交換機名稱（與 Backend/Consumer 保持一致）
     */
    public static final String TYMB_EXCHANGE = "tymb-exchange";

    /**
     * 異步結果隊列名稱
     */
    public static final String ASYNC_RESULT_QUEUE = "async-result";

    /**
     * 異步結果路由鍵
     */
    public static final String ASYNC_RESULT_ROUTING_KEY = "async.result";

    /**
     * 創建交換機
     */
    @Bean
    public DirectExchange tymbExchange() {
        return new DirectExchange(TYMB_EXCHANGE, true, false);
    }

    /**
     * 創建異步結果隊列
     *
     * 注意：這個隊列應該已經由 Consumer 創建，這裡聲明是為了確保 Gateway 可以監聽
     * 需要與 Consumer 的配置保持一致，包括 TTL 設置
     */
    @Bean
    public Queue asyncResultQueue() {
        return QueueBuilder.durable(ASYNC_RESULT_QUEUE)
                .withArgument("x-message-ttl", 300000) // 5分鐘 TTL，與 Consumer 保持一致
                .build();
    }

    /**
     * 綁定異步結果隊列到交換機
     */
    @Bean
    public Binding asyncResultBinding(Queue asyncResultQueue, DirectExchange tymbExchange) {
        return BindingBuilder.bind(asyncResultQueue)
                .to(tymbExchange)
                .with(ASYNC_RESULT_ROUTING_KEY);
    }

    /**
     * 配置 JSON 消息轉換器
     */
    /**
     * 配置 JSON 消息轉換器
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setClassMapper(classMapper());
        return converter;
    }

    @Bean
    public org.springframework.amqp.support.converter.DefaultClassMapper classMapper() {
        org.springframework.amqp.support.converter.DefaultClassMapper classMapper = new org.springframework.amqp.support.converter.DefaultClassMapper();
        classMapper.setTrustedPackages("*");
        java.util.Map<String, Class<?>> idClassMapping = new java.util.HashMap<>();
        idClassMapping.put("com.vinskao.ty_multiverse_consumer.core.dto.AsyncResultMessage",
                tw.com.tymgateway.dto.AsyncResultMessage.class);
        classMapper.setIdClassMapping(idClassMapping);
        return classMapper;
    }

    /**
     * 配置 RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
