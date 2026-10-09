package cn.zhicloud.framework.redis.config;

import org.junit.jupiter.api.Test;
import org.redisson.config.Config;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ZhiCloudRedisAutoConfigurationTest {

    private final RedissonAutoConfigurationCustomizer customizer =
            new ZhiCloudRedisAutoConfiguration().zhicloudRedisPasswordCustomizer();

    @Test
    void blankPasswordIsNormalizedToNull() {
        Config config = new Config();
        config.setPassword("");

        customizer.customize(config);

        assertNull(config.getPassword());
    }

    @Test
    void whitespacePasswordIsNormalizedToNull() {
        Config config = new Config();
        config.setPassword("   ");

        customizer.customize(config);

        assertNull(config.getPassword());
    }

    @Test
    void configuredPasswordIsPreserved() {
        Config config = new Config();
        config.setPassword("secret");

        customizer.customize(config);

        assertEquals("secret", config.getPassword());
    }

    @Test
    void nullPasswordStaysNull() {
        Config config = new Config();

        customizer.customize(config);

        assertNull(config.getPassword());
    }
}
