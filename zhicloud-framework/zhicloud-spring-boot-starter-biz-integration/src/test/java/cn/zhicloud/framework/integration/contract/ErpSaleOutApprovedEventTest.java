package cn.zhicloud.framework.integration.contract;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * ERP 销售出库审核通过事件体测试（V101）：eventId 稳定性与 JSON 往返
 *
 * @author 智云集成治理
 */
class ErpSaleOutApprovedEventTest {

    @Test
    void eventIdIsStableAndDerivableFromBusinessKey() {
        assertEquals("erp-sale-out-1024-approved", ErpSaleOutApprovedEvent.eventIdOf(1024L));
        assertEquals(ErpSaleOutApprovedEvent.eventIdOf(1024L), ErpSaleOutApprovedEvent.eventIdOf(1024L),
                "同一单据重复触发必须得到相同 eventId，否则消费侧无法去重");
    }

    @Test
    void jsonRoundTripKeepsItems() {
        ErpSaleOutApprovedEvent event = ErpSaleOutApprovedEvent.builder()
                .no("XSCK202609260001")
                .saleOutId(1024L)
                .customerId(88L)
                .outTime(LocalDateTime.of(2026, 9, 26, 10, 30))
                .totalPrice(new BigDecimal("2260.0000"))
                .items(List.of(ErpSaleOutApprovedEvent.Item.builder()
                        .productId(1L).warehouseId(1L)
                        .count(new BigDecimal("10")).price(new BigDecimal("200.00"))
                        .build()))
                .build();

        ErpSaleOutApprovedEvent parsed = ErpSaleOutApprovedEvent.fromJson(event.toJson());

        assertNotNull(parsed);
        assertEquals("XSCK202609260001", parsed.getNo());
        assertEquals(1024L, parsed.getSaleOutId());
        assertEquals(88L, parsed.getCustomerId());
        assertEquals(new BigDecimal("2260.0000"), parsed.getTotalPrice());
        assertEquals(1, parsed.getItems().size());
        assertEquals(1L, parsed.getItems().get(0).getProductId());
        assertEquals(1L, parsed.getItems().get(0).getWarehouseId());
        assertEquals(new BigDecimal("10"), parsed.getItems().get(0).getCount());
    }

}