package cn.zhicloud.framework.mq.redis.core.stream;

import org.redisson.api.RedissonClient;
import org.redisson.spring.data.connection.RedissonConnection;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStreamCommands;

/**
 * Redis Stream 容器专用的 Redisson 连接工厂：为 stream 命令挂载空读豁免。
 *
 * <p>为什么需要：Redisson 核心按命令名把 XREAD/XREADGROUP 的空阻塞读回成 Map，而
 * redisson-spring-data 适配器声明的返回是 List 并做了强转，流为空时每次轮询抛
 * ClassCastException（详见 {@link StreamSafeRedisStreamCommands} 的考证）。spring 的
 * {@code DefaultStreamMessageListenerContainer} 只在 {@link RedisConnection#streamCommands()}
 * 这条路径上取命令 —— 在工厂这层覆盖 streamCommands() 即可完整覆盖容器的空轮询，
 * 且不影响其它 Redis 操作（cache、lock、pubsub 各自走自己的工厂/连接）。
 *
 * <p>装配：只应挂到 StreamMessageListenerContainer 的创建点
 * （{@code ZhiCloudRedisMQConsumerAutoConfiguration} 与 {@code IotRedisMessageBus}），
 * 不要替换全局 {@code RedisConnectionFactory} Bean。
 */
public class StreamSafeRedissonConnectionFactory extends RedissonConnectionFactory {

    private final RedissonClient redissonClient;

    public StreamSafeRedissonConnectionFactory(RedissonClient redissonClient) {
        super(redissonClient);
        this.redissonClient = redissonClient;
    }

    @Override
    public RedisConnection getConnection() {
        return new RedissonConnection(redissonClient) {
            @Override
            public RedisStreamCommands streamCommands() {
                return new StreamSafeRedisStreamCommands(super.streamCommands());
            }
        };
    }

}
