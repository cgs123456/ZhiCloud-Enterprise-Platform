package cn.zhicloud.framework.integration.config;

import cn.zhicloud.framework.integration.job.EventRelayJob;
import cn.zhicloud.framework.integration.service.EventInboxService;
import cn.zhicloud.framework.integration.service.EventInboxServiceImpl;
import cn.zhicloud.framework.integration.service.EventOutboxService;
import cn.zhicloud.framework.integration.service.EventOutboxServiceImpl;
import cn.zhicloud.framework.integration.service.EventRelayService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 可靠集成数据面自动配置（V100）
 *
 * <p>与共享库存 Starter 同理：Mapper 位于 {@code cn.zhicloud.framework.integration.dal.mysql}，
 * 不在业务模块默认的 {@code zhicloud.info.base-package}（{@code cn.zhicloud.module}）扫描范围内，
 * 故用 {@link MapperScan} 显式限定包，避免与全局扫描冲突。
 *
 * <p>阶段 1 只落库 + 单库中继，不引入 MQ：调度由宿主机应用按其定时框架调用
 * {@link EventRelayService#relayOnce(int)}，本 Starter 不自带定时器，避免在 framework 层引入作业依赖。
 *
 * @author 智云集成治理
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "zhicloud.integration", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(IntegrationProperties.class)
@MapperScan("cn.zhicloud.framework.integration.dal.mysql")
public class IntegrationAutoConfiguration {

    @Bean
    public EventOutboxService eventOutboxService() {
        return new EventOutboxServiceImpl();
    }

    @Bean
    public EventInboxService eventInboxService() {
        return new EventInboxServiceImpl();
    }

    @Bean
    public EventRelayService eventRelayService() {
        return new EventRelayService();
    }

    @Bean
    @ConditionalOnProperty(prefix = "zhicloud.integration", name = "relay-enabled",
            havingValue = "true", matchIfMissing = true)
    public EventRelayJob eventRelayJob() {
        return new EventRelayJob();
    }

    /**
     * 中继定时任务依赖 Spring 的 {@code @Scheduled} 基础设施。
     * <p>这里显式开启而不是假设宿主已开启：如果宿主没有 {@code @EnableScheduling}，
     * 定时任务会「静默不执行」——bean 存在、启动日志正常、但事件永不排空，属于最难排查的一类故障。
     * 代价是 Spring 自带调度器被启用（{@code @EnableScheduling} 幂等，与 Quartz 不冲突）。
     */
    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    @ConditionalOnProperty(prefix = "zhicloud.integration", name = "relay-enabled",
            havingValue = "true", matchIfMissing = true)
    public static class RelaySchedulingConfiguration {
    }

}