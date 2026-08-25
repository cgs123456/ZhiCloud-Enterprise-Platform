package cn.zhicloud.framework.websocket.config;

import cn.zhicloud.framework.mq.redis.config.ZhiCloudRedisMQConsumerAutoConfiguration;
import cn.zhicloud.framework.mq.redis.core.RedisMQTemplate;
import cn.zhicloud.framework.websocket.core.handler.JsonWebSocketMessageHandler;
import cn.zhicloud.framework.websocket.core.listener.WebSocketMessageListener;
import cn.zhicloud.framework.websocket.core.security.LoginUserHandshakeInterceptor;
import cn.zhicloud.framework.websocket.core.security.WebSocketAuthorizeRequestsCustomizer;
import cn.zhicloud.framework.websocket.core.sender.kafka.KafkaWebSocketMessageConsumer;
import cn.zhicloud.framework.websocket.core.sender.kafka.KafkaWebSocketMessageSender;
import cn.zhicloud.framework.websocket.core.sender.local.LocalWebSocketMessageSender;
import cn.zhicloud.framework.websocket.core.sender.rabbitmq.RabbitMQWebSocketMessageConsumer;
import cn.zhicloud.framework.websocket.core.sender.rabbitmq.RabbitMQWebSocketMessageSender;
import cn.zhicloud.framework.websocket.core.sender.redis.RedisWebSocketMessageConsumer;
import cn.zhicloud.framework.websocket.core.sender.redis.RedisWebSocketMessageSender;
import cn.zhicloud.framework.websocket.core.sender.rocketmq.RocketMQWebSocketMessageConsumer;
import cn.zhicloud.framework.websocket.core.sender.rocketmq.RocketMQWebSocketMessageSender;
import cn.zhicloud.framework.websocket.core.session.WebSocketSessionHandlerDecorator;
import cn.zhicloud.framework.websocket.core.session.WebSocketSessionManager;
import cn.zhicloud.framework.websocket.core.session.WebSocketSessionManagerImpl;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Arrays;
import java.util.List;

/**
 * WebSocket 自动配置
 *
 * @author xingyu4j
 */
@AutoConfiguration(before = ZhiCloudRedisMQConsumerAutoConfiguration.class) // before ZhiCloudRedisMQConsumerAutoConfiguration 的原因是，需要保证 RedisWebSocketMessageConsumer 先创建，才能创建 RedisMessageListenerContainer
@EnableWebSocket // 开启 websocket
@ConditionalOnProperty(prefix = "zhicloud.websocket", value = "enable", matchIfMissing = true) // 允许使用 zhicloud.websocket.enable=false 禁用 websocket
@EnableConfigurationProperties(WebSocketProperties.class)
public class ZhiCloudWebSocketAutoConfiguration {

    /**
     * CORS 允许的源地址白名单（逗号分隔），与 web 模块 ZhiCloudWebAutoConfiguration 的
     * {@code zhicloud.web.cors.allowed-origins} 同源配置。
     */
    @Value("${zhicloud.web.cors.allowed-origins:}")
    private String corsAllowedOrigins;

    /**
     * CORS 允许的源地址模式白名单（逗号分隔），优先级高于 {@link #corsAllowedOrigins}
     */
    @Value("${zhicloud.web.cors.allowed-origin-patterns:}")
    private String corsAllowedOriginPatterns;

    @Bean
    public WebSocketConfigurer webSocketConfigurer(HandshakeInterceptor[] handshakeInterceptors,
                                                   WebSocketHandler webSocketHandler,
                                                   WebSocketProperties webSocketProperties) {
        return registry -> registry
                // 添加 WebSocketHandler
                .addHandler(webSocketHandler, webSocketProperties.getPath())
                .addInterceptors(handshakeInterceptors)
                // Origin 白名单收敛：优先复用 web 模块的 CORS 白名单配置。
                // 安全提示：生产环境必须通过 zhicloud.web.cors.allowed-origins（或 allowed-origin-patterns）
                // 配置具体域名白名单，防止任意站点发起 WebSocket 跨站劫持（CSWSH）；未配置时保持通配以兼容存量部署
                .setAllowedOriginPatterns(resolveAllowedOriginPatterns());
    }

    /**
     * 解析 WebSocket 握手允许的 Origin 列表：
     * 优先使用 {@code zhicloud.web.cors.allowed-origin-patterns}，其次 {@code zhicloud.web.cors.allowed-origins}，
     * 均为空时保持 "*" 兼容现状
     */
    private String[] resolveAllowedOriginPatterns() {
        String patterns = !corsAllowedOriginPatterns.isEmpty() ? corsAllowedOriginPatterns : corsAllowedOrigins;
        if (patterns.isBlank()) {
            return new String[]{"*"};
        }
        return Arrays.stream(patterns.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
    }

    @Bean
    public HandshakeInterceptor handshakeInterceptor() {
        return new LoginUserHandshakeInterceptor();
    }

    @Bean
    public WebSocketHandler webSocketHandler(WebSocketSessionManager sessionManager,
                                             List<? extends WebSocketMessageListener<?>> messageListeners) {
        // 1. 创建 JsonWebSocketMessageHandler 对象，处理消息
        JsonWebSocketMessageHandler messageHandler = new JsonWebSocketMessageHandler(messageListeners);
        // 2. 创建 WebSocketSessionHandlerDecorator 对象，处理连接
        return new WebSocketSessionHandlerDecorator(messageHandler, sessionManager);
    }

    @Bean
    public WebSocketSessionManager webSocketSessionManager() {
        return new WebSocketSessionManagerImpl();
    }

    @Bean
    public WebSocketAuthorizeRequestsCustomizer webSocketAuthorizeRequestsCustomizer(WebSocketProperties webSocketProperties) {
        return new WebSocketAuthorizeRequestsCustomizer(webSocketProperties);
    }

    // ==================== Sender 相关 ====================

    @Configuration
    @ConditionalOnProperty(prefix = "zhicloud.websocket", name = "sender-type", havingValue = "local")
    public class LocalWebSocketMessageSenderConfiguration {

        @Bean
        public LocalWebSocketMessageSender localWebSocketMessageSender(WebSocketSessionManager sessionManager) {
            return new LocalWebSocketMessageSender(sessionManager);
        }

    }

    @Configuration
    @ConditionalOnProperty(prefix = "zhicloud.websocket", name = "sender-type", havingValue = "redis")
    public class RedisWebSocketMessageSenderConfiguration {

        @Bean
        public RedisWebSocketMessageSender redisWebSocketMessageSender(WebSocketSessionManager sessionManager,
                                                                       RedisMQTemplate redisMQTemplate) {
            return new RedisWebSocketMessageSender(sessionManager, redisMQTemplate);
        }

        @Bean
        public RedisWebSocketMessageConsumer redisWebSocketMessageConsumer(
                RedisWebSocketMessageSender redisWebSocketMessageSender) {
            return new RedisWebSocketMessageConsumer(redisWebSocketMessageSender);
        }

    }

    @Configuration
    @ConditionalOnProperty(prefix = "zhicloud.websocket", name = "sender-type", havingValue = "rocketmq")
    public class RocketMQWebSocketMessageSenderConfiguration {

        @Bean
        public RocketMQWebSocketMessageSender rocketMQWebSocketMessageSender(
                WebSocketSessionManager sessionManager, RocketMQTemplate rocketMQTemplate,
                @Value("${zhicloud.websocket.sender-rocketmq.topic}") String topic) {
            return new RocketMQWebSocketMessageSender(sessionManager, rocketMQTemplate, topic);
        }

        @Bean
        public RocketMQWebSocketMessageConsumer rocketMQWebSocketMessageConsumer(
                RocketMQWebSocketMessageSender rocketMQWebSocketMessageSender) {
            return new RocketMQWebSocketMessageConsumer(rocketMQWebSocketMessageSender);
        }

    }

    @Configuration
    @ConditionalOnProperty(prefix = "zhicloud.websocket", name = "sender-type", havingValue = "rabbitmq")
    public class RabbitMQWebSocketMessageSenderConfiguration {

        @Bean
        public RabbitMQWebSocketMessageSender rabbitMQWebSocketMessageSender(
                WebSocketSessionManager sessionManager, RabbitTemplate rabbitTemplate,
                TopicExchange websocketTopicExchange) {
            return new RabbitMQWebSocketMessageSender(sessionManager, rabbitTemplate, websocketTopicExchange);
        }

        @Bean
        public RabbitMQWebSocketMessageConsumer rabbitMQWebSocketMessageConsumer(
                RabbitMQWebSocketMessageSender rabbitMQWebSocketMessageSender) {
            return new RabbitMQWebSocketMessageConsumer(rabbitMQWebSocketMessageSender);
        }

        /**
         * 创建 Topic Exchange
         */
        @Bean
        public TopicExchange websocketTopicExchange(@Value("${zhicloud.websocket.sender-rabbitmq.exchange}") String exchange) {
            return new TopicExchange(exchange,
                    true,  // durable: 是否持久化
                    false);  // exclusive: 是否排它
        }

    }

    @Configuration
    @ConditionalOnProperty(prefix = "zhicloud.websocket", name = "sender-type", havingValue = "kafka")
    public class KafkaWebSocketMessageSenderConfiguration {

        @Bean
        public KafkaWebSocketMessageSender kafkaWebSocketMessageSender(
                WebSocketSessionManager sessionManager, KafkaTemplate<Object, Object> kafkaTemplate,
                @Value("${zhicloud.websocket.sender-kafka.topic}") String topic) {
            return new KafkaWebSocketMessageSender(sessionManager, kafkaTemplate, topic);
        }

        @Bean
        public KafkaWebSocketMessageConsumer kafkaWebSocketMessageConsumer(
                KafkaWebSocketMessageSender kafkaWebSocketMessageSender) {
            return new KafkaWebSocketMessageConsumer(kafkaWebSocketMessageSender);
        }

    }

}