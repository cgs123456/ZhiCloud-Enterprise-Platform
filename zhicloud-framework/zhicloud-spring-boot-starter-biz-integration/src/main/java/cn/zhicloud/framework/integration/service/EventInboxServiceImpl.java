package cn.zhicloud.framework.integration.service;

import cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil;
import cn.zhicloud.framework.integration.api.ConsumeResult;
import cn.zhicloud.framework.integration.api.EventHandler;
import cn.zhicloud.framework.integration.config.IntegrationProperties;
import cn.zhicloud.framework.integration.dal.dataobject.EventInboxDO;
import cn.zhicloud.framework.integration.dal.mysql.EventInboxMapper;
import cn.zhicloud.framework.integration.enums.ErrorCodeConstants;
import cn.zhicloud.framework.integration.util.EventTextUtils;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.framework.tenant.core.util.TenantUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static cn.zhicloud.framework.integration.enums.EventStatusConstants.INBOX_DEAD;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.INBOX_FAILED;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.INBOX_PROCESSED;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.INBOX_RECEIVED;

/**
 * 事件收件箱 Service 实现（V100）
 *
 * <p>消费判定和结算规则：
 * <ol>
 *     <li>首次投递：插入 RECEIVED 行，然后通过带 owner/租约的 CAS 领取；</li>
 *     <li>已 PROCESSED：直接返回 {@link ConsumeResult#DUPLICATE}，业务处理器不执行；</li>
 *     <li>已 DEAD：抛 {@link ErrorCodeConstants#EVENT_INBOX_DEAD}，拒绝静默跳过，
 *         否则死信会被中继当成「消费成功」而把发件箱标记完成，故障彻底消失在监控之外；</li>
 *     <li>RECEIVED / FAILED / 租约已过期的 PROCESSING：CAS 抢占，抢到才执行业务；</li>
 *     <li>租约未过期的 PROCESSING：返回 {@link ConsumeResult#IN_PROGRESS}，调用方不得确认 Outbox；</li>
 *     <li>handler 成功但 owner fencing 结算失败：抛异常，不返回成功，避免旧 worker 覆盖接管者。</li>
 * </ol>
 *
 * @author 智云集成治理
 */
@Slf4j
public class EventInboxServiceImpl implements EventInboxService {

    @Resource
    private EventInboxMapper eventInboxMapper;

    @Resource
    private IntegrationProperties properties;

    @Override
    public ConsumeResult consume(String consumerName, String eventId, String eventType, Long tenantId,
                                 EventHandler handler) throws Exception {
        if (EventTextUtils.isBlank(consumerName) || EventTextUtils.isBlank(eventId)) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EVENT_OUTBOX_ARG_INVALID);
        }
        Long eventTenantId = requireTenantId(tenantId);
        Long oldTenantId = TenantContextHolder.getTenantId();
        boolean oldIgnore = TenantContextHolder.isIgnore();
        try {
            // 去重键包含 tenant_id；即使调用方来自定时任务，也必须先进入事件所属租户上下文。
            TenantContextHolder.setTenantId(eventTenantId);
            TenantContextHolder.setIgnore(false);
            return doConsume(consumerName, eventId, eventType, eventTenantId, handler);
        } finally {
            TenantContextHolder.setTenantId(oldTenantId);
            TenantContextHolder.setIgnore(oldIgnore);
        }
    }

    private ConsumeResult doConsume(String consumerName, String eventId, String eventType,
                                    Long eventTenantId, EventHandler handler) throws Exception {
        EventInboxDO row = eventInboxMapper.selectByConsumerAndEvent(consumerName, eventId, eventTenantId);
        if (row == null) {
            EventInboxDO created = EventInboxDO.builder()
                    .tenantId(eventTenantId)
                    .consumerName(consumerName)
                    .eventId(eventId)
                    .eventType(eventType == null ? "" : eventType)
                    .status(INBOX_RECEIVED)
                    .retryCount(0)
                    .receivedAt(LocalDateTime.now())
                    .build();
            try {
                eventInboxMapper.insert(created);
                row = created;
            } catch (DuplicateKeyException e) {
                // 并发首次投递：唯一键兜底，回查已有行按既有状态判定
                row = eventInboxMapper.selectByConsumerAndEvent(consumerName, eventId, eventTenantId);
                if (row == null) {
                    throw e;
                }
            }
        }

        if (INBOX_PROCESSED.equals(row.getStatus())) {
            log.info("[consume][重复投递跳过 consumer={} eventId={}]", consumerName, eventId);
            return ConsumeResult.DUPLICATE;
        }
        if (INBOX_DEAD.equals(row.getStatus())) {
            log.error("[consume][死信事件拒绝继续消费 consumer={} eventId={}]", consumerName, eventId);
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EVENT_INBOX_DEAD);
        }

        // 每次领取生成独立 owner；即使两个节点同时尝试接管过期租约，也只有一个能写入该 owner。
        String owner = UUID.randomUUID().toString();
        LocalDateTime claimedAt = LocalDateTime.now();
        int claimed = eventInboxMapper.claim(row.getId(), eventTenantId, owner, claimedAt,
                claimedAt.plusSeconds(inboxLeaseSeconds()));
        if (claimed != 1) {
            return classifyClaimFailure(consumerName, eventId, eventTenantId);
        }

        try {
            if (handler != null) {
                handler.handle();
            }
        } catch (Exception ex) {
            settleFailure(row.getId(), eventTenantId, owner, row.getRetryCount(), consumerName, eventId, ex);
            throw ex;
        }

        int marked = eventInboxMapper.markProcessed(row.getId(), eventTenantId, owner, LocalDateTime.now());
        if (marked != 1) {
            // handler 可能已经产生业务副作用，但当前 owner 已失去结算权。必须 fail-closed，
            // 让调用方保留/重试 Outbox；绝不能返回 PROCESSED 并确认事件。
            throw new IllegalStateException("Inbox processing lease lost before success settlement: consumer="  // @bare-throw-ignore 刻意 fail-closed 的基础设施信号，异常类型已被现有测试断言；改成 ServiceException 会同时改变对外语义与这些测试结果，是否统一属人工裁决
                    + consumerName + ", eventId=" + eventId);
        }
        return ConsumeResult.PROCESSED;
    }

    /**
     * CAS 失败后按数据库中的最新状态判定结果。
     *
     * <p>只有明确看到 PROCESSED 才能返回 DUPLICATE；其他状态都意味着仍有并发处理或状态争用，
     * 调用方不得确认 Outbox，因此返回 IN_PROGRESS。
     */
    private ConsumeResult classifyClaimFailure(String consumerName, String eventId, Long eventTenantId) {
        EventInboxDO current = eventInboxMapper.selectByConsumerAndEvent(consumerName, eventId, eventTenantId);
        if (current != null && INBOX_PROCESSED.equals(current.getStatus())) {
            log.info("[consume][重复投递跳过 consumer={} eventId={}]", consumerName, eventId);
            return ConsumeResult.DUPLICATE;
        }
        if (current != null && INBOX_DEAD.equals(current.getStatus())) {
            log.error("[consume][死信事件拒绝继续消费 consumer={} eventId={}]", consumerName, eventId);
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EVENT_INBOX_DEAD);
        }
        log.info("[consume][事件仍在处理中或发生状态争用 consumer={} eventId={} status={}]",
                consumerName, eventId, current == null ? null : current.getStatus());
        return ConsumeResult.IN_PROGRESS;
    }

    private void settleFailure(Long id, Long eventTenantId, String owner, Integer retryCount,
                               String consumerName, String eventId, Exception cause) {
        String error = EventTextUtils.truncate(EventTextUtils.rootMessage(cause), EventTextUtils.MAX_ERROR_LEN);
        int nextRetry = (retryCount == null ? 0 : retryCount) + 1;
        int updated;
        if (nextRetry >= maxRetry()) {
            updated = eventInboxMapper.markDead(id, eventTenantId, owner, error);
            if (updated == 1) {
                log.error("[consume][消费进入死信 consumer={} eventId={} retryCount={} error={}]",
                        consumerName, eventId, nextRetry, error);
            }
        } else {
            updated = eventInboxMapper.markFailed(id, eventTenantId, owner, error);
            if (updated == 1) {
                log.warn("[consume][消费失败待重试 consumer={} eventId={} retryCount={} error={}]",
                        consumerName, eventId, nextRetry, error);
            }
        }
        if (updated != 1) {
            log.error("[consume][消费失败结算被 owner fencing 拒绝 consumer={} eventId={} owner={}]",
                    consumerName, eventId, owner);
        }
    }

    @Override
    public EventInboxDO selectByConsumerAndEvent(String consumerName, String eventId, Long tenantId) {
        Long eventTenantId = requireTenantId(tenantId);
        return TenantUtils.execute(eventTenantId,
                () -> eventInboxMapper.selectByConsumerAndEvent(consumerName, eventId, eventTenantId));
    }

    @Override
    public boolean replay(String consumerName, String eventId, Long tenantId) {
        Long eventTenantId = requireTenantId(tenantId);
        return TenantUtils.execute(eventTenantId, () -> {
            EventInboxDO row = eventInboxMapper.selectByConsumerAndEvent(consumerName, eventId, eventTenantId);
            if (row == null || !INBOX_DEAD.equals(row.getStatus())) {
                return false;
            }
            return eventInboxMapper.replayByTenantId(row.getId(), eventTenantId) == 1;
        });
    }

    @Override
    public long countFailed() {
        Long count = TenantUtils.executeIgnore(() -> eventInboxMapper.selectCount(
                new LambdaQueryWrapper<EventInboxDO>()
                        .eq(EventInboxDO::getStatus, INBOX_FAILED)));
        return count == null ? 0L : count;
    }

    @Override
    public long countDead() {
        Long count = TenantUtils.executeIgnore(() -> eventInboxMapper.selectCount(
                new LambdaQueryWrapper<EventInboxDO>()
                        .eq(EventInboxDO::getStatus, INBOX_DEAD)));
        return count == null ? 0L : count;
    }

    @Override
    public long oldestPendingSeconds() {
        LocalDateTime oldest = TenantUtils.executeIgnore(eventInboxMapper::selectOldestPendingReceivedAt);
        if (oldest == null) {
            return 0L;
        }
        // 数据库与应用时钟可能存在轻微偏差；负数没有业务意义，统一归零避免指标抖动。
        return Math.max(0L, Duration.between(oldest, LocalDateTime.now()).getSeconds());
    }

    /**
     * 解析收件箱操作租户：显式租户不得覆盖调用方已有的不同租户上下文。
     *
     * <p>中继任务可以没有 ambient tenant 上下文，因此允许显式租户创建正确上下文；
     * 但调用方一旦已有租户上下文，显式传入其他租户即视为越权并 fail-closed。
     */
    private Long requireTenantId(Long tenantId) {
        if (tenantId == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EVENT_TENANT_REQUIRED);
        }
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId != null && !currentTenantId.equals(tenantId)) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EVENT_TENANT_MISMATCH);
        }
        return tenantId;
    }

    private int maxRetry() {
        Integer value = properties.getMaxRetry();
        return value == null || value <= 0 ? 5 : value;
    }

    private long inboxLeaseSeconds() {
        Long value = properties.getInboxLeaseSeconds();
        return value == null || value <= 0 ? 60L : value;
    }

}
