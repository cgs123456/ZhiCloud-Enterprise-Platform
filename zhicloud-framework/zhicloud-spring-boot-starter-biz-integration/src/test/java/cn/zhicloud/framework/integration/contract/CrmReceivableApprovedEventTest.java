package cn.zhicloud.framework.integration.contract;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * CRM 回款审批通过事件体测试（D1/§12.267）：eventId 稳定性与 JSON 往返
 *
 * @author 智云集成治理
 */
class CrmReceivableApprovedEventTest {

    @Test
    void eventIdIsStableAndDerivableFromBusinessKey() {
        assertEquals("crm-receivable-1024-approved", CrmReceivableApprovedEvent.eventIdOf(1024L));
        assertEquals(CrmReceivableApprovedEvent.eventIdOf(1024L), CrmReceivableApprovedEvent.eventIdOf(1024L),
                "同一回款重复触发必须得到相同 eventId，否则消费侧无法去重");
    }

    @Test
    void jsonRoundTripKeepsKeyFields() {
        CrmReceivableApprovedEvent event = CrmReceivableApprovedEvent.builder()
                .no("HK20261018000001")
                .receivableId(1024L)
                .customerId(88L)
                .returnTime(LocalDateTime.of(2026, 10, 18, 10, 30))
                .price(new BigDecimal("400.00"))
                .build();

        CrmReceivableApprovedEvent parsed = CrmReceivableApprovedEvent.fromJson(event.toJson());

        assertNotNull(parsed);
        assertEquals("HK20261018000001", parsed.getNo());
        assertEquals(1024L, parsed.getReceivableId());
        assertEquals(88L, parsed.getCustomerId());
        assertEquals(LocalDateTime.of(2026, 10, 18, 10, 30), parsed.getReturnTime());
        assertEquals(new BigDecimal("400.00"), parsed.getPrice());
    }

}