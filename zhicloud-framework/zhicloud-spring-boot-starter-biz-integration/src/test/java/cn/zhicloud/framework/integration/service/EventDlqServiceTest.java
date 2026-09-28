package cn.zhicloud.framework.integration.service;

import cn.zhicloud.framework.common.pojo.PageParam;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.integration.config.IntegrationAutoConfiguration;
import cn.zhicloud.framework.integration.dal.dataobject.EventDlqReplayLogDO;
import cn.zhicloud.framework.integration.dal.dataobject.EventInboxDO;
import cn.zhicloud.framework.integration.dal.dataobject.EventOutboxDO;
import cn.zhicloud.framework.integration.dal.mysql.EventDlqReplayLogMapper;
import cn.zhicloud.framework.integration.dal.mysql.EventInboxMapper;
import cn.zhicloud.framework.integration.dal.mysql.EventOutboxMapper;
import cn.zhicloud.framework.integration.enums.ErrorCodeConstants;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static cn.zhicloud.framework.integration.enums.EventStatusConstants.*;
import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成死信运营、审计与租户隔离测试（V105）
 *
 * <p>人工重放不是把 Inbox 从 DEAD 改回 FAILED 就结束：Outbox 若不重新排队，事件不会再次投递；
 * 只重放 Outbox 又会被 Inbox 的 DEAD 状态挡住。重放还必须留下可追责的操作者、原因、时间窗和结果审计。
 */
@Import({IntegrationAutoConfiguration.class})
public class EventDlqServiceTest extends BaseDbUnitTest {

    private static final String EVENT_TYPE = "erp.sale-out.approved.v1";
    private static final Long TENANT_ID = 0L;
    private static final Long OPERATOR_ID = 1001L;
    private static final String OPERATOR_NAME = "integration-admin";
    private static final String REPLAY_REASON = "consumer fixed in change-20260926";

    @Resource
    private EventDlqService eventDlqService;

    @Resource
    private EventOutboxMapper eventOutboxMapper;

    @Resource
    private EventInboxMapper eventInboxMapper;

    @Resource
    private EventDlqReplayLogMapper eventDlqReplayLogMapper;

    @BeforeEach
    public void setUpTenant() {
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    public void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    public void testReplayCoordinated_outboxNewAndInboxFailed() {
        insertOutbox(TENANT_ID, "evt-coordinated-1", OUTBOX_DEAD);
        insertInbox(TENANT_ID, "consumer-a", "evt-coordinated-1", INBOX_DEAD);

        EventDlqReplayResult result = eventDlqService.replay(command("evt-coordinated-1"));

        assertTrue(result.isSuccess());
        assertTrue(result.isOutboxReplayed());
        assertEquals(1, result.getInboxReplayed());
        EventOutboxDO outbox = eventOutboxMapper.selectByEventIdAndTenantId("evt-coordinated-1", TENANT_ID);
        EventInboxDO inbox = eventInboxMapper.selectByConsumerAndEvent("consumer-a", "evt-coordinated-1", TENANT_ID);
        assertEquals(OUTBOX_NEW, outbox.getStatus());
        assertEquals(0, outbox.getRetryCount());
        assertNull(outbox.getNextRetryAt());
        assertNull(outbox.getLastError());
        assertEquals(INBOX_FAILED, inbox.getStatus());
        assertEquals(0, inbox.getRetryCount());
        assertNull(inbox.getError());
    }

    @Test
    public void testReplayPublishedOutboxWithDeadInbox_requeuesBoth() {
        insertOutbox(TENANT_ID, "evt-published-1", OUTBOX_PUBLISHED);
        insertInbox(TENANT_ID, "consumer-a", "evt-published-1", INBOX_DEAD);

        EventDlqReplayResult result = eventDlqService.replay(command("evt-published-1"));

        assertTrue(result.isOutboxReplayed());
        assertEquals(1, result.getInboxReplayed());
        EventOutboxDO outbox = eventOutboxMapper.selectByEventIdAndTenantId("evt-published-1", TENANT_ID);
        assertEquals(OUTBOX_NEW, outbox.getStatus());
        assertNull(outbox.getPublishedAt(), "重新排队后 published_at 必须清空，避免状态与时间字段矛盾");
        assertEquals(INBOX_FAILED,
                eventInboxMapper.selectByConsumerAndEvent("consumer-a", "evt-published-1", TENANT_ID).getStatus());
    }

    @Test
    public void testReplayDeadOutboxWithoutInbox() {
        insertOutbox(TENANT_ID, "evt-no-inbox-1", OUTBOX_DEAD);

        EventDlqReplayResult result = eventDlqService.replay(command("evt-no-inbox-1"));

        assertTrue(result.isOutboxReplayed());
        assertEquals(0, result.getInboxReplayed());
        assertEquals(OUTBOX_NEW, eventOutboxMapper.selectByEventIdAndTenantId("evt-no-inbox-1", TENANT_ID).getStatus());
    }

    @Test
    public void testReplayInboxOnlyWhenOutboxAlreadyNew() {
        insertOutbox(TENANT_ID, "evt-new-1", OUTBOX_NEW);
        insertInbox(TENANT_ID, "consumer-a", "evt-new-1", INBOX_DEAD);

        EventDlqReplayResult result = eventDlqService.replay(command("evt-new-1"));

        assertFalse(result.isOutboxReplayed());
        assertEquals(1, result.getInboxReplayed());
        assertEquals(OUTBOX_NEW, eventOutboxMapper.selectByEventIdAndTenantId("evt-new-1", TENANT_ID).getStatus());
        assertEquals(INBOX_FAILED,
                eventInboxMapper.selectByConsumerAndEvent("consumer-a", "evt-new-1", TENANT_ID).getStatus());
    }

    @Test
    public void testReplayRejectsPublishedOutboxWithoutDeadInbox() {
        insertOutbox(TENANT_ID, "evt-published-2", OUTBOX_PUBLISHED);
        insertInbox(TENANT_ID, "consumer-a", "evt-published-2", INBOX_PROCESSED);

        assertServiceException(() -> eventDlqService.replay(command("evt-published-2")),
                ErrorCodeConstants.EVENT_DLQ_NOT_REPLAYABLE);
        assertEquals(OUTBOX_PUBLISHED, eventOutboxMapper.selectByEventIdAndTenantId("evt-published-2", TENANT_ID).getStatus());
    }

    @Test
    public void testReplayRejectsPublishingOutboxWithDeadInbox() {
        insertOutbox(TENANT_ID, "evt-publishing-1", OUTBOX_PUBLISHING);
        insertInbox(TENANT_ID, "consumer-a", "evt-publishing-1", INBOX_DEAD);

        assertServiceException(() -> eventDlqService.replay(command("evt-publishing-1")),
                ErrorCodeConstants.EVENT_DLQ_NOT_REPLAYABLE);
        assertEquals(INBOX_DEAD,
                eventInboxMapper.selectByConsumerAndEvent("consumer-a", "evt-publishing-1", TENANT_ID).getStatus());
    }

    @Test
    public void testReplayRejectsNonDeadRecords() {
        insertOutbox(TENANT_ID, "evt-new-2", OUTBOX_NEW);
        insertInbox(TENANT_ID, "consumer-a", "evt-new-2", INBOX_PROCESSED);

        assertServiceException(() -> eventDlqService.replay(command("evt-new-2")),
                ErrorCodeConstants.EVENT_DLQ_NOT_REPLAYABLE);
    }

    @Test
    public void testReplayRejectsMissingOperator() {
        insertOutbox(TENANT_ID, "evt-no-operator", OUTBOX_DEAD);

        EventDlqReplayCommand command = command("evt-no-operator");
        command.setOperatorId(null);

        assertServiceException(() -> eventDlqService.replay(command),
                ErrorCodeConstants.EVENT_DLQ_AUDIT_ARG_INVALID);
    }

    @Test
    public void testReplayRejectsMissingReason() {
        insertOutbox(TENANT_ID, "evt-no-reason", OUTBOX_DEAD);

        EventDlqReplayCommand command = command("evt-no-reason");
        command.setReason("  ");

        assertServiceException(() -> eventDlqService.replay(command),
                ErrorCodeConstants.EVENT_DLQ_AUDIT_ARG_INVALID);
    }

    @Test
    public void testReplayRejectsInvalidWindow() {
        insertOutbox(TENANT_ID, "evt-invalid-window", OUTBOX_DEAD);

        EventDlqReplayCommand command = command("evt-invalid-window");
        command.setFromTime(command.getToTime().plusSeconds(1));

        assertServiceException(() -> eventDlqService.replay(command),
                ErrorCodeConstants.EVENT_DLQ_AUDIT_ARG_INVALID);
    }

    @Test
    public void testReplayRejectsEventOutsideWindow() {
        insertOutbox(TENANT_ID, "evt-outside-window", OUTBOX_DEAD, LocalDateTime.now().minusDays(10));
        EventDlqReplayCommand command = command("evt-outside-window");

        assertServiceException(() -> eventDlqService.replay(command),
                ErrorCodeConstants.EVENT_DLQ_OUTSIDE_WINDOW);
        assertEquals(OUTBOX_DEAD, eventOutboxMapper.selectByEventIdAndTenantId("evt-outside-window", TENANT_ID).getStatus());
    }

    @Test
    public void testReplayWritesSuccessAudit() {
        insertOutbox(TENANT_ID, "evt-audit-success", OUTBOX_DEAD);
        insertInbox(TENANT_ID, "consumer-a", "evt-audit-success", INBOX_DEAD);

        EventDlqReplayResult result = eventDlqService.replay(command("evt-audit-success"));

        assertTrue(result.isSuccess());
        List<EventDlqReplayLogDO> logs = selectReplayLogs("evt-audit-success");
        assertEquals(1, logs.size());
        EventDlqReplayLogDO log = logs.get(0);
        assertEquals(TENANT_ID, log.getTenantId());
        assertEquals(OPERATOR_ID, log.getOperatorId());
        assertEquals(OPERATOR_NAME, log.getOperatorName());
        assertEquals(REPLAY_REASON, log.getReason());
        assertEquals(EventDlqReplayResult.RESULT_SUCCESS, log.getResult());
        assertTrue(log.getOutboxReplayed());
        assertEquals(1, log.getInboxReplayed());
        assertNull(log.getErrorMessage());
    }

    @Test
    public void testReplayWritesFailureAudit() {
        insertOutbox(TENANT_ID, "evt-audit-failure", OUTBOX_PUBLISHED);

        assertServiceException(() -> eventDlqService.replay(command("evt-audit-failure")),
                ErrorCodeConstants.EVENT_DLQ_NOT_REPLAYABLE);

        List<EventDlqReplayLogDO> logs = selectReplayLogs("evt-audit-failure");
        assertEquals(1, logs.size());
        EventDlqReplayLogDO log = logs.get(0);
        assertEquals(EventDlqReplayResult.RESULT_FAILED, log.getResult());
        assertFalse(log.getOutboxReplayed());
        assertEquals(0, log.getInboxReplayed());
        assertNotNull(log.getErrorMessage());
    }

    @Test
    public void testReplayIsTenantScoped() {
        insertOutbox(2L, "evt-tenant-b", OUTBOX_DEAD);
        insertInbox(2L, "consumer-a", "evt-tenant-b", INBOX_DEAD);
        TenantContextHolder.setTenantId(1L);

        assertServiceException(() -> eventDlqService.replay(command("evt-tenant-b")),
                ErrorCodeConstants.EVENT_DLQ_NOT_REPLAYABLE);

        assertEquals(OUTBOX_DEAD, eventOutboxMapper.selectByEventIdAndTenantId("evt-tenant-b", 2L).getStatus());
        assertEquals(INBOX_DEAD,
                eventInboxMapper.selectByConsumerAndEvent("consumer-a", "evt-tenant-b", 2L).getStatus());
        List<EventDlqReplayLogDO> tenantAudit = selectReplayLogs("evt-tenant-b");
        assertEquals(1, tenantAudit.size(), "越权重放必须在当前租户留下失败审计");
        assertEquals(1L, tenantAudit.get(0).getTenantId(), "失败审计必须归属当前租户，不得写入目标租户");
        assertEquals(EventDlqReplayResult.RESULT_FAILED, tenantAudit.get(0).getResult());
    }

    @Test
    public void testRequeueForReplayCasAllowsOnlyOneWinner() {
        insertOutbox(TENANT_ID, "evt-replay-cas", OUTBOX_DEAD);

        assertEquals(1, eventOutboxMapper.requeueForReplay("evt-replay-cas", TENANT_ID),
                "首次重放应通过状态 CAS");
        assertEquals(0, eventOutboxMapper.requeueForReplay("evt-replay-cas", TENANT_ID),
                "重复或并发重放不得再次改写状态");
        assertEquals(OUTBOX_NEW, eventOutboxMapper.selectByEventIdAndTenantId("evt-replay-cas", TENANT_ID).getStatus());
    }

    @Test
    public void testDeadPageIsTenantScoped() {
        insertOutbox(1L, "evt-page-tenant-a", OUTBOX_DEAD);
        insertOutbox(2L, "evt-page-tenant-b", OUTBOX_DEAD);
        TenantContextHolder.setTenantId(1L);

        PageParam pageParam = new PageParam();
        pageParam.setPageNo(1);
        pageParam.setPageSize(10);
        PageResult<EventOutboxDO> page = eventDlqService.getDeadOutboxPage(
                pageParam, null, null, null, null, null);

        assertEquals(1L, page.getTotal());
        assertEquals("evt-page-tenant-a", page.getList().get(0).getEventId());
    }

    @Test
    public void testBatchRejectsOverLimit() {
        List<String> eventIds = new ArrayList<>();
        for (int i = 0; i < EventDlqService.MAX_BATCH_SIZE + 1; i++) {
            eventIds.add("evt-batch-" + i);
        }
        EventDlqReplayCommand command = command(eventIds.toArray(String[]::new));

        assertServiceException(() -> eventDlqService.replayBatch(command),
                ErrorCodeConstants.EVENT_DLQ_BATCH_LIMIT_EXCEEDED);
    }

    @Test
    public void testBatchContinuesAfterSingleFailureAndAuditsEachEvent() {
        insertOutbox(TENANT_ID, "evt-batch-ok", OUTBOX_DEAD);

        List<EventDlqReplayResult> results = eventDlqService.replayBatch(
                command("evt-batch-ok", "evt-batch-missing"));

        assertEquals(2, results.size());
        assertTrue(results.get(0).isSuccess());
        assertFalse(results.get(1).isSuccess());
        assertEquals("evt-batch-missing", results.get(1).getEventId());
        assertEquals(OUTBOX_NEW, eventOutboxMapper.selectByEventIdAndTenantId("evt-batch-ok", TENANT_ID).getStatus());
        assertEquals(1, selectReplayLogs("evt-batch-ok").size());
        assertEquals(1, selectReplayLogs("evt-batch-missing").size());
    }

    @Test
    public void testPageDeadOutboxFiltersByStatusAndEventId() {
        insertOutbox(TENANT_ID, "evt-page-1", OUTBOX_DEAD);
        insertOutbox(TENANT_ID, "evt-page-2", OUTBOX_DEAD);
        insertOutbox(TENANT_ID, "evt-page-3", OUTBOX_NEW);

        PageParam pageParam = new PageParam();
        pageParam.setPageNo(1);
        pageParam.setPageSize(10);
        PageResult<EventOutboxDO> page = eventDlqService.getDeadOutboxPage(
                pageParam, "evt-page-1", "erp_sale_out", EVENT_TYPE, null, null);

        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getList().size());
        assertEquals("evt-page-1", page.getList().get(0).getEventId());
    }

    @Test
    public void testPageDeadInboxFiltersByStatusAndEventId() {
        insertInbox(TENANT_ID, "consumer-a", "evt-page-1", INBOX_DEAD);
        insertInbox(TENANT_ID, "consumer-b", "evt-page-1", INBOX_DEAD);
        insertInbox(TENANT_ID, "consumer-a", "evt-page-2", INBOX_DEAD);
        insertInbox(TENANT_ID, "consumer-c", "evt-page-1", INBOX_PROCESSED);

        PageParam pageParam = new PageParam();
        pageParam.setPageNo(1);
        pageParam.setPageSize(10);
        PageResult<EventInboxDO> page = eventDlqService.getDeadInboxPage(
                pageParam, "evt-page-1", "consumer-a", EVENT_TYPE, null, null);

        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getList().size());
        assertEquals("consumer-a", page.getList().get(0).getConsumerName());
    }

    @Test
    public void testPageReplayLogsIsTenantScopedAndFiltersByEventId() {
        insertReplayLog(1L, "evt-log-1", EventDlqReplayResult.RESULT_SUCCESS);
        insertReplayLog(1L, "evt-log-2", EventDlqReplayResult.RESULT_FAILED);
        insertReplayLog(2L, "evt-log-1", EventDlqReplayResult.RESULT_SUCCESS);
        TenantContextHolder.setTenantId(1L);

        PageParam pageParam = new PageParam();
        pageParam.setPageNo(1);
        pageParam.setPageSize(10);
        PageResult<EventDlqReplayLogDO> page = eventDlqService.getReplayLogPage(
                pageParam, "evt-log-1", null, null, null, null);

        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getList().size());
        assertEquals(1L, page.getList().get(0).getTenantId());
        assertEquals("evt-log-1", page.getList().get(0).getEventId());
    }
    private EventDlqReplayCommand command(String... eventIds) {
        return EventDlqReplayCommand.builder()
                .eventIds(List.of(eventIds))
                .operatorId(OPERATOR_ID)
                .operatorName(OPERATOR_NAME)
                .reason(REPLAY_REASON)
                .fromTime(LocalDateTime.now().minusDays(1))
                .toTime(LocalDateTime.now().plusDays(1))
                .build();
    }

    private void insertReplayLog(Long tenantId, String eventId, String result) {
        EventDlqReplayLogDO row = EventDlqReplayLogDO.builder()
                .tenantId(tenantId)
                .eventId(eventId)
                .operatorId(OPERATOR_ID)
                .operatorName(OPERATOR_NAME)
                .reason(REPLAY_REASON)
                .result(result)
                .outboxReplayed(EventDlqReplayResult.RESULT_SUCCESS.equals(result))
                .inboxReplayed(0)
                .build();
        eventDlqReplayLogMapper.insert(row);
    }
    private List<EventDlqReplayLogDO> selectReplayLogs(String eventId) {
        return eventDlqReplayLogMapper.selectList(new LambdaQueryWrapper<EventDlqReplayLogDO>()
                .eq(EventDlqReplayLogDO::getEventId, eventId)
                .orderByAsc(EventDlqReplayLogDO::getId));
    }

    private void insertOutbox(Long tenantId, String eventId, String status) {
        insertOutbox(tenantId, eventId, status, LocalDateTime.now());
    }

    private void insertOutbox(Long tenantId, String eventId, String status, LocalDateTime occurredAt) {
        EventOutboxDO row = EventOutboxDO.builder()
                .eventId(eventId)
                .tenantId(tenantId)
                .aggregateType("erp_sale_out")
                .aggregateId("1001")
                .eventType(EVENT_TYPE)
                .schemaVersion("v1")
                .partitionKey(tenantId + ":1001")
                .payload("{}")
                .status(status)
                .retryCount(3)
                .nextRetryAt(LocalDateTime.now().plusMinutes(1))
                .occurredAt(occurredAt)
                .publishedAt(OUTBOX_PUBLISHED.equals(status) ? LocalDateTime.now() : null)
                .lastError("boom")
                .build();
        eventOutboxMapper.insert(row);
    }

    private void insertInbox(Long tenantId, String consumerName, String eventId, String status) {
        EventInboxDO row = EventInboxDO.builder()
                .tenantId(tenantId)
                .consumerName(consumerName)
                .eventId(eventId)
                .eventType(EVENT_TYPE)
                .status(status)
                .retryCount(3)
                .receivedAt(LocalDateTime.now())
                .processedAt(INBOX_PROCESSED.equals(status) ? LocalDateTime.now() : null)
                .error("boom")
                .build();
        eventInboxMapper.insert(row);
    }

}
