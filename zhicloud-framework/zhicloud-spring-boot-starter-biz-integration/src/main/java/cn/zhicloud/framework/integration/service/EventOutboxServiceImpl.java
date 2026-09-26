package cn.zhicloud.framework.integration.service;

import cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil;
import cn.zhicloud.framework.common.util.json.JsonUtils;
import cn.zhicloud.framework.integration.api.EventAppendReq;
import cn.zhicloud.framework.integration.config.IntegrationProperties;
import cn.zhicloud.framework.integration.dal.dataobject.EventOutboxDO;
import cn.zhicloud.framework.integration.dal.mysql.EventOutboxMapper;
import cn.zhicloud.framework.integration.enums.ErrorCodeConstants;
import cn.zhicloud.framework.integration.util.EventTextUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static cn.zhicloud.framework.integration.enums.EventStatusConstants.OUTBOX_DEAD;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.OUTBOX_NEW;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.OUTBOX_PUBLISHING;

/**
 * 事件发件箱 Service 实现（V100）
 *
 * <p>不标注 {@code @Service}：由 {@code IntegrationAutoConfiguration} 以 {@code @Bean} 注册。
 *
 * @author 智云集成治理
 */
@Slf4j
public class EventOutboxServiceImpl implements EventOutboxService {

    private static final int MAX_BATCH_SIZE = 1000;
    private static final int MAX_BACKOFF_SHIFT = 20;
    private static final String DEFAULT_SCHEMA_VERSION = "v1";

    @Resource
    private EventOutboxMapper eventOutboxMapper;

    @Resource
    private IntegrationProperties properties;

    @Override
    public Long append(EventAppendReq req) {
        validate(req);
        Long tenantId = req.getTenantId() == null ? 0L : req.getTenantId();
        EventOutboxDO event = EventOutboxDO.builder()
                .eventId(req.getEventId())
                .tenantId(tenantId)
                .aggregateType(req.getAggregateType())
                .aggregateId(req.getAggregateId())
                .eventType(req.getEventType())
                .schemaVersion(EventTextUtils.isBlank(req.getSchemaVersion()) ? DEFAULT_SCHEMA_VERSION : req.getSchemaVersion())
                .partitionKey(EventTextUtils.isBlank(req.getPartitionKey()) ? tenantId + ":" + req.getAggregateId() : req.getPartitionKey())
                .payload(req.getPayload())
                .status(OUTBOX_NEW)
                .retryCount(0)
                .nextRetryAt(null)
                .occurredAt(req.getOccurredAt() == null ? LocalDateTime.now() : req.getOccurredAt())
                .traceId(req.getTraceId())
                .build();
        try {
            eventOutboxMapper.insert(event);
            return event.getId();
        } catch (DuplicateKeyException e) {
            // 幂等登记：同一 eventId 视为同一事件。业务侧重复触发（用户双击、上游重推）不应产生第二行，
            // 否则消费侧会因为 eventId 不唯一而失去去重能力。
            EventOutboxDO existing = eventOutboxMapper.selectByEventId(req.getEventId());
            if (existing != null) {
                log.warn("[append][事件编号重复，按幂等登记返回已有行 id={} eventId={}]", existing.getId(), req.getEventId());
                return existing.getId();
            }
            throw e;
        }
    }

    @Override
    public List<EventOutboxDO> selectClaimable(LocalDateTime now, int limit) {
        int size = limit <= 0 ? batchSize() : Math.min(limit, MAX_BATCH_SIZE);
        List<Long> ids = eventOutboxMapper.selectClaimableIds(now == null ? LocalDateTime.now() : now, size);
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return eventOutboxMapper.selectBatchIds(ids);
    }

    @Override
    public boolean claim(Long id, LocalDateTime now) {
        if (id == null) {
            return false;
        }
        LocalDateTime base = now == null ? LocalDateTime.now() : now;
        // 租约：领取后 leaseSeconds 内不可再领取，过期后 PUBLISHING 事件重新可见（崩溃恢复）
        return eventOutboxMapper.claim(id, base, base.plusSeconds(leaseSeconds())) == 1;
    }

    @Override
    public boolean markPublished(Long id) {
        if (id == null) {
            return false;
        }
        return eventOutboxMapper.markPublished(id, LocalDateTime.now()) == 1;
    }

    @Override
    public void markRetryOrDead(Long id, Throwable cause) {
        if (id == null) {
            return;
        }
        EventOutboxDO event = eventOutboxMapper.selectById(id);
        if (event == null) {
            return;
        }
        String error = EventTextUtils.truncate(EventTextUtils.rootMessage(cause), EventTextUtils.MAX_ERROR_LEN);
        int nextRetry = (event.getRetryCount() == null ? 0 : event.getRetryCount()) + 1;
        if (nextRetry >= maxRetry()) {
            eventOutboxMapper.markDead(id, error);
            log.error("[markRetryOrDead][事件进入死信 id={} eventId={} retryCount={} error={}]",
                    id, event.getEventId(), nextRetry, error);
            return;
        }
        long backoff = backoffSeconds(nextRetry);
        eventOutboxMapper.markRetry(id, LocalDateTime.now().plusSeconds(backoff), error);
        log.warn("[markRetryOrDead][事件重试入队 id={} eventId={} retryCount={} backoffSeconds={}]",
                id, event.getEventId(), nextRetry, backoff);
    }

    @Override
    public long countBacklog() {
        Long count = eventOutboxMapper.selectCount(new LambdaQueryWrapper<EventOutboxDO>()
                .in(EventOutboxDO::getStatus, Arrays.asList(OUTBOX_NEW, OUTBOX_PUBLISHING)));
        return count == null ? 0L : count;
    }

    @Override
    public long countDead() {
        Long count = eventOutboxMapper.selectCount(new LambdaQueryWrapper<EventOutboxDO>()
                .eq(EventOutboxDO::getStatus, OUTBOX_DEAD));
        return count == null ? 0L : count;
    }

    @Override
    public long oldestPendingSeconds() {
        LocalDateTime oldest = eventOutboxMapper.selectOldestPendingOccurredAt();
        if (oldest == null) {
            return 0L;
        }
        // 数据库与应用时钟可能存在轻微偏差；负数没有业务意义，统一归零避免指标抖动。
        return Math.max(0L, Duration.between(oldest, LocalDateTime.now()).getSeconds());
    }

    @Override
    public boolean replay(Long id) {
        if (id == null) {
            return false;
        }
        return eventOutboxMapper.replay(id) == 1;
    }

    // -------------------- 内部方法 --------------------

    private void validate(EventAppendReq req) {
        if (req == null
                || EventTextUtils.isBlank(req.getEventId())
                || EventTextUtils.isBlank(req.getAggregateType())
                || EventTextUtils.isBlank(req.getAggregateId())
                || EventTextUtils.isBlank(req.getEventType())
                || EventTextUtils.isBlank(req.getPayload())) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EVENT_OUTBOX_ARG_INVALID);
        }
        // payload 列是 LONGTEXT 而非 JSON 类型，数据库层不做格式校验，故在应用层补上
        if (!JsonUtils.isJson(req.getPayload())) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EVENT_OUTBOX_PAYLOAD_INVALID);
        }
    }

    /**
     * 指数退避：第 n 次失败后等待 {@code base * 2^(n-1)} 秒，封顶 {@code maxBackoffSeconds}
     */
    private long backoffSeconds(int retryCount) {
        long base = properties.getBaseBackoffSeconds() == null ? 5L : properties.getBaseBackoffSeconds();
        long max = properties.getMaxBackoffSeconds() == null ? 300L : properties.getMaxBackoffSeconds();
        int shift = Math.min(Math.max(retryCount - 1, 0), MAX_BACKOFF_SHIFT);
        long seconds = base * (1L << shift);
        return Math.min(Math.max(seconds, 0L), max);
    }

    private int maxRetry() {
        Integer value = properties.getMaxRetry();
        return value == null || value <= 0 ? 5 : value;
    }

    private long leaseSeconds() {
        Long value = properties.getLeaseSeconds();
        return value == null || value <= 0 ? 60L : value;
    }

    private int batchSize() {
        Integer value = properties.getBatchSize();
        return value == null || value <= 0 ? 50 : value;
    }

}