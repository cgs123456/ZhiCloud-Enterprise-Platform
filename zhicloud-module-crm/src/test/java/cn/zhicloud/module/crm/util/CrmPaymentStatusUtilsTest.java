package cn.zhicloud.module.crm.util;

import cn.zhicloud.module.crm.enums.salesorder.CrmSaleOrderPaymentStatusEnum;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link CrmPaymentStatusUtils} 的单元测试：付款状态投影函数的分支口径（§12.260 D1）
 */
public class CrmPaymentStatusUtilsTest {

    @Test
    public void test_resolve_unpaid() {
        assertEquals(CrmSaleOrderPaymentStatusEnum.UNPAID.getStatus(),
                CrmPaymentStatusUtils.resolve(BigDecimal.ZERO, new BigDecimal("1000.00")));
        // null 收款视为 0
        assertEquals(CrmSaleOrderPaymentStatusEnum.UNPAID.getStatus(),
                CrmPaymentStatusUtils.resolve(null, new BigDecimal("1000.00")));
    }

    @Test
    public void test_resolve_partialPaid() {
        assertEquals(CrmSaleOrderPaymentStatusEnum.PARTIAL_PAID.getStatus(),
                CrmPaymentStatusUtils.resolve(new BigDecimal("600.00"), new BigDecimal("1000.00")));
        assertEquals(CrmSaleOrderPaymentStatusEnum.PARTIAL_PAID.getStatus(),
                CrmPaymentStatusUtils.resolve(new BigDecimal("999.99"), new BigDecimal("1000.00")));
    }

    @Test
    public void test_resolve_paid() {
        assertEquals(CrmSaleOrderPaymentStatusEnum.PAID.getStatus(),
                CrmPaymentStatusUtils.resolve(new BigDecimal("1000.00"), new BigDecimal("1000.00")));
        // 防御：额度校验下不会超收，> 等额仍按已付款口径（不造 PARTIAL 假象）
        assertEquals(CrmSaleOrderPaymentStatusEnum.PAID.getStatus(),
                CrmPaymentStatusUtils.resolve(new BigDecimal("1200.00"), new BigDecimal("1000.00")));
    }

    @Test
    public void test_resolve_noBaseline() {
        // totalPrice null/0/负：无收齐基准，仅部分付款/未付款可达
        assertEquals(CrmSaleOrderPaymentStatusEnum.UNPAID.getStatus(),
                CrmPaymentStatusUtils.resolve(BigDecimal.ZERO, null));
        assertEquals(CrmSaleOrderPaymentStatusEnum.UNPAID.getStatus(),
                CrmPaymentStatusUtils.resolve(BigDecimal.ZERO, BigDecimal.ZERO));
        assertEquals(CrmSaleOrderPaymentStatusEnum.PARTIAL_PAID.getStatus(),
                CrmPaymentStatusUtils.resolve(new BigDecimal("100.00"), null));
        assertEquals(CrmSaleOrderPaymentStatusEnum.PARTIAL_PAID.getStatus(),
                CrmPaymentStatusUtils.resolve(new BigDecimal("100.00"), BigDecimal.ZERO));
        assertEquals(CrmSaleOrderPaymentStatusEnum.PARTIAL_PAID.getStatus(),
                CrmPaymentStatusUtils.resolve(new BigDecimal("100.00"), new BigDecimal("-5.00")));
    }

}
