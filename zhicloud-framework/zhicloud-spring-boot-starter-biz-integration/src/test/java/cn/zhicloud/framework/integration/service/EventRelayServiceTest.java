package cn.zhicloud.framework.integration.service;

import cn.zhicloud.framework.datasource.config.ZhiCloudDataSourceAutoConfiguration;
import cn.zhicloud.framework.integration.api.EventAppendReq;
import cn.zhicloud.framework.integration.api.EventConsumer;
import cn.zhicloud.framework.integration.config.IntegrationAutoConfiguration;
import cn.zhicloud.framework.integration.config.IntegrationProperties;
import cn.zhicloud.framework.integration.dal.dataobject.EventOutboxDO;
import cn.zhicloud.framework.integration.dal.mysql.EventInboxMapper;
import cn.zhicloud.framework.integration.dal.mysql.EventOutboxMapper;
import cn.zhicloud.framework.mybatis.config.ZhiCloudMybatisAutoConfiguration;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.framework.test.config.SqlInitializationTestConfiguration;
import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.github.yulichang.autoconfigure.MybatisPlusJoinAutoConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.zhicloud.framework.integration.enums.EventStatusConstants.INBOX_PROCESSED;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.OUTBOX_NEW;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.OUTBOX_PUBLISHED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 单库事件中继测试（V100）：领取 → 分发 → 确认/重试 的闭环
 *
 * @author 智云集成治理
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = EventRelayServiceTest.TestApplication.class)
@ActiveProfiles("unit-test")
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class EventRelayServiceTest {

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
    private EventOutboxService eventOutboxService;

    @Resource
    private EventInboxService eventInboxService;

    @Resource
    private EventOutboxMapper eventOutboxMapper;

    @Resource
    private EventInboxMapper eventInboxMapper;

    @Resource
    private IntegrationProperties properties;

    @BeforeEach
    public void setUpTenantContext() {
        TenantContextHolder.setTenantId(0L);
    }

    @AfterEach
    public void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    public void testRelayPublishesAndConsumesExactlyOnce() {
        CountingConsumer consumer = new CountingConsumer(false);
        EventRelayService relay = newRelay(List.of(consumer));
        append("relay-1");

        assertEquals(1, relay.relayOnce(10));
        assertEquals(1, consumer.calls.get());
        EventOutboxDO event = eventOutboxMapper.selectByEventIdAndTenantId("relay-1", 0L);
        assertEquals(OUTBOX_PUBLISHED, event.getStatus());
        assertNotNull(event.getPublishedAt());
        assertEquals(INBOX_PROCESSED, eventInboxService.selectByConsumerAndEvent("test-consumer", "relay-1", 0L).getStatus());

        // 再跑一轮：已完成的事件不应被重复消费
        assertEquals(0, relay.relayOnce(10));
        assertEquals(1, consumer.calls.get(), "中继重复扫描不应导致业务重复执行");
    }

    @Test
    public void testRelayRetryOnConsumerFailure() {
        CountingConsumer consumer = new CountingConsumer(true);
        EventRelayService relay = newRelay(List.of(consumer));
        append("relay-2");

        assertEquals(0, relay.relayOnce(10), "消费者失败时不应标记投递完成");
        assertEquals(1, consumer.calls.get());
        EventOutboxDO event = eventOutboxMapper.selectByEventIdAndTenantId("relay-2", 0L);
        assertEquals(OUTBOX_NEW, event.getStatus());
        assertEquals(1, event.getRetryCount());
        assertNull(event.getPublishedAt());
        assertTrue(event.getNextRetryAt().isAfter(LocalDateTime.now()), "失败后必须进入退避窗口，避免无间隔热重试");
        assertEquals("FAILED", eventInboxService.selectByConsumerAndEvent("test-consumer", "relay-2", 0L).getStatus());
    }

    @Test
    public void testRelaySkipsEventsInBackoffWindow() {
        CountingConsumer consumer = new CountingConsumer(true);
        EventRelayService relay = newRelay(List.of(consumer));
        append("relay-3");

        assertEquals(0, relay.relayOnce(10));
        // 退避窗口内再扫描：不应领取、不应再次执行业务
        assertEquals(0, relay.relayOnce(10));
        assertEquals(1, consumer.calls.get());
        assertEquals(1, eventOutboxMapper.selectByEventIdAndTenantId("relay-3", 0L).getRetryCount());
    }

    @Test
    public void testRelayWithoutMatchingConsumerLeavesEventRetryable() {
        CountingConsumer consumer = new CountingConsumer(false);
        EventRelayService relay = newRelay(List.of(consumer));
        append("relay-4", "erp.sale-in.approved.v1");

        assertEquals(0, relay.relayOnce(10), "无匹配消费者时必须 fail-closed，不能把事件伪装成投递成功");
        EventOutboxDO event = eventOutboxMapper.selectByEventIdAndTenantId("relay-4", 0L);
        assertEquals(OUTBOX_NEW, event.getStatus());
        assertEquals(1, event.getRetryCount());
        assertNull(event.getPublishedAt());
        assertNotNull(event.getNextRetryAt());
        assertEquals(0, consumer.calls.get());
        assertEquals(0L, eventInboxMapper.selectCount(null));
    }

    @Test
    public void testRelayRespectsBatchSize() {
        CountingConsumer consumer = new CountingConsumer(false);
        EventRelayService relay = newRelay(List.of(consumer));
        append("relay-5");
        append("relay-6");
        append("relay-7");

        assertEquals(2, relay.relayOnce(2), "单轮只处理批大小允许的事件数");
        assertEquals(2, consumer.calls.get());
        assertEquals(1, relay.relayOnce(10));
        assertEquals(3, consumer.calls.get());
    }

    // -------------------- 辅助 --------------------

    private void append(String eventId) {
        append(eventId, "erp.sale-out.approved.v1");
    }

    private void append(String eventId, String eventType) {
        eventOutboxService.append(EventAppendReq.builder()
                .eventId(eventId)
                .tenantId(0L)
                .aggregateType("erp_sale_out")
                .aggregateId("1001")
                .eventType(eventType)
                .payload("{\"id\":1001}")
                .build());
    }

    private EventRelayService newRelay(List<EventConsumer> consumers) {
        EventRelayService relay = new EventRelayService();
        setField(relay, "eventOutboxService", eventOutboxService);
        setField(relay, "eventInboxService", eventInboxService);
        setField(relay, "properties", properties);
        relay.setConsumers(consumers);
        return relay;
    }

    private void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 计数消费者：{@code fail=true} 时恒定失败，用于验证重试路径
     */
    private static class CountingConsumer implements EventConsumer {

        private final AtomicInteger calls = new AtomicInteger();
        private final boolean fail;

        CountingConsumer(boolean fail) {
            this.fail = fail;
        }

        @Override
        public String name() {
            return "test-consumer";
        }

        @Override
        public boolean supports(String eventType) {
            return eventType != null && eventType.startsWith("erp.sale-out.");
        }

        @Override
        public void handle(EventOutboxDO event) {
            calls.incrementAndGet();
            if (fail) {
                throw new IllegalStateException("consumer failed: " + event.getEventId());
            }
        }
    }

}