package cn.zhicloud.framework.integration.service;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil;
import cn.zhicloud.framework.common.pojo.PageParam;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.integration.dal.dataobject.EventDlqReplayLogDO;
import cn.zhicloud.framework.integration.dal.dataobject.EventInboxDO;
import cn.zhicloud.framework.integration.dal.dataobject.EventOutboxDO;
import cn.zhicloud.framework.integration.dal.mysql.EventDlqReplayLogMapper;
import cn.zhicloud.framework.integration.dal.mysql.EventInboxMapper;
import cn.zhicloud.framework.integration.dal.mysql.EventOutboxMapper;
import cn.zhicloud.framework.integration.enums.ErrorCodeConstants;
import cn.zhicloud.framework.integration.util.EventTextUtils;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static cn.zhicloud.framework.integration.enums.EventStatusConstants.INBOX_DEAD;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.OUTBOX_DEAD;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.OUTBOX_NEW;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.OUTBOX_PUBLISHED;

/**
 * 集成死信查询与协调重放实现（V105）
 *
 * @author 智云集成治理
 */
@Slf4j
public class EventDlqServiceImpl implements EventDlqService {

    @Resource
    private EventOutboxMapper eventOutboxMapper;

    @Resource
    private EventInboxMapper eventInboxMapper;

    @Resource
    private EventDlqReplayLogMapper eventDlqReplayLogMapper;

    @Resource
    private PlatformTransactionManager transactionManager;

    @Override
    public PageResult<EventOutboxDO> getDeadOutboxPage(PageParam pageParam, String eventId,
                                                        String aggregateType, String eventType,
                                                        LocalDateTime fromTime, LocalDateTime toTime) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        PageParam page = pageParam == null ? new PageParam() : pageParam;
        return eventOutboxMapper.selectPage(page, new LambdaQueryWrapperX<EventOutboxDO>()
                .eq(EventOutboxDO::getTenantId, tenantId)
                .eq(EventOutboxDO::getStatus, OUTBOX_DEAD)
                .eqIfPresent(EventOutboxDO::getEventId, eventId)
                .eqIfPresent(EventOutboxDO::getAggregateType, aggregateType)
                .likeIfPresent(EventOutboxDO::getEventType, eventType)
                .betweenIfPresent(EventOutboxDO::getOccurredAt, fromTime, toTime)
                .orderByDesc(EventOutboxDO::getId));
    }

    @Override
    public PageResult<EventInboxDO> getDeadInboxPage(PageParam pageParam, String eventId,
                                                      String consumerName, String eventType,
                                                      LocalDateTime fromTime, LocalDateTime toTime) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        PageParam page = pageParam == null ? new PageParam() : pageParam;
        return eventInboxMapper.selectPage(page, new LambdaQueryWrapperX<EventInboxDO>()
                .eq(EventInboxDO::getTenantId, tenantId)
                .eq(EventInboxDO::getStatus, INBOX_DEAD)
                .eqIfPresent(EventInboxDO::getEventId, eventId)
                .eqIfPresent(EventInboxDO::getConsumerName, consumerName)
                .likeIfPresent(EventInboxDO::getEventType, eventType)
                .betweenIfPresent(EventInboxDO::getReceivedAt, fromTime, toTime)
                .orderByDesc(EventInboxDO::getId));
    }

    @Override
    public PageResult<EventDlqReplayLogDO> getReplayLogPage(PageParam pageParam, String eventId,
                                                             Long operatorId, String result,
                                                             LocalDateTime fromTime, LocalDateTime toTime) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        PageParam page = pageParam == null ? new PageParam() : pageParam;
        return eventDlqReplayLogMapper.selectPage(page, new LambdaQueryWrapperX<EventDlqReplayLogDO>()
                .eq(EventDlqReplayLogDO::getTenantId, tenantId)
                .eqIfPresent(EventDlqReplayLogDO::getEventId, eventId)
                .eqIfPresent(EventDlqReplayLogDO::getOperatorId, operatorId)
                .eqIfPresent(EventDlqReplayLogDO::getResult, result)
                .betweenIfPresent(EventDlqReplayLogDO::getCreateTime, fromTime, toTime)
                .orderByDesc(EventDlqReplayLogDO::getId));
    }
    @Override
    public EventDlqReplayResult replay(EventDlqReplayCommand command) {
        validateReplayCommand(command);
        if (command.getEventIds().size() != 1) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EVENT_DLQ_ARG_INVALID);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        return replayOne(command, command.getEventIds().get(0), tenantId);
    }

    @Override
    public List<EventDlqReplayResult> replayBatch(EventDlqReplayCommand command) {
        validateReplayCommand(command);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<String> uniqueEventIds = command.getEventIds().stream().distinct().toList();
        List<EventDlqReplayResult> results = new ArrayList<>(uniqueEventIds.size());
        for (String eventId : uniqueEventIds) {
            try {
                results.add(replayOne(command, eventId, tenantId));
            } catch (ServiceException ex) {
                results.add(EventDlqReplayResult.failure(eventId, ex.getMessage()));
            } catch (RuntimeException ex) {
                log.error("[replayBatch][单条重放失败 eventId={}]", eventId, ex);
                results.add(EventDlqReplayResult.failure(eventId, "重放失败，请查看服务日志"));
            }
        }
        return results;
    }

    /**
     * 单条事件在一个独立事务内完成状态协调与成功审计。
     * 失败后由外层 catch 使用 REQUIRES_NEW 事务补记失败审计，避免主事务回滚抹掉证据。
     */
    private EventDlqReplayResult replayOne(EventDlqReplayCommand command, String eventId, Long tenantId) {
        try {
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            EventDlqReplayResult result = transactionTemplate.execute(
                    status -> replayInTransaction(command, eventId, tenantId));
            if (result == null) {
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.EVENT_DLQ_CONCURRENT_MODIFIED);
            }
            return result;
        } catch (ServiceException ex) {
            auditFailureIfNeeded(command, eventId, tenantId, ex);
            throw ex;
        } catch (RuntimeException ex) {
            auditFailureIfNeeded(command, eventId, tenantId, ex);
            throw ex;
        }
    }

    /**
     * 必须由事务模板调用。批量接口为每条事件创建一个 TransactionTemplate，保证单条失败只回滚该条。
     */
    private EventDlqReplayResult replayInTransaction(EventDlqReplayCommand command,
                                                     String eventId, Long tenantId) {
        EventOutboxDO outbox = eventOutboxMapper.selectByEventIdAndTenantId(eventId, tenantId);
        if (outbox == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EVENT_DLQ_NOT_REPLAYABLE);
        }
        if (!withinWindow(outbox.getOccurredAt(), command.getFromTime(), command.getToTime())) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EVENT_DLQ_OUTSIDE_WINDOW);
        }

        List<EventInboxDO> deadInboxes = eventInboxMapper.selectDeadByEventIdAndTenantId(eventId, tenantId);
        boolean hasDeadInbox = !deadInboxes.isEmpty();
        String status = outbox.getStatus();

        boolean requeueOutbox;
        if (OUTBOX_DEAD.equals(status)) {
            requeueOutbox = true;
        } else if (OUTBOX_PUBLISHED.equals(status)) {
            // PUBLISHED 本身不是死信；只有消费侧确实存在 DEAD，才允许重新投递，
            // 否则人工操作会把已成功事件无意义地再跑一遍。
            if (!hasDeadInbox) {
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.EVENT_DLQ_NOT_REPLAYABLE);
            }
            requeueOutbox = true;
        } else if (OUTBOX_NEW.equals(status)) {
            // Outbox 已经待投递，只需恢复消费侧死信；不要重复改状态制造并发歧义。
            if (!hasDeadInbox) {
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.EVENT_DLQ_NOT_REPLAYABLE);
            }
            requeueOutbox = false;
        } else {
            // PUBLISHING 正在被中继线程处理；人工改状态会与中继的 CAS 结算竞争。
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EVENT_DLQ_NOT_REPLAYABLE);
        }

        if (requeueOutbox
                && eventOutboxMapper.requeueForReplay(eventId, tenantId) != 1) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EVENT_DLQ_CONCURRENT_MODIFIED);
        }

        int inboxReplayed = 0;
        for (EventInboxDO inbox : deadInboxes) {
            inboxReplayed += eventInboxMapper.replayByTenantId(inbox.getId(), tenantId);
        }
        EventDlqReplayResult result = EventDlqReplayResult.builder()
                .eventId(eventId)
                .success(true)
                .outboxReplayed(requeueOutbox)
                .inboxReplayed(inboxReplayed)
                .build();
        eventDlqReplayLogMapper.insert(buildReplayLog(command, eventId, tenantId, result, null));
        return result;
    }

    private void auditFailureIfNeeded(EventDlqReplayCommand command, String eventId,
                                      Long tenantId, RuntimeException failure) {
        EventDlqReplayResult result = EventDlqReplayResult.failure(eventId, failure.getMessage());
        try {
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transactionTemplate.executeWithoutResult(status -> eventDlqReplayLogMapper.insert(
                    buildReplayLog(command, eventId, tenantId, result, failure.getMessage())));
        } catch (RuntimeException auditException) {
            log.error("[replay][失败审计写入异常 eventId={} tenantId={}]", eventId, tenantId, auditException);
        }
    }


    private EventDlqReplayLogDO buildReplayLog(EventDlqReplayCommand command, String eventId,
                                               Long tenantId, EventDlqReplayResult result,
                                               String errorMessage) {
        return EventDlqReplayLogDO.builder()
                .tenantId(tenantId)
                .eventId(eventId)
                .operatorId(command.getOperatorId())
                .operatorName(command.getOperatorName())
                .reason(command.getReason())
                .result(result.isSuccess()
                        ? EventDlqReplayResult.RESULT_SUCCESS : EventDlqReplayResult.RESULT_FAILED)
                .outboxReplayed(result.isOutboxReplayed())
                .inboxReplayed(result.getInboxReplayed())
                .errorMessage(errorMessage)
                .build();
    }

    private boolean withinWindow(LocalDateTime occurredAt, LocalDateTime fromTime, LocalDateTime toTime) {
        return occurredAt != null
                && !occurredAt.isBefore(fromTime)
                && !occurredAt.isAfter(toTime);
    }

    private void validateReplayCommand(EventDlqReplayCommand command) {
        if (command == null || command.getEventIds() == null || command.getEventIds().isEmpty()
                || command.getEventIds().stream().anyMatch(EventTextUtils::isBlank)) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EVENT_DLQ_ARG_INVALID);
        }
        if (command.getEventIds().size() > MAX_BATCH_SIZE) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EVENT_DLQ_BATCH_LIMIT_EXCEEDED);
        }
        if (command.getOperatorId() == null || EventTextUtils.isBlank(command.getOperatorName())
                || EventTextUtils.isBlank(command.getReason())
                || command.getFromTime() == null || command.getToTime() == null
                || command.getFromTime().isAfter(command.getToTime())) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EVENT_DLQ_AUDIT_ARG_INVALID);
        }
    }

}

