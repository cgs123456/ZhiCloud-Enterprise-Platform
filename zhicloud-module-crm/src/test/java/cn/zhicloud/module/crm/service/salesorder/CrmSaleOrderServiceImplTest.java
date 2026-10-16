package cn.zhicloud.module.crm.service.salesorder;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.crm.dal.dataobject.salesorder.CrmSaleOrderDO;
import cn.zhicloud.module.crm.dal.mysql.salesorder.CrmSaleOrderMapper;
import cn.zhicloud.module.crm.dal.redis.no.CrmNoRedisDAO;
import cn.zhicloud.module.crm.enums.salesorder.CrmSaleOrderDeliveryStatusEnum;
import cn.zhicloud.module.crm.enums.salesorder.CrmSaleOrderPaymentStatusEnum;
import cn.zhicloud.module.crm.enums.salesorder.CrmSaleOrderStatusEnum;
import cn.zhicloud.module.crm.service.contract.CrmContractService;
import cn.zhicloud.module.crm.service.customer.CrmCustomerService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.framework.test.core.util.RandomUtils.randomLongId;
import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link CrmSaleOrderServiceImpl} 的单元测试：销售订单生命周期状态流转（CAS）
 *
 * 覆盖业务断点 3（§12.241）：confirm/ship/complete/cancel 四类迁移的出口与并发语义
 * （不复活已取消订单、重复动作不重复生效）。
 */
@Import(CrmSaleOrderServiceImpl.class)
public class CrmSaleOrderServiceImplTest extends BaseDbUnitTest {

    @Resource
    private CrmSaleOrderServiceImpl saleOrderService;

    @Resource
    private CrmSaleOrderMapper saleOrderMapper;

    @MockitoBean
    private CrmCustomerService customerService;
    @MockitoBean
    private CrmContractService contractService;
    @MockitoBean
    private CrmNoRedisDAO noRedisDAO;

    @Test
    public void test_confirmSaleOrder_success() {
        // mock 数据
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.DRAFT.getStatus());
        // 调用
        saleOrderService.confirmSaleOrder(order.getId());
        // 校验
        assertEquals(CrmSaleOrderStatusEnum.CONFIRMED.getStatus(),
                saleOrderMapper.selectById(order.getId()).getStatus());
    }

    @Test
    public void test_confirmSaleOrder_failNotDraft() {
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.CONFIRMED.getStatus());
        assertServiceException(() -> saleOrderService.confirmSaleOrder(order.getId()),
                SALE_ORDER_CONFIRM_FAIL_NOT_DRAFT);
        assertEquals(CrmSaleOrderStatusEnum.CONFIRMED.getStatus(),
                saleOrderMapper.selectById(order.getId()).getStatus());
    }

    @Test
    public void test_confirmSaleOrder_notExists() {
        assertServiceException(() -> saleOrderService.confirmSaleOrder(randomLongId()),
                SALE_ORDER_NOT_EXISTS);
    }

    @Test
    public void test_shipSaleOrder_success() {
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.CONFIRMED.getStatus());
        saleOrderService.shipSaleOrder(order.getId());
        CrmSaleOrderDO result = saleOrderMapper.selectById(order.getId());
        assertEquals(CrmSaleOrderStatusEnum.SHIPPED.getStatus(), result.getStatus());
        assertEquals(CrmSaleOrderDeliveryStatusEnum.SHIPPED.getStatus(), result.getDeliveryStatus());
    }

    @Test
    public void test_shipSaleOrder_failNotConfirmed() {
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.DRAFT.getStatus());
        assertServiceException(() -> saleOrderService.shipSaleOrder(order.getId()),
                SALE_ORDER_SHIP_FAIL_NOT_CONFIRMED);
        assertEquals(CrmSaleOrderStatusEnum.DRAFT.getStatus(),
                saleOrderMapper.selectById(order.getId()).getStatus());
    }

    @Test
    public void test_shipSaleOrder_notExists() {
        assertServiceException(() -> saleOrderService.shipSaleOrder(randomLongId()),
                SALE_ORDER_NOT_EXISTS);
    }

    @Test
    public void test_completeSaleOrder_success() {
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.SHIPPED.getStatus());
        saleOrderService.completeSaleOrder(order.getId());
        CrmSaleOrderDO result = saleOrderMapper.selectById(order.getId());
        assertEquals(CrmSaleOrderStatusEnum.COMPLETED.getStatus(), result.getStatus());
        assertEquals(CrmSaleOrderDeliveryStatusEnum.SIGNED.getStatus(), result.getDeliveryStatus());
    }

    @Test
    public void test_completeSaleOrder_failNotShipped() {
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.CONFIRMED.getStatus());
        assertServiceException(() -> saleOrderService.completeSaleOrder(order.getId()),
                SALE_ORDER_COMPLETE_FAIL_NOT_SHIPPED);
        assertEquals(CrmSaleOrderStatusEnum.CONFIRMED.getStatus(),
                saleOrderMapper.selectById(order.getId()).getStatus());
    }

    @Test
    public void test_completeSaleOrder_notExists() {
        assertServiceException(() -> saleOrderService.completeSaleOrder(randomLongId()),
                SALE_ORDER_NOT_EXISTS);
    }

    @Test
    public void test_cancelSaleOrder_fromDraft_success() {
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.DRAFT.getStatus());
        saleOrderService.cancelSaleOrder(order.getId());
        assertEquals(CrmSaleOrderStatusEnum.CANCELED.getStatus(),
                saleOrderMapper.selectById(order.getId()).getStatus());
    }

    @Test
    public void test_cancelSaleOrder_fromConfirmed_success() {
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.CONFIRMED.getStatus());
        saleOrderService.cancelSaleOrder(order.getId());
        CrmSaleOrderDO result = saleOrderMapper.selectById(order.getId());
        assertEquals(CrmSaleOrderStatusEnum.CANCELED.getStatus(), result.getStatus());
        // 取消不动发货/付款状态
        assertEquals(CrmSaleOrderDeliveryStatusEnum.UN_SHIPPED.getStatus(), result.getDeliveryStatus());
        assertEquals(CrmSaleOrderPaymentStatusEnum.UNPAID.getStatus(), result.getPaymentStatus());
    }

    @Test
    public void test_cancelSaleOrder_failAfterShipped() {
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.SHIPPED.getStatus());
        assertServiceException(() -> saleOrderService.cancelSaleOrder(order.getId()),
                SALE_ORDER_CANCEL_FAIL);
        assertEquals(CrmSaleOrderStatusEnum.SHIPPED.getStatus(),
                saleOrderMapper.selectById(order.getId()).getStatus());
    }

    @Test
    public void test_cancelSaleOrder_failAfterCompleted() {
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.COMPLETED.getStatus());
        assertServiceException(() -> saleOrderService.cancelSaleOrder(order.getId()),
                SALE_ORDER_CANCEL_FAIL);
    }

    @Test
    public void test_cancelSaleOrder_twice_fail() {
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.DRAFT.getStatus());
        saleOrderService.cancelSaleOrder(order.getId());
        assertServiceException(() -> saleOrderService.cancelSaleOrder(order.getId()),
                SALE_ORDER_CANCEL_FAIL);
        assertEquals(CrmSaleOrderStatusEnum.CANCELED.getStatus(),
                saleOrderMapper.selectById(order.getId()).getStatus());
    }

    @Test
    public void test_cancelSaleOrder_notExists() {
        assertServiceException(() -> saleOrderService.cancelSaleOrder(randomLongId()),
                SALE_ORDER_NOT_EXISTS);
    }

    @Test
    public void test_confirmAfterCancel_notResurrect() {
        // 取消后确认必须失败，且状态保持已取消（CAS 语义：不复活终态订单）
        CrmSaleOrderDO order = insertOrder(CrmSaleOrderStatusEnum.DRAFT.getStatus());
        saleOrderService.cancelSaleOrder(order.getId());
        assertServiceException(() -> saleOrderService.confirmSaleOrder(order.getId()),
                SALE_ORDER_CONFIRM_FAIL_NOT_DRAFT);
        assertEquals(CrmSaleOrderStatusEnum.CANCELED.getStatus(),
                saleOrderMapper.selectById(order.getId()).getStatus());
    }

    private CrmSaleOrderDO insertOrder(Integer status) {
        CrmSaleOrderDO order = randomPojo(CrmSaleOrderDO.class, o -> {
            o.setStatus(status);
            o.setDeliveryStatus(CrmSaleOrderDeliveryStatusEnum.UN_SHIPPED.getStatus());
            o.setPaymentStatus(CrmSaleOrderPaymentStatusEnum.UNPAID.getStatus());
            o.setDeleted(false);
        });
        saleOrderMapper.insert(order);
        return order;
    }

}
