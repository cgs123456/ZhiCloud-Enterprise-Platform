package cn.zhicloud.server.integration;

import cn.zhicloud.framework.datasource.config.ZhiCloudDataSourceAutoConfiguration;
import cn.zhicloud.framework.integration.api.ConsumeResult;
import cn.zhicloud.framework.integration.api.EventAppendReq;
import cn.zhicloud.framework.integration.config.IntegrationAutoConfiguration;
import cn.zhicloud.framework.integration.config.IntegrationProperties;
import cn.zhicloud.framework.integration.dal.dataobject.EventInboxDO;
import cn.zhicloud.framework.integration.dal.dataobject.EventOutboxDO;
import cn.zhicloud.framework.integration.service.EventInboxService;
import cn.zhicloud.framework.integration.service.EventOutboxService;
import cn.zhicloud.framework.mybatis.config.ZhiCloudMybatisAutoConfiguration;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.github.yulichang.autoconfigure.MybatisPlusJoinAutoConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.zhicloud.framework.integration.enums.EventStatusConstants.INBOX_FAILED;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.INBOX_PROCESSED;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.INBOX_PROCESSING;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.OUTBOX_DEAD;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.OUTBOX_NEW;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.OUTBOX_PUBLISHING;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.OUTBOX_PUBLISHED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真实 MySQL owner fencing 矩阵。
 *
 * <p>该测试只针对专用沙箱库运行，用于把 H2 语义测试扩展到真实 MySQL 的
 * DATETIME、InnoDB 行锁和 UPDATE ... WHERE owner = ? 行为。它不是生产拓扑、
 * 复制、故障切换或负载验证，不得据此关闭生产就绪门禁。
 */
@EnabledIfSystemProperty(named = "zhicloud.mysql.integration", matches = "true",
        disabledReason = "真实 MySQL 集成测试需显式开启")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = EventOwnerFencingMysqlIntegrationTest.TestApplication.class,
        properties = {
                "spring.config.location=classpath:/application-integration-mysql.yaml",
                "spring.main.banner-mode=off",
                "spring.flyway.enabled=false",
                "spring.sql.init.mode=never",
                "zhicloud.integration.relay-enabled=false"
        })
public class EventOwnerFencingMysqlIntegrationTest {

    private static final Long TENANT_ID = 99L;
    private static final int THREADS = 8;

    @Import({
            ZhiCloudDataSourceAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            DruidDataSourceAutoConfigure.class,
            ZhiCloudMybatisAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            MybatisPlusJoinAutoConfiguration.class,
            IntegrationAutoConfiguration.class
    })
    public static class TestApplication {
    }

    @Resource
    private EventInboxService eventInboxService;

    @Resource
    private EventOutboxService eventOutboxService;

    @Resource
    private IntegrationProperties properties;

    @Resource
    private JdbcTemplate jdbcTemplate;

    private String consumerName;
    private String inboxEventId;
    private String outboxEventId;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        consumerName = "mysql-matrix-consumer-" + UUID.randomUUID();
        inboxEventId = "mysql-matrix-inbox-" + UUID.randomUUID();
        outboxEventId = "mysql-matrix-outbox-" + UUID.randomUUID();
        cleanup();
    }

    @AfterEach
    void tearDown() {
        try {
            cleanup();
        } finally {
            TenantContextHolder.clear();
        }
    }

    // -------------------- Inbox --------------------

    @Test
    void testInboxActiveLeaseReturnsInProgressWithoutHandler() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        insertInboxProcessing(consumerName, inboxEventId, "owner-active", now.plusSeconds(60), now);
        AtomicInteger handlerCalls = new AtomicInteger();

        ConsumeResult result = consumeInbox(handlerCalls::incrementAndGet);

        assertEquals(ConsumeResult.IN_PROGRESS, result);
        assertEquals(0, handlerCalls.get(), "活跃租约内不得执行 handler");
        EventInboxDO row = eventInboxService.selectByConsumerAndEvent(consumerName, inboxEventId, TENANT_ID);
        assertEquals(INBOX_PROCESSING, row.getStatus());
        assertEquals("owner-active", row.getProcessingOwner());
        assertNotNull(row.getLeaseUntil());
    }

    @Test
    void testInboxExpiredLeaseIsTakenOverAndProcessed() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        insertInboxProcessing(consumerName, inboxEventId, "owner-old", now.minusSeconds(1), now.minusSeconds(30));
        AtomicInteger handlerCalls = new AtomicInteger();

        ConsumeResult result = consumeInbox(handlerCalls::incrementAndGet);

        assertEquals(ConsumeResult.PROCESSED, result);
        assertEquals(1, handlerCalls.get());
        EventInboxDO row = eventInboxService.selectByConsumerAndEvent(consumerName, inboxEventId, TENANT_ID);
        assertEquals(INBOX_PROCESSED, row.getStatus());
        assertNull(row.getProcessingOwner(), "成功后必须清空 owner");
        assertNull(row.getLeaseUntil(), "成功后必须清空 lease");
    }

    @Test
    void testInboxStaleOwnerSettlementFailsClosed() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        insertInboxProcessing(consumerName, inboxEventId, "owner-old", now.minusSeconds(1), now.minusSeconds(30));
        AtomicInteger handlerCalls = new AtomicInteger();

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> consumeInbox(() -> {
            handlerCalls.incrementAndGet();
            // 模拟另一个实例已接管；旧实例的 markProcessed 必须失败。
            jdbcTemplate.update("UPDATE event_inbox SET processing_owner = ? "
                            + "WHERE tenant_id = ? AND consumer_name = ? AND event_id = ?",
                    "owner-new", TENANT_ID, consumerName, inboxEventId);
        }));

        assertTrue(error.getMessage().contains("lease lost before success settlement"));
        assertEquals(1, handlerCalls.get());
        EventInboxDO row = eventInboxService.selectByConsumerAndEvent(consumerName, inboxEventId, TENANT_ID);
        assertEquals(INBOX_PROCESSING, row.getStatus());
        assertEquals("owner-new", row.getProcessingOwner());
        assertNotNull(row.getLeaseUntil());
    }

    @Test
    void testInboxHandlerFailureMarksFailedAndClearsLease() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        insertInboxProcessing(consumerName, inboxEventId, "owner-old", now.minusSeconds(1), now.minusSeconds(30));

        assertThrows(IllegalStateException.class, () -> consumeInbox(() -> {
            throw new IllegalStateException("mysql-inbox-boom");
        }));

        EventInboxDO row = eventInboxService.selectByConsumerAndEvent(consumerName, inboxEventId, TENANT_ID);
        assertEquals(INBOX_FAILED, row.getStatus());
        assertEquals(1, row.getRetryCount());
        assertNull(row.getProcessingOwner());
        assertNull(row.getLeaseUntil());
        assertTrue(row.getError().contains("mysql-inbox-boom"));
    }

    @Test
    void testInboxConcurrentFirstDeliveryHasSingleHandler() throws Exception {
        AtomicInteger handlerCalls = new AtomicInteger();
        CountDownLatch handlerEntered = new CountDownLatch(1);
        CountDownLatch releaseHandler = new CountDownLatch(1);
        CountDownLatch callersReturned = new CountDownLatch(THREADS - 1);
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        List<Future<ConsumeResult>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < THREADS; i++) {
                futures.add(executor.submit(() -> {
                    TenantContextHolder.setTenantId(TENANT_ID);
                    try {
                        try {
                            return consumeInbox(() -> {
                                handlerCalls.incrementAndGet();
                                handlerEntered.countDown();
                                await(releaseHandler);
                            });
                        } finally {
                            callersReturned.countDown();
                        }
                    } finally {
                        TenantContextHolder.clear();
                    }
                }));
            }
            assertTrue(handlerEntered.await(10, TimeUnit.SECONDS), "必须有一个线程进入 handler");
            assertTrue(callersReturned.await(10, TimeUnit.SECONDS),
                    "其余并发调用必须在 winner 结算前返回 IN_PROGRESS");
            releaseHandler.countDown();
            List<ConsumeResult> results = new ArrayList<>();
            for (Future<ConsumeResult> future : futures) {
                results.add(future.get(20, TimeUnit.SECONDS));
            }

            assertEquals(1, handlerCalls.get(), "并发首次投递只能执行一次 handler");
            assertEquals(1, results.stream().filter(r -> r == ConsumeResult.PROCESSED).count());
            assertEquals(THREADS - 1, results.stream().filter(r -> r == ConsumeResult.IN_PROGRESS).count());
            EventInboxDO row = eventInboxService.selectByConsumerAndEvent(consumerName, inboxEventId, TENANT_ID);
            assertEquals(INBOX_PROCESSED, row.getStatus());
        } finally {
            releaseHandler.countDown();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    // -------------------- Outbox --------------------

    @Test
    void testOutboxLeaseBlocksThenAllowsTakeover() {
        Long id = appendOutbox();
        LocalDateTime base = LocalDateTime.now().withNano(0);
        String oldOwner = eventOutboxService.claim(id, base);
        assertNotNull(oldOwner);

        assertNull(eventOutboxService.claim(id, base), "活跃租约内不得二次领取");
        assertNull(eventOutboxService.claim(id, base.plusSeconds(properties.getLeaseSeconds() - 1)),
                "租约到期前不得二次领取");
        String newOwner = eventOutboxService.claim(id, base.plusSeconds(properties.getLeaseSeconds() + 1));
        assertNotNull(newOwner, "租约过期后必须允许接管");
        assertFalse(oldOwner.equals(newOwner));
    }

    @Test
    void testOutboxStaleOwnerCannotPublishOrRetryAfterTakeover() {
        Long id = appendOutbox();
        LocalDateTime base = LocalDateTime.now().withNano(0);
        String oldOwner = eventOutboxService.claim(id, base);
        String newOwner = eventOutboxService.claim(id, base.plusSeconds(properties.getLeaseSeconds() + 1));
        assertNotNull(oldOwner);
        assertNotNull(newOwner);
        assertFalse(oldOwner.equals(newOwner));

        assertFalse(eventOutboxService.markPublished(id, oldOwner), "旧 owner 不得确认发布");
        eventOutboxService.markRetryOrDead(id, oldOwner, new IllegalStateException("stale-owner"));
        EventOutboxDO afterStale = selectOutbox(id);
        assertNotNull(afterStale, "接管后的 PUBLISHING 行仍应存在");
        assertEquals(OUTBOX_PUBLISHING, afterStale.getStatus());
        assertEquals(0, afterStale.getRetryCount());
        assertEquals(newOwner, afterStale.getProcessingOwner());

        assertTrue(eventOutboxService.markPublished(id, newOwner));
        EventOutboxDO published = selectOutbox(id);
        assertEquals(OUTBOX_PUBLISHED, published.getStatus());
        assertNull(published.getProcessingOwner());
        assertNull(published.getLeaseUntil());
    }

    @Test
    void testOutboxStaleOwnerCannotMarkDeadAfterTakeover() {
        Long id = appendOutbox();
        int retryBeforeDead = properties.getMaxRetry() - 1;
        jdbcTemplate.update("UPDATE event_outbox SET retry_count = ? WHERE tenant_id = ? AND id = ?",
                retryBeforeDead, TENANT_ID, id);
        LocalDateTime base = LocalDateTime.now().withNano(0);
        String oldOwner = eventOutboxService.claim(id, base);
        String newOwner = eventOutboxService.claim(id, base.plusSeconds(properties.getLeaseSeconds() + 1));
        assertNotNull(oldOwner);
        assertNotNull(newOwner);
        assertFalse(oldOwner.equals(newOwner));

        eventOutboxService.markRetryOrDead(id, oldOwner, new IllegalStateException("stale-dead"));
        EventOutboxDO afterStale = selectOutbox(id);
        assertEquals(OUTBOX_PUBLISHING, afterStale.getStatus());
        assertEquals(retryBeforeDead, afterStale.getRetryCount());
        assertEquals(newOwner, afterStale.getProcessingOwner());

        eventOutboxService.markRetryOrDead(id, newOwner, new IllegalStateException("new-owner-dead"));
        EventOutboxDO dead = selectOutbox(id);
        assertEquals(OUTBOX_DEAD, dead.getStatus());
        assertEquals(properties.getMaxRetry(), dead.getRetryCount());
        assertNull(dead.getProcessingOwner());
        assertNull(dead.getLeaseUntil());
    }

    @Test
    void testOutboxConcurrentClaimHasSingleWinner() throws Exception {
        Long id = appendOutbox();
        LocalDateTime claimAt = LocalDateTime.now().withNano(0);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        List<Future<String>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < THREADS; i++) {
                futures.add(executor.submit(() -> {
                    TenantContextHolder.setTenantId(TENANT_ID);
                    try {
                        start.await(10, TimeUnit.SECONDS);
                        return eventOutboxService.claim(id, claimAt);
                    } finally {
                        TenantContextHolder.clear();
                    }
                }));
            }
            start.countDown();
            List<String> owners = new ArrayList<>();
            for (Future<String> future : futures) {
                owners.add(future.get(20, TimeUnit.SECONDS));
            }

            long winners = owners.stream().filter(owner -> owner != null).count();
            assertEquals(1L, winners, "并发 claim 只能有一个 winner");
            String winner = owners.stream().filter(owner -> owner != null).findFirst().orElseThrow();
            assertTrue(eventOutboxService.markPublished(id, winner));
            assertEquals(OUTBOX_PUBLISHED, selectOutbox(id).getStatus());
        } finally {
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    // -------------------- 辅助方法 --------------------

    private ConsumeResult consumeInbox(cn.zhicloud.framework.integration.api.EventHandler handler) {
        try {
            return eventInboxService.consume(consumerName, inboxEventId, "mysql.matrix.v1", TENANT_ID, handler);
        } catch (Exception ex) {
            if (ex instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new AssertionError(ex);
        }
    }

    private Long appendOutbox() {
        return eventOutboxService.append(EventAppendReq.builder()
                .eventId(outboxEventId)
                .tenantId(TENANT_ID)
                .aggregateType("mysql_matrix")
                .aggregateId("1")
                .eventType("mysql.matrix.v1")
                .payload("{\"source\":\"mysql-matrix\"}")
                .build());
    }

    private EventOutboxDO selectOutbox(Long id) {
        List<EventOutboxDO> rows = eventOutboxService.selectClaimable(LocalDateTime.now().plusDays(1), 1000);
        EventOutboxDO row = rows.stream().filter(item -> item.getId().equals(id)).findFirst().orElse(null);
        if (row != null) {
            return row;
        }
        // selectClaimable 只返回 NEW/PUBLISHING；终态或已接管行需要直接按主键读取。
        return jdbcTemplate.queryForObject(
                "SELECT id, event_id, tenant_id, aggregate_type, aggregate_id, event_type, schema_version, "
                        + "partition_key, payload, status, processing_owner, lease_until, retry_count, "
                        + "next_retry_at, occurred_at, published_at, trace_id, last_error "
                        + "FROM event_outbox WHERE id = ? AND tenant_id = ?",
                (rs, rowNum) -> EventOutboxDO.builder()
                        .id(rs.getLong("id"))
                        .eventId(rs.getString("event_id"))
                        .tenantId(rs.getLong("tenant_id"))
                        .aggregateType(rs.getString("aggregate_type"))
                        .aggregateId(rs.getString("aggregate_id"))
                        .eventType(rs.getString("event_type"))
                        .schemaVersion(rs.getString("schema_version"))
                        .partitionKey(rs.getString("partition_key"))
                        .payload(rs.getString("payload"))
                        .status(rs.getString("status"))
                        .processingOwner(rs.getString("processing_owner"))
                        .leaseUntil(rs.getTimestamp("lease_until") == null ? null : rs.getTimestamp("lease_until").toLocalDateTime())
                        .retryCount(rs.getInt("retry_count"))
                        .nextRetryAt(rs.getTimestamp("next_retry_at") == null ? null : rs.getTimestamp("next_retry_at").toLocalDateTime())
                        .occurredAt(rs.getTimestamp("occurred_at") == null ? null : rs.getTimestamp("occurred_at").toLocalDateTime())
                        .publishedAt(rs.getTimestamp("published_at") == null ? null : rs.getTimestamp("published_at").toLocalDateTime())
                        .traceId(rs.getString("trace_id"))
                        .lastError(rs.getString("last_error"))
                        .build(),
                id, TENANT_ID);
    }

    private void insertInboxProcessing(String consumer, String eventId, String owner, LocalDateTime leaseUntil,
                                       LocalDateTime receivedAt) {
        jdbcTemplate.update("INSERT INTO event_inbox "
                        + "(tenant_id, consumer_name, event_id, event_type, status, processing_owner, "
                        + "lease_until, claimed_at, retry_count, received_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                TENANT_ID, consumer, eventId, "mysql.matrix.v1", INBOX_PROCESSING, owner,
                Timestamp.valueOf(leaseUntil), Timestamp.valueOf(LocalDateTime.now().withNano(0)), 0,
                Timestamp.valueOf(receivedAt));
    }

    private void cleanup() {
        jdbcTemplate.update("DELETE FROM event_inbox WHERE tenant_id = ? AND event_id LIKE 'mysql-matrix-inbox-%'",
                TENANT_ID);
        jdbcTemplate.update("DELETE FROM event_outbox WHERE tenant_id = ? AND event_id LIKE 'mysql-matrix-outbox-%'",
                TENANT_ID);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(20, TimeUnit.SECONDS)) {
                throw new IllegalStateException("等待并发测试 latch 超时");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("并发测试线程被中断", ex);
        }
    }
}