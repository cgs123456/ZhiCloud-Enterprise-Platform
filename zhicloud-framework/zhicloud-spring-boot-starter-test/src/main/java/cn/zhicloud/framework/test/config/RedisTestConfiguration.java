package cn.zhicloud.framework.test.config;

import com.github.fppt.jedismock.RedisServer;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.io.IOException;

/**
 * Redis 测试 Configuration，主要实现内嵌 Redis 的启动
 *
 * @author 智云
 */
@Configuration(proxyBeanMethods = false)
@Lazy(false) // 禁止延迟加载
@EnableConfigurationProperties(RedisProperties.class)
public class RedisTestConfiguration {

    /**
     * 创建模拟的 Redis Server 服务器
     */
    @Bean(destroyMethod = "stop") // 上下文关闭时释放端口，避免一次跑多个测试上下文时端口越占越多
    public RedisServer redisServer(RedisProperties properties) throws IOException {
        // 先按配置端口起；起不来就退回临时端口，并把真正绑上的端口回写给 RedisProperties。
        // 老写法是"固定端口 + catch (Exception ignore)"：端口被上一个上下文占住时内嵌服务其实没起来，
        // 客户端于是连到机器上真实存在的 Redis（例如 6379 上别人的实例）——
        // 表现要么是 NOAUTH/连接失败，要么更糟：测试直接往真实例里写键。
        RedisServer redisServer = startQuietly(properties.getPort());
        if (redisServer == null) {
            redisServer = startQuietly(0); // 交给内核选端口
        }
        if (redisServer == null) {
            throw new IllegalStateException("[redisServer][内嵌 Redis 起不来，宁可直接失败，也不让测试连到真实 Redis 实例]");
        }
        properties.setPort(redisServer.getBindPort());
        return redisServer;
    }

    /**
     * @param port 监听端口，0 表示由内核分配
     * @return 启动成功的服务器；端口不可用时返回 null
     */
    private static RedisServer startQuietly(int port) {
        RedisServer candidate = new RedisServer(port);
        try {
            candidate.start();
            return candidate;
        } catch (Exception ex) {
            return null;
        }
    }

}
