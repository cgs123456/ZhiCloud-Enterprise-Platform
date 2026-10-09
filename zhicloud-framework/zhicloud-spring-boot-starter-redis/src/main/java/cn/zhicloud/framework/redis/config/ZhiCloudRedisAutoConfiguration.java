package cn.zhicloud.framework.redis.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.redisson.spring.starter.RedissonAutoConfigurationV2;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis 配置类
 */
@AutoConfiguration(before = RedissonAutoConfigurationV2.class) // 目的：使用自己定义的 RedisTemplate Bean
public class ZhiCloudRedisAutoConfiguration {
    /**
     * Redisson 4.x 会在 password 非 null 时发送 AUTH。Spring 的
     * {@code ${REDIS_PASSWORD:}} 在未设置变量时会绑定为空字符串，而不是 null，
     * 因此无密码 Redis 也会收到空 AUTH。这里只把空字符串归一化为 null，
     * 不改变任何已配置的非空密码。
     */
    @Bean
    public RedissonAutoConfigurationCustomizer zhicloudRedisPasswordCustomizer() {
        return config -> {
            if (config.getPassword() != null && config.getPassword().isBlank()) {
                config.setPassword(null);
            }
        };
    }

    /**
     * 创建 RedisTemplate Bean，使用 JSON 序列化方式
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        // 创建 RedisTemplate 对象
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // 设置 RedisConnection 工厂。😈 它就是实现多种 Java Redis 客户端接入的秘密工厂。感兴趣的胖友，可以自己去撸下。
        template.setConnectionFactory(factory);
        // 使用 String 序列化方式，序列化 KEY 。
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        // 使用 JSON 序列化方式，序列化 VALUE
        RedisSerializer<?> redisSerializer = buildRedisSerializer();
        template.setValueSerializer(redisSerializer);
        template.setHashValueSerializer(redisSerializer);
        return template;
    }

    public static RedisSerializer<?> buildRedisSerializer() {
        // 自建 ObjectMapper，避免通过反射修改 GenericJackson2JsonRedisSerializer 内部 mapper（升级时易失效）
        ObjectMapper objectMapper = new ObjectMapper();
        // 解决 LocalDateTime 的序列化
        objectMapper.registerModule(new JavaTimeModule());
        // 时间序列化为 ISO 格式字符串，便于排查问题
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 携带类型信息，反序列化时可还原为原始类型；与 GenericJackson2JsonRedisSerializer 默认行为保持一致
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        // 注册 NullValue 的序列化支持，保持与原 RedisSerializer.json() 默认构造行为一致
        GenericJackson2JsonRedisSerializer.registerNullValueSerializer(objectMapper, null);
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

}
