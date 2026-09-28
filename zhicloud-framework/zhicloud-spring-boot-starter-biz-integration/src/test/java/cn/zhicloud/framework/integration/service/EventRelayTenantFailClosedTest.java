package cn.zhicloud.framework.integration.service;

import cn.zhicloud.framework.integration.api.ConsumeResult;
import cn.zhicloud.framework.integration.api.EventConsumer;
import cn.zhicloud.framework.integration.api.EventHandler;
import cn.zhicloud.framework.integration.config.IntegrationProperties;
import cn.zhicloud.framework.integration.dal.dataobject.EventInboxDO;
import cn.zhicloud.framework.integration.dal.dataobject.EventOutboxDO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 中继租户边界 fail-closed 回归：缺少事件租户时不得按租户 0 投递。
 */
public class EventRelayTenantFailClosedTest {

    @Test
    public void testEventWithoutTenantIsNotClaimedConsumedOrPublished() {
        StubOutboxService outbox = new StubOutboxService();
        StubInboxService inbox = new StubInboxService();
        AtomicInteger consumed = new AtomicInteger();
        EventRelayService relay = newRelay(outbox, inbox);
        relay.setConsumers(List.of(new EventConsumer() {
            @Override
            public String name() {
                return "null-tenant-recorder";
            }

            @Override
            public boolean supports(String eventType) {
                return true;
            }

            @Override
            public void handle(EventOutboxDO event) {
                consumed.incrementAndGet();
            }
        }));

        assertEquals(0, relay.relayOnce(10));
        assertEquals(0, outbox.claimCalls.get(), "空租户事件不得被领取");
        assertEquals(0, inbox.consumeCalls.get(), "空租户事件不得进入任何消费者的收件箱");
        assertEquals(0, consumed.get(), "空租户事件不得执行消费者");
        assertEquals(0, outbox.markPublishedCalls.get(), "空租户事件不得被标记为已投递");
    }

    private EventRelayService newRelay(EventOutboxService outbox, EventInboxService inbox) {
        EventRelayService relay = new EventRelayService();
        setField(relay, "eventOutboxService", outbox);
        setField(relay, "eventInboxService", inbox);
        setField(relay, "properties", new IntegrationProperties());
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

    private static class StubOutboxService implements EventOutboxService {

        private final AtomicInteger claimCalls = new AtomicInteger();
        private final AtomicInteger markPublishedCalls = new AtomicInteger();

        @Override
        public Long append(cn.zhicloud.framework.integration.api.EventAppendReq req) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<EventOutboxDO> selectClaimable(LocalDateTime now, int limit) {
            return List.of(EventOutboxDO.builder()
                    .id(1L)
                    .eventId("evt-null-tenant")
                    .aggregateType("erp_sale_out")
                    .aggregateId("1001")
                    .eventType("erp.sale-out.approved.v1")
                    .payload("{}")
                    .status("NEW")
                    .retryCount(0)
                    .occurredAt(LocalDateTime.now())
                    .build());
        }

        @Override
        public boolean claim(Long id, LocalDateTime now) {
            claimCalls.incrementAndGet();
            return true;
        }

        @Override
        public boolean markPublished(Long id) {
            markPublishedCalls.incrementAndGet();
            return true;
        }

        @Override
        public void markRetryOrDead(Long id, Throwable cause) {
        }

        @Override
        public long countBacklog() {
            return 0;
        }

        @Override
        public long countDead() {
            return 0;
        }

        @Override
        public long oldestPendingSeconds() {
            return 0;
        }
    }

    private static class StubInboxService implements EventInboxService {

        private final AtomicInteger consumeCalls = new AtomicInteger();

        @Override
        public ConsumeResult consume(String consumerName, String eventId, String eventType, Long tenantId,
                                     EventHandler handler) {
            consumeCalls.incrementAndGet();
            return ConsumeResult.PROCESSED;
        }

        @Override
        public EventInboxDO selectByConsumerAndEvent(String consumerName, String eventId, Long tenantId) {
            return null;
        }

        @Override
        public boolean replay(String consumerName, String eventId, Long tenantId) {
            return false;
        }

        @Override
        public long countFailed() {
            return 0;
        }

        @Override
        public long countDead() {
            return 0;
        }

        @Override
        public long oldestPendingSeconds() {
            return 0;
        }
    }
}
