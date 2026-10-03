package cn.zhicloud.framework.integration.service;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.framework.common.pojo.PageParam;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.datasource.config.ZhiCloudDataSourceAutoConfiguration;
import cn.zhicloud.framework.integration.api.EventAppendReq;
import cn.zhicloud.framework.integration.api.EventConsumer;
import cn.zhicloud.framework.integration.config.IntegrationAutoConfiguration;
import cn.zhicloud.framework.integration.dal.dataobject.EventDlqReplayLogDO;
import cn.zhicloud.framework.integration.dal.dataobject.EventInboxDO;
import cn.zhicloud.framework.integration.dal.dataobject.EventOutboxDO;
import cn.zhicloud.framework.integration.dal.mysql.EventDlqReplayLogMapper;
import cn.zhicloud.framework.integration.dal.mysql.EventInboxMapper;
import cn.zhicloud.framework.integration.dal.mysql.EventOutboxMapper;
import cn.zhicloud.framework.mybatis.config.ZhiCloudMybatisAutoConfiguration;
import cn.zhicloud.framework.mybatis.core.util.MyBatisUtils;
import cn.zhicloud.framework.tenant.config.TenantProperties;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.zhicloud.framework.tenant.core.util.TenantUtils;
import cn.zhicloud.framework.test.config.SqlInitializationTestConfiguration;
import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.github.yulichang.autoconfigure.MybatisPlusJoinAutoConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.List;

import static cn.zhicloud.framework.integration.enums.EventStatusConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 租户拦截器下的可靠集成数据面回归测试。
 *
 * <p>这里刻意注册与生产相同的 {@link TenantDatabaseInterceptor} + {@link TenantLineInnerInterceptor}，
 * 因为不加载租户拦截器的普通 H2 单测会掩盖“定时任务无租户上下文”“管理端忘记显式租户条件”这类生产问题。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = EventTenantIsolationTest.TestApplication.class)
@ActiveProfiles("unit-test")
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class EventTenantIsolationTest {

    @Import({
            ZhiCloudDataSourceAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            DruidDataSourceAutoConfigure.class,
            SqlInitializationTestConfiguration.class,
            ZhiCloudMybatisAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            MybatisPlusJoinAutoConfiguration.class,
            IntegrationAutoConfiguration.class,
            TenantInterceptorTestConfiguration.class
    })
    public static class TestApplication {
    }

    /**
     * 只装配租户 DB 拦截器，不引入 Web/Redis 等与本回归无关的生产依赖。
     */
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    public static class TenantInterceptorTestConfiguration {

        @Bean
        public TenantProperties tenantProperties() {
            return new TenantProperties();
        }

        @Bean
        public TenantLineInnerInterceptor tenantLineInnerInterceptor(TenantProperties properties,
                                                                     MybatisPlusInterceptor interceptor) {
            TenantLineInnerInterceptor inner = new TenantLineInnerInterceptor(
                    new TenantDatabaseInterceptor(properties));
            MyBatisUtils.addInterceptor(interceptor, inner, 0);
            return inner;
        }
    }

    @Resource
    private EventOutboxService eventOutboxService;

    @Resource
    private EventRelayService eventRelayService;

    @Resource
    private EventInboxService eventInboxService;

    @Resource
    private EventDlqService eventDlqService;

    @Resource
    private EventOutboxMapper eventOutboxMapper;

    @Resource
    private EventInboxMapper eventInboxMapper;

    @Resource
    private EventDlqReplayLogMapper eventDlqReplayLogMapper;

    @BeforeEach
    public void setUp() {
        TenantContextHolder.clear();
        eventRelayService.setConsumers(List.of());
    }

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
        eventRelayService.setConsumers(List.of());
    }

    @Test
    public void testRelayProcessesMultipleTenantsWithoutAmbientTenantContext() {
        append(1L, "evt-tenant-relay-1");
        append(2L, "evt-tenant-relay-2");

        RecordingConsumer consumer = new RecordingConsumer();
        eventRelayService.setConsumers(List.of(consumer));

        assertEquals(2, eventRelayService.relayOnce(10),
                "定时中继必须能在没有租户上下文的调度线程中跨租户排空事件");
        assertEquals(List.of(1L, 2L), consumer.tenantIds,
                "每个消费者处理事件时必须进入事件所属租户上下文");

        assertPublished(1L, "evt-tenant-relay-1");
        assertPublished(2L, "evt-tenant-relay-2");
    }

    @Test
    public void testSameEventIdIsIndependentAcrossTenants() {
        append(1L, "evt-tenant-shared-id");
        append(2L, "evt-tenant-shared-id");

        RecordingConsumer consumer = new RecordingConsumer();
        eventRelayService.setConsumers(List.of(consumer));

        assertEquals(2, eventRelayService.relayOnce(10),
                "同一业务事件编号在不同租户中必须各自投递一次");
        assertEquals(List.of(1L, 2L), consumer.tenantIds,
                "消费者必须分别在事件所属租户上下文中执行");

        assertPublished(1L, "evt-tenant-shared-id");
        assertPublished(2L, "evt-tenant-shared-id");
    }

    @Test
    public void testDeadLetterPageIsTenantScopedEvenWhenTableUsesExplicitTenantColumn() {
        insertOutbox(1L, "evt-tenant-page-1", OUTBOX_DEAD);
        insertOutbox(2L, "evt-tenant-page-2", OUTBOX_DEAD);

        TenantContextHolder.setTenantId(1L);
        PageResult<EventOutboxDO> page = eventDlqService.getDeadOutboxPage(
                new PageParam(), null, null, null, null, null);

        assertEquals(1L, page.getTotal());
        assertEquals("evt-tenant-page-1", page.getList().get(0).getEventId());
        assertEquals(1L, page.getList().get(0).getTenantId());
    }

    @Test
    public void testOutboxStateTransitionsCannotTouchOtherTenant() {
        Long claimableId = insertOutbox(2L, "evt-tenant-claim-other", OUTBOX_NEW);
        Long publishingId = insertOutbox(2L, "evt-tenant-publish-other", OUTBOX_PUBLISHING);
        Long retryId = insertOutbox(2L, "evt-tenant-retry-other", OUTBOX_PUBLISHING);

        TenantContextHolder.setTenantId(1L);
        assertNull(eventOutboxService.claim(claimableId, LocalDateTime.now()),
                "租户 1 的上下文不得领取租户 2 的事件");
        assertFalse(eventOutboxService.markPublished(publishingId, "tenant-1-owner"),
                "租户 1 的上下文不得确认租户 2 的事件");
        eventOutboxService.markRetryOrDead(retryId, "tenant-1-owner",
                new IllegalStateException("cross-tenant"));

        TenantUtils.executeIgnore(() -> {
            assertEquals(OUTBOX_NEW, eventOutboxMapper.selectById(claimableId).getStatus(),
                    "跨租户领取不得改变事件状态");
            assertEquals(OUTBOX_PUBLISHING, eventOutboxMapper.selectById(publishingId).getStatus(),
                    "跨租户确认不得改变事件状态");
            EventOutboxDO retryRow = eventOutboxMapper.selectById(retryId);
            assertEquals(OUTBOX_PUBLISHING, retryRow.getStatus(),
                    "跨租户重试不得改变事件状态");
            assertEquals(3, retryRow.getRetryCount(),
                    "跨租户重试不得递增其他租户的重试次数");
        });
    }

    @Test
    public void testInboxStateTransitionsCannotTouchOtherTenant() {
        Long claimableId = insertInbox(2L, "cross-tenant-consumer", "evt-inbox-claim-other", INBOX_RECEIVED);
        Long processedId = insertInbox(2L, "cross-tenant-consumer", "evt-inbox-process-other", INBOX_RECEIVED);
        Long failedId = insertInbox(2L, "cross-tenant-consumer", "evt-inbox-fail-other", INBOX_FAILED);
        Long deadId = insertInbox(2L, "cross-tenant-consumer", "evt-inbox-dead-other", INBOX_FAILED);

        TenantContextHolder.setTenantId(1L);
        assertEquals(0, eventInboxMapper.claim(claimableId, 1L, "tenant-1-owner",
                LocalDateTime.now(), LocalDateTime.now().plusMinutes(1)),
                "租户 1 不得抢占租户 2 的收件箱记录");
        assertEquals(0, eventInboxMapper.markProcessed(processedId, 1L, "tenant-1-owner", LocalDateTime.now()),
                "租户 1 不得完成租户 2 的收件箱记录");
        assertEquals(0, eventInboxMapper.markFailed(failedId, 1L, "tenant-1-owner", "cross-tenant"),
                "租户 1 不得把租户 2 的记录标记为失败");
        assertEquals(0, eventInboxMapper.markDead(deadId, 1L, "tenant-1-owner", "cross-tenant"),
                "租户 1 不得把租户 2 的记录标记为死信");

        TenantUtils.executeIgnore(() -> {
            assertEquals(INBOX_RECEIVED, eventInboxMapper.selectById(claimableId).getStatus());
            assertEquals(INBOX_RECEIVED, eventInboxMapper.selectById(processedId).getStatus());
            EventInboxDO failedRow = eventInboxMapper.selectById(failedId);
            assertEquals(INBOX_FAILED, failedRow.getStatus());
            assertEquals(0, failedRow.getRetryCount(), "跨租户失败标记不得递增重试次数");
            EventInboxDO deadRow = eventInboxMapper.selectById(deadId);
            assertEquals(INBOX_FAILED, deadRow.getStatus());
            assertEquals(0, deadRow.getRetryCount(), "跨租户死信标记不得递增重试次数");
        });
    }

    @Test
    public void testCrossTenantReplayWritesFailureAuditInCurrentTenant() {
        insertOutbox(2L, "evt-tenant-other", OUTBOX_DEAD);

        TenantContextHolder.setTenantId(1L);
        assertThrows(ServiceException.class, () -> eventDlqService.replay(replayCommand("evt-tenant-other")));

        TenantContextHolder.setTenantId(1L);
        List<EventDlqReplayLogDO> logs = eventDlqReplayLogMapper.selectList(
                new LambdaQueryWrapper<EventDlqReplayLogDO>()
                        .eq(EventDlqReplayLogDO::getTenantId, 1L)
                        .eq(EventDlqReplayLogDO::getEventId, "evt-tenant-other"));
        assertEquals(1, logs.size(), "越权重放尝试必须在当前租户留下失败审计，且不得写目标租户审计");
        assertEquals(EventDlqReplayResult.RESULT_FAILED, logs.get(0).getResult());

        TenantUtils.executeIgnore(() -> assertEquals(OUTBOX_DEAD,
                eventOutboxMapper.selectByEventIdAndTenantId("evt-tenant-other", 2L).getStatus(),
                "越权重放不得修改其他租户事件状态"));
    }

    @Test
    public void testAppendDerivesTenantFromCurrentContext() {
        TenantContextHolder.setTenantId(7L);

        Long id = appendWithoutTenant("evt-context-derived");

        TenantUtils.executeIgnore(() -> assertEquals(7L, eventOutboxMapper.selectById(id).getTenantId(),
                "未显式传租户时，事件必须登记到当前上下文租户"));
    }

    @Test
    public void testAppendWithoutTenantContextFailsClosed() {
        TenantContextHolder.clear();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> appendWithoutTenant("evt-no-tenant-context"));

        assertEquals(1_099_001_010, ex.getCode(),
                "无租户上下文时禁止静默写入租户 0");
    }

    @Test
    public void testAppendRejectsExplicitTenantDifferentFromContext() {
        TenantContextHolder.setTenantId(7L);

        ServiceException ex = assertThrows(ServiceException.class, () -> appendDirect(8L, "evt-tenant-mismatch"));

        assertEquals(1_099_001_011, ex.getCode(),
                "显式租户与当前上下文不一致时必须拒绝，防止跨租户登记事件");
        TenantUtils.executeIgnore(() -> assertEquals(0L, eventOutboxMapper.selectCount(
                new LambdaQueryWrapper<EventOutboxDO>().eq(EventOutboxDO::getEventId, "evt-tenant-mismatch"))));
    }

    @Test
    public void testAppendRejectsExplicitTenantWithoutContext() {
        TenantContextHolder.clear();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> appendDirect(8L, "evt-explicit-without-context"));

        assertEquals(1_099_001_010, ex.getCode(),
                "显式租户不能替代调用方缺失的租户上下文，事件登记必须 fail-closed");
    }

    @Test
    public void testInboxConsumeWithoutTenantFailsClosed() {
        TenantContextHolder.clear();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> eventInboxService.consume("tenant-required-consumer", "evt-inbox-no-tenant", "t", null, () -> {
                }));

        assertEquals(1_099_001_010, ex.getCode(), "收件箱消费缺少租户时禁止回退到租户 0");
    }

    @Test
    public void testInboxSelectWithoutTenantFailsClosed() {
        TenantContextHolder.clear();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> eventInboxService.selectByConsumerAndEvent("tenant-required-consumer", "evt-inbox-select", null));

        assertEquals(1_099_001_010, ex.getCode(), "收件箱查询缺少租户时必须显式失败");
    }

    @Test
    public void testInboxReplayWithoutTenantFailsClosed() {
        TenantContextHolder.clear();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> eventInboxService.replay("tenant-required-consumer", "evt-inbox-replay", null));

        assertEquals(1_099_001_010, ex.getCode(), "收件箱重放缺少租户时必须显式失败");
    }

    @Test
    public void testInboxConsumeRejectsExplicitTenantDifferentFromContext() {
        TenantContextHolder.setTenantId(7L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> eventInboxService.consume("tenant-mismatch-consumer", "evt-inbox-mismatch", "t", 8L, () -> {
                }));

        assertEquals(1_099_001_011, ex.getCode(), "收件箱消费显式租户与当前上下文冲突时必须拒绝");
    }

    @Test
    public void testInboxSelectRejectsExplicitTenantDifferentFromContext() {
        TenantContextHolder.setTenantId(7L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> eventInboxService.selectByConsumerAndEvent("tenant-mismatch-consumer", "evt-select-mismatch", 8L));

        assertEquals(1_099_001_011, ex.getCode(), "收件箱查询显式租户与当前上下文冲突时必须拒绝");
    }

    @Test
    public void testInboxReplayRejectsExplicitTenantDifferentFromContext() {
        TenantContextHolder.setTenantId(7L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> eventInboxService.replay("tenant-mismatch-consumer", "evt-replay-mismatch", 8L));

        assertEquals(1_099_001_011, ex.getCode(), "收件箱重放显式租户与当前上下文冲突时必须拒绝");
    }

    private Long appendWithoutTenant(String eventId) {
        return eventOutboxService.append(EventAppendReq.builder()
                .eventId(eventId)
                .aggregateType("erp_sale_out")
                .aggregateId("1001")
                .eventType("erp.sale-out.approved.v1")
                .payload("{\"id\":1001}")
                .build());
    }

    private void appendDirect(Long tenantId, String eventId) {
        eventOutboxService.append(EventAppendReq.builder()
                .eventId(eventId)
                .tenantId(tenantId)
                .aggregateType("erp_sale_out")
                .aggregateId("1001")
                .eventType("erp.sale-out.approved.v1")
                .payload("{\"id\":1001}")
                .build());
    }

    private void append(Long tenantId, String eventId) {
        TenantUtils.execute(tenantId, () -> {
            eventOutboxService.append(EventAppendReq.builder()
                    .eventId(eventId)
                    .tenantId(tenantId)
                    .aggregateType("erp_sale_out")
                    .aggregateId("1001")
                    .eventType("erp.sale-out.approved.v1")
                    .payload("{\"id\":1001}")
                    .build());
            return null;
        });
    }

    private void assertPublished(Long tenantId, String eventId) {
        TenantUtils.execute(tenantId, () -> {
            EventOutboxDO outbox = eventOutboxMapper.selectByEventIdAndTenantId(eventId, tenantId);
            assertNotNull(outbox);
            assertEquals(OUTBOX_PUBLISHED, outbox.getStatus());
            assertEquals(INBOX_PROCESSED, eventInboxMapper
                    .selectByConsumerAndEvent("tenant-recording-consumer", eventId, tenantId).getStatus());
        });
    }

    private Long insertInbox(Long tenantId, String consumerName, String eventId, String status) {
        EventInboxDO row = EventInboxDO.builder()
                .tenantId(tenantId)
                .consumerName(consumerName)
                .eventId(eventId)
                .eventType("erp.sale-out.approved.v1")
                .status(status)
                .retryCount(0)
                .receivedAt(LocalDateTime.now())
                .build();
        TenantUtils.executeIgnore(() -> eventInboxMapper.insert(row));
        return row.getId();
    }

    private Long insertOutbox(Long tenantId, String eventId, String status) {
        EventOutboxDO row = EventOutboxDO.builder()
                .eventId(eventId)
                .aggregateType("erp_sale_out")
                .aggregateId("1001")
                .eventType("erp.sale-out.approved.v1")
                .schemaVersion("v1")
                .partitionKey(tenantId + ":1001")
                .payload("{}")
                .status(status)
                .retryCount(3)
                .nextRetryAt(OUTBOX_NEW.equals(status) ? null : LocalDateTime.now().plusMinutes(1))
                .occurredAt(LocalDateTime.now())
                .lastError("boom")
                .build();
        row.setTenantId(tenantId);
        TenantUtils.executeIgnore(() -> eventOutboxMapper.insert(row));
        return row.getId();
    }

    private EventDlqReplayCommand replayCommand(String eventId) {
        return EventDlqReplayCommand.builder()
                .eventIds(List.of(eventId))
                .operatorId(1001L)
                .operatorName("tenant-a-admin")
                .reason("security regression")
                .fromTime(LocalDateTime.now().minusDays(1))
                .toTime(LocalDateTime.now().plusDays(1))
                .build();
    }

    private static class RecordingConsumer implements EventConsumer {

        private final List<Long> tenantIds = new java.util.ArrayList<>();

        @Override
        public String name() {
            return "tenant-recording-consumer";
        }

        @Override
        public boolean supports(String eventType) {
            return eventType != null && eventType.startsWith("erp.sale-out.");
        }

        @Override
        public void handle(EventOutboxDO event) {
            tenantIds.add(TenantContextHolder.getRequiredTenantId());
        }
    }
}
