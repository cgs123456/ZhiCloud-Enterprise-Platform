package cn.zhicloud.framework.integration.job;

import cn.zhicloud.framework.datasource.config.ZhiCloudDataSourceAutoConfiguration;
import cn.zhicloud.framework.integration.config.IntegrationAutoConfiguration;
import cn.zhicloud.framework.integration.config.IntegrationProperties;
import cn.zhicloud.framework.mybatis.config.ZhiCloudMybatisAutoConfiguration;
import cn.zhicloud.framework.test.config.SqlInitializationTestConfiguration;
import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.github.yulichang.autoconfigure.MybatisPlusJoinAutoConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 中继定时任务的接线测试（V100）
 *
 * <p>验证「开启开关 → bean 存在 → 且 Spring 调度基础设施已就绪」这条链是通的。
 * 少了任何一环，表现都是「启动无报错、事件永不排空」，只能靠这个用例提前拦住。
 * 周期刻意设成 1 小时，保证测试期间任务不会真的去扫库。
 *
 * @author 智云集成治理
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = EventRelayJobWiringTest.TestApplication.class,
        properties = {
                "zhicloud.integration.relay-enabled=true",
                "zhicloud.integration.relay-initial-delay=3600000",
                "zhicloud.integration.relay-fixed-delay=3600000"
        })
@ActiveProfiles("unit-test")
public class EventRelayJobWiringTest {

    @Import({
            ZhiCloudDataSourceAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            DruidDataSourceAutoConfigure.class,
            SqlInitializationTestConfiguration.class,
            ZhiCloudMybatisAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            MybatisPlusJoinAutoConfiguration.class,
            IntegrationAutoConfiguration.class
    })
    public static class TestApplication {
    }

    @Resource
    private ApplicationContext applicationContext;

    @Resource
    private IntegrationProperties properties;

    @Test
    public void testRelayJobRegisteredWhenEnabled() {
        assertTrue(Boolean.TRUE.equals(properties.getRelayEnabled()));
        assertNotNull(applicationContext.getBean(EventRelayJob.class), "开启开关后必须注册中继任务");
        assertNotNull(applicationContext.getBean(ScheduledAnnotationBeanPostProcessor.class),
                "没有 @EnableScheduling 时 @Scheduled 会静默失效，事件永不排空");
        assertEquals(3600000L, properties.getRelayFixedDelay().longValue());
    }

}