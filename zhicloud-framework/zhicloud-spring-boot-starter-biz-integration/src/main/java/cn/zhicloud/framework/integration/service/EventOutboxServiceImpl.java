package cn.zhicloud.framework.integration.service;

import cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil;
import cn.zhicloud.framework.common.util.json.JsonUtils;
import cn.zhicloud.framework.integration.api.EventAppendReq;
import cn.zhicloud.framework.integration.config.IntegrationProperties;
import cn.zhicloud.framework.integration.dal.dataobject.EventOutboxDO;
import cn.zhicloud.framework.integration.dal.mysql.EventOutboxMapper;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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
        Long tenantId = resolveTenantId(req.getTenantId());
        return TenantUtils.execute(tenantId, () -> {
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
                // 幂等登记：同一租户内的同一 eventId 视为同一事件。业务侧重复触发（用户双击、上游重推）
                // 不应产生第二行；不同租户的业务编号空间相互独立，不能跨租户合并。
                EventOutboxDO existing = eventOutboxMapper.selectByEventIdAndTenantId(req.getEventId(), tenantId);
                if (existing != null) {
                    log.warn("[append][事件编号重复，按幂等登记返回已有行 id={} eventId={}]", existing.getId(), req.getEventId());
                    return existing.getId();
                }
                throw e;
            }
        });
    }

    @Override
    public List<EventOutboxDO> selectClaimable(LocalDateTime now, int limit) {
        int size = limit <= 0 ? batchSize() : Math.min(limit, MAX_BATCH_SIZE);
        return TenantUtils.executeIgnore(() -> {
            List<Long> ids = eventOutboxMapper.selectClaimableIds(now == null ? LocalDateTime.now() : now, size);
            if (ids == null || ids.isEmpty()) {
                return new ArrayList<>();
            }
            return eventOutboxMapper.selectBatchIds(ids);
        });
    }

    @Override
    public String claim(Long id, LocalDateTime now) {
        if (id == null) {
            return null;
        }
        LocalDateTime base = now == null ? LocalDateTime.now() : now;
        Long tenantId = currentTenantId();
        String owner = UUID.randomUUID().toString();
        // 租约：领取后 leaseSeconds 内不可再领取，过期后 PUBLISHING 事件重新可见（崩溃恢复）
        int claimed = eventOutboxMapper.claim(id, tenantId, owner, base,
                base.plusSeconds(leaseSeconds()));
        return claimed == 1 ? owner : null;
    }

    @Override
    public boolean markPublished(Long id, String owner) {
        if (id == null || EventTextUtils.isBlank(owner)) {
            return false;
        }
        return eventOutboxMapper.markPublished(id, currentTenantId(), owner, LocalDateTime.now()) == 1;
    }

    @Override
    public void markRetryOrDead(Long id, String owner, Throwable cause) {
        if (id == null || EventTextUtils.isBlank(owner)) {
            return;
        }
        Long tenantId = currentTenantId();
        EventOutboxDO event = eventOutboxMapper.selectByIdAndTenantId(id, tenantId);
        if (event == null) {
            return;
        }
        String error = EventTextUtils.truncate(EventTextUtils.rootMessage(cause), EventTextUtils.MAX_ERROR_LEN);
        int nextRetry = (event.getRetryCount() == null ? 0 : event.getRetryCount()) + 1;
        if (nextRetry >= maxRetry()) {
            int updated = eventOutboxMapper.markDead(id, tenantId, owner, error);
            if (updated == 1) {
                log.error("[markRetryOrDead][事件进入死信 id={} eventId={} retryCount={} owner={} error={}]",
                        id, event.getEventId(), nextRetry, owner, error);
            } else {
                log.error("[markRetryOrDead][owner fencing 拒绝死信结算 id={} eventId={} owner={}]",
                        id, event.getEventId(), owner);
            }
            return;
        }
        long backoff = backoffSeconds(nextRetry);
        int updated = eventOutboxMapper.markRetry(id, tenantId, owner,
                LocalDateTime.now().plusSeconds(backoff), error);
        if (updated == 1) {
            log.warn("[markRetryOrDead][事件重试入队 id={} eventId={} retryCount={} owner={} backoffSeconds={}]",
                    id, event.getEventId(), nextRetry, owner, backoff);
        } else {
            log.error("[markRetryOrDead][owner fencing 拒绝重试结算 id={} eventId={} owner={}]",
                    id, event.getEventId(), owner);
        }
    }

    @Override
    public long countBacklog() {
        Long count = TenantUtils.executeIgnore(() -> eventOutboxMapper.selectCount(
                new LambdaQueryWrapper<EventOutboxDO>()
                        .in(EventOutboxDO::getStatus, Arrays.asList(OUTBOX_NEW, OUTBOX_PUBLISHING))));
        return count == null ? 0L : count;
    }

    @Override
    public long countDead() {
        Long count = TenantUtils.executeIgnore(() -> eventOutboxMapper.selectCount(
                new LambdaQueryWrapper<EventOutboxDO>()
                        .eq(EventOutboxDO::getStatus, OUTBOX_DEAD)));
        return count == null ? 0L : count;
    }

    @Override
    public long oldestPendingSeconds() {
        LocalDateTime oldest = TenantUtils.executeIgnore(eventOutboxMapper::selectOldestPendingOccurredAt);
        if (oldest == null) {
            return 0L;
        }
        // 数据库与应用时钟可能存在轻微偏差；负数没有业务意义，统一归零避免指标抖动。
        return Math.max(0L, Duration.between(oldest, LocalDateTime.now()).getSeconds());
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

    /**
     * 解析事件所属租户：允许从当前上下文推导，但禁止静默回退租户 0，也禁止跨租户显式登记。
     */
    private Long resolveTenantId(Long requestedTenantId) {
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EVENT_TENANT_REQUIRED);
        }
        if (requestedTenantId != null && !requestedTenantId.equals(currentTenantId)) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EVENT_TENANT_MISMATCH);
        }
        return currentTenantId;
    }

    /**
     * 状态迁移必须作用于当前上下文所属租户，禁止无上下文时回退到租户 0。
     */
    private Long currentTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EVENT_TENANT_REQUIRED);
        }
        return tenantId;
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