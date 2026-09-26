package cn.zhicloud.framework.integration.service;

import cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil;
import cn.zhicloud.framework.integration.api.ConsumeResult;
import cn.zhicloud.framework.integration.api.EventHandler;
import cn.zhicloud.framework.integration.config.IntegrationProperties;
import cn.zhicloud.framework.integration.dal.dataobject.EventInboxDO;
import cn.zhicloud.framework.integration.dal.mysql.EventInboxMapper;
import cn.zhicloud.framework.integration.enums.ErrorCodeConstants;
import cn.zhicloud.framework.integration.util.EventTextUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;

import java.time.Duration;
import java.time.LocalDateTime;

import static cn.zhicloud.framework.integration.enums.EventStatusConstants.INBOX_DEAD;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.INBOX_FAILED;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.INBOX_PROCESSED;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.INBOX_RECEIVED;

/**
 * 事件收件箱 Service 实现（V100）
 *
 * <p>去重判定的三条分支：
 * <ol>
 *     <li>首次投递：插入 RECEIVED 行；</li>
 *     <li>已 PROCESSED：直接返回 {@link ConsumeResult#DUPLICATE}，业务处理器不执行；</li>
 *     <li>已 DEAD：抛 {@link ErrorCodeConstants#EVENT_INBOX_DEAD}，拒绝静默跳过，
 *         否则死信会被中继当成「消费成功」而把发件箱标记完成，故障彻底消失在监控之外；</li>
 *     <li>其余状态（RECEIVED / FAILED）：CAS 抢占，抢到才执行业务。</li>
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
        EventInboxDO row = eventInboxMapper.selectByConsumerAndEvent(consumerName, eventId);
        boolean fresh = false;
        if (row == null) {
            EventInboxDO created = EventInboxDO.builder()
                    .tenantId(tenantId == null ? 0L : tenantId)
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
                fresh = true;
            } catch (DuplicateKeyException e) {
                // 并发首次投递：唯一键兜底，回查已有行按既有状态判定
                row = eventInboxMapper.selectByConsumerAndEvent(consumerName, eventId);
                if (row == null) {
                    throw e;
                }
            }
        }
        if (!fresh) {
            if (INBOX_PROCESSED.equals(row.getStatus())) {
                log.info("[consume][重复投递跳过 consumer={} eventId={}]", consumerName, eventId);
                return ConsumeResult.DUPLICATE;
            }
            if (INBOX_DEAD.equals(row.getStatus())) {
                log.error("[consume][死信事件拒绝继续消费 consumer={} eventId={}]", consumerName, eventId);
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.EVENT_INBOX_DEAD);
            }
            // RECEIVED / FAILED：抢占后再执行，避免同一事件被两个线程同时跑业务
            if (eventInboxMapper.claim(row.getId()) == 0) {
                log.info("[consume][并发抢占失败，跳过 consumer={} eventId={}]", consumerName, eventId);
                return ConsumeResult.DUPLICATE;
            }
        }
        try {
            if (handler != null) {
                handler.handle();
            }
            eventInboxMapper.markProcessed(row.getId(), LocalDateTime.now());
            return ConsumeResult.PROCESSED;
        } catch (Exception ex) {
            String error = EventTextUtils.truncate(EventTextUtils.rootMessage(ex), EventTextUtils.MAX_ERROR_LEN);
            int nextRetry = (row.getRetryCount() == null ? 0 : row.getRetryCount()) + 1;
            if (nextRetry >= maxRetry()) {
                eventInboxMapper.markDead(row.getId(), error);
                log.error("[consume][消费进入死信 consumer={} eventId={} retryCount={} error={}]",
                        consumerName, eventId, nextRetry, error);
            } else {
                eventInboxMapper.markFailed(row.getId(), error);
                log.warn("[consume][消费失败待重试 consumer={} eventId={} retryCount={} error={}]",
                        consumerName, eventId, nextRetry, error);
            }
            throw ex;
        }
    }

    @Override
    public EventInboxDO selectByConsumerAndEvent(String consumerName, String eventId) {
        return eventInboxMapper.selectByConsumerAndEvent(consumerName, eventId);
    }

    @Override
    public boolean replay(String consumerName, String eventId) {
        EventInboxDO row = eventInboxMapper.selectByConsumerAndEvent(consumerName, eventId);
        if (row == null || !INBOX_DEAD.equals(row.getStatus())) {
            return false;
        }
        return eventInboxMapper.replay(row.getId()) == 1;
    }

    @Override
    public long countFailed() {
        Long count = eventInboxMapper.selectCount(new LambdaQueryWrapper<EventInboxDO>()
                .eq(EventInboxDO::getStatus, INBOX_FAILED));
        return count == null ? 0L : count;
    }

    @Override
    public long countDead() {
        Long count = eventInboxMapper.selectCount(new LambdaQueryWrapper<EventInboxDO>()
                .eq(EventInboxDO::getStatus, INBOX_DEAD));
        return count == null ? 0L : count;
    }

    @Override
    public long oldestPendingSeconds() {
        LocalDateTime oldest = eventInboxMapper.selectOldestPendingReceivedAt();
        if (oldest == null) {
            return 0L;
        }
        // 数据库与应用时钟可能存在轻微偏差；负数没有业务意义，统一归零避免指标抖动。
        return Math.max(0L, Duration.between(oldest, LocalDateTime.now()).getSeconds());
    }

    private int maxRetry() {
        Integer value = properties.getMaxRetry();
        return value == null || value <= 0 ? 5 : value;
    }

}
