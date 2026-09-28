package cn.zhicloud.framework.integration.service;

import cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil;
import cn.zhicloud.framework.integration.api.EventConsumer;
import cn.zhicloud.framework.integration.config.IntegrationProperties;
import cn.zhicloud.framework.integration.enums.ErrorCodeConstants;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.framework.integration.dal.dataobject.EventOutboxDO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 单库事件中继（V100 阶段 1）
 *
 * <p>把发件箱里「已发生」的事件投递给本进程内的消费者：领取 → 分发 → 确认/重试。
 * 刻意不引入 MQ：阶段 1 的目标是先消除「业务成功但事件丢失」，跨进程的可靠投递留到阶段 2 再换传输层，
 * 届时 {@code event_outbox / event_inbox} 两张表不需要改。
 *
 * <p>关键：本类<b>不加</b> {@code @Transactional}。领取与确认必须各自独立提交——
 * 如果包在一个事务里，业务处理器抛异常会把「已被领取」这个事实一起回滚，
 * 事件退回 NEW 并被立刻重新领取，形成无退避的热循环。
 *
 * @author 智云集成治理
 */
@Slf4j
public class EventRelayService {

    @Resource
    private EventOutboxService eventOutboxService;

    @Resource
    private EventInboxService eventInboxService;

    @Resource
    private IntegrationProperties properties;

    /**
     * 进程内消费者；无消费者或没有消费者支持事件类型时，事件保持可重试并最终进入死信。
     */
    private List<EventConsumer> consumers = new ArrayList<>();

    @Autowired(required = false)
    public void setConsumers(List<EventConsumer> consumers) {
        this.consumers = consumers == null ? new ArrayList<>() : consumers;
    }

    /**
     * 执行一次中继扫描
     *
     * @param limit 本次最多处理的事件数，{@code <= 0} 时取配置 {@code zhicloud.integration.batch-size}
     * @return 本轮标记投递完成的事件数
     */
    public int relayOnce(int limit) {
        int size = limit <= 0 ? (properties.getBatchSize() == null ? 50 : properties.getBatchSize()) : limit;
        LocalDateTime now = LocalDateTime.now();
        List<EventOutboxDO> candidates = eventOutboxService.selectClaimable(now, size);
        int published = 0;
        for (EventOutboxDO event : candidates) {
            if (event.getTenantId() == null) {
                log.error("[relayOnce][事件缺少租户，拒绝投递 id={} eventId={}]", event.getId(), event.getEventId());
                continue;
            }
            boolean claimed;
            try {
                claimed = executeWithTenant(event.getTenantId(),
                        () -> eventOutboxService.claim(event.getId(), now));
            } catch (Exception ex) {
                log.error("[relayOnce][事件领取异常 id={} eventId={} tenantId={}]",
                        event.getId(), event.getEventId(), event.getTenantId(), ex);
                continue;
            }
            if (!claimed) {
                // 已被其他节点/线程领走，或在上一轮扫描后被推入退避窗口
                continue;
            }
            try {
                executeWithTenant(event.getTenantId(), () -> {
                    dispatch(event);
                    return null;
                });
                boolean marked = executeWithTenant(event.getTenantId(),
                        () -> eventOutboxService.markPublished(event.getId()));
                if (marked) {
                    published++;
                }
            } catch (Exception ex) {
                try {
                    executeWithTenant(event.getTenantId(), () -> {
                        eventOutboxService.markRetryOrDead(event.getId(), ex);
                        return null;
                    });
                } catch (Exception settleEx) {
                    log.error("[relayOnce][事件失败结算异常 id={} eventId={} tenantId={}]",
                            event.getId(), event.getEventId(), event.getTenantId(), settleEx);
                }
                log.warn("[relayOnce][事件投递失败 id={} eventId={}]", event.getId(), event.getEventId(), ex);
            }
        }
        return published;
    }

    /**
     * 按事件类型分发给所有匹配消费者；无匹配消费者时 fail-closed。
     *
     * <p>消费者漏注册、事件类型写错或对应模块未部署时，绝不能把事件标记为投递成功。
     * 抛出异常后由 {@link #relayOnce(int)} 按统一策略退避重试，最终进入死信并触发告警。
     */
    private void dispatch(EventOutboxDO event) throws Exception {
        boolean matched = false;
        for (EventConsumer consumer : consumers) {
            if (consumer.supports(event.getEventType())) {
                matched = true;
                eventInboxService.consume(consumer.name(), event.getEventId(), event.getEventType(),
                        event.getTenantId(), () -> consumer.handle(event));
            }
        }
        if (!matched) {
            throw new IllegalStateException("No matching event consumer for eventId="
                    + event.getEventId() + ", eventType=" + event.getEventType());
        }
    }


    /**
     * 事件状态迁移和消费者必须在事件所属租户上下文中执行，否则租户拦截器会串租户或拒绝访问。
     * 这里手动保存/恢复上下文，保留原始受检异常，避免重试策略被包装异常干扰。
     */
    private <T> T executeWithTenant(Long tenantId, ThrowingSupplier<T> action) throws Exception {
        Long oldTenantId = TenantContextHolder.getTenantId();
        boolean oldIgnore = TenantContextHolder.isIgnore();
        if (tenantId == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.EVENT_TENANT_REQUIRED);
        }
        try {
            TenantContextHolder.setTenantId(tenantId);
            TenantContextHolder.setIgnore(false);
            return action.get();
        } finally {
            TenantContextHolder.setTenantId(oldTenantId);
            TenantContextHolder.setIgnore(oldIgnore);
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {

        T get() throws Exception;

    }
}
