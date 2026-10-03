package cn.zhicloud.framework.integration.service;

import cn.zhicloud.framework.integration.api.ConsumeResult;
import cn.zhicloud.framework.integration.api.EventAppendReq;
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
 * Relay 不得把“其他线程仍在处理中”误判为投递完成。
 *
 * <p>这是 Inbox 引入 PROCESSING 租约后的关联回归：IN_PROGRESS 既不能确认 Outbox，
 * 也不能计入失败重试；只有 DUPLICATE 才表示此前已成功处理，可以确认 Outbox。
 */
public class EventRelayInProgressTest {

    @Test
    public void testInProgressDoesNotPublishOrRetry() {
        StubOutboxService outbox = new StubOutboxService();
        StubInboxService inbox = new StubInboxService(ConsumeResult.IN_PROGRESS);
        EventRelayService relay = newRelay(outbox, inbox);
        relay.setConsumers(List.of(new CountingConsumer()));

        assertEquals(0, relay.relayOnce(10));
        assertEquals(1, inbox.consumeCalls.get(), "必须调用收件箱进行抢占判定");
        assertEquals(0, outbox.markPublishedCalls.get(), "处理中不得确认 Outbox");
        assertEquals(0, outbox.markRetryOrDeadCalls.get(), "并发争用不得计入失败重试");
    }

    @Test
    public void testDuplicateConfirmsPreviouslyCompletedEvent() {
        StubOutboxService outbox = new StubOutboxService();
        StubInboxService inbox = new StubInboxService(ConsumeResult.DUPLICATE);
        EventRelayService relay = newRelay(outbox, inbox);
        relay.setConsumers(List.of(new CountingConsumer()));

        assertEquals(1, relay.relayOnce(10));
        assertEquals(1, inbox.consumeCalls.get());
        assertEquals(1, outbox.markPublishedCalls.get(), "此前已成功处理的事件可以确认 Outbox");
        assertEquals(0, outbox.markRetryOrDeadCalls.get());
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

    private static class CountingConsumer implements EventConsumer {

        @Override
        public String name() {
            return "relay-in-progress-test";
        }

        @Override
        public boolean supports(String eventType) {
            return true;
        }

        @Override
        public void handle(EventOutboxDO event) {
            throw new AssertionError("stub inbox 不应直接执行 handler");
        }
    }

    private static class StubOutboxService implements EventOutboxService {

        private final AtomicInteger markPublishedCalls = new AtomicInteger();
        private final AtomicInteger markRetryOrDeadCalls = new AtomicInteger();

        @Override
        public Long append(EventAppendReq req) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<EventOutboxDO> selectClaimable(LocalDateTime now, int limit) {
            return List.of(EventOutboxDO.builder()
                    .id(1L)
                    .tenantId(1L)
                    .eventId("relay-in-progress-event")
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
        public String claim(Long id, LocalDateTime now) {
            return "relay-in-progress-owner";
        }

        @Override
        public boolean markPublished(Long id, String owner) {
            markPublishedCalls.incrementAndGet();
            return true;
        }

        @Override
        public void markRetryOrDead(Long id, String owner, Throwable cause) {
            markRetryOrDeadCalls.incrementAndGet();
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

        private final ConsumeResult result;
        private final AtomicInteger consumeCalls = new AtomicInteger();

        private StubInboxService(ConsumeResult result) {
            this.result = result;
        }

        @Override
        public ConsumeResult consume(String consumerName, String eventId, String eventType, Long tenantId,
                                     EventHandler handler) {
            consumeCalls.incrementAndGet();
            return result;
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