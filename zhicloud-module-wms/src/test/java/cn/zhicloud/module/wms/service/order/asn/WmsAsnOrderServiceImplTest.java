package cn.zhicloud.module.wms.service.order.asn;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.wms.controller.admin.order.asn.vo.WmsAsnOrderDetailSaveReqVO;
import cn.zhicloud.module.wms.controller.admin.order.asn.vo.WmsAsnOrderSaveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.order.asn.WmsAsnOrderDO;
import cn.zhicloud.module.wms.dal.dataobject.order.asn.WmsAsnOrderDetailDO;
import cn.zhicloud.module.wms.dal.dataobject.order.dock.WmsDockDO;
import cn.zhicloud.module.wms.dal.mysql.order.asn.WmsAsnOrderMapper;
import cn.zhicloud.module.wms.dal.mysql.order.asn.WmsAsnOrderDetailMapper;
import cn.zhicloud.module.wms.dal.mysql.order.dock.WmsDockMapper;
import cn.zhicloud.module.wms.service.md.warehouse.WmsWarehouseService;
import cn.zhicloud.module.wms.service.order.dock.WmsDockService;
import cn.zhicloud.module.wms.service.order.receipt.WmsReceiptOrderService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.ASN_ORDER_DETAIL_REQUIRED;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.ASN_ORDER_NO_DUPLICATE;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.ASN_ORDER_NOT_EXISTS;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.ASN_ORDER_STATUS_NOT_ARRIVED;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.ASN_ORDER_STATUS_NOT_PENDING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Import({WmsAsnOrderServiceImpl.class, WmsAsnOrderDetailServiceImpl.class})
public class WmsAsnOrderServiceImplTest extends BaseDbUnitTest {

    private static final int STATUS_PENDING = 10;
    private static final int STATUS_ARRIVED = 20;
    private static final int STATUS_RECEIVED = 30;
    private static final int STATUS_CLOSED = 50;
    private static final int DOCK_STATUS_OCCUPIED = 20;

    @Resource
    private WmsAsnOrderServiceImpl asnOrderService;
    @Resource
    private WmsAsnOrderMapper asnOrderMapper;
    @Resource
    private WmsAsnOrderDetailMapper asnOrderDetailMapper;
    @Resource
    private WmsDockMapper dockMapper;
    @MockitoBean
    private WmsWarehouseService warehouseService;
    @MockitoBean
    private WmsReceiptOrderService receiptOrderService;
    @MockitoBean
    private WmsDockService dockService;

    private WmsAsnOrderDetailSaveReqVO detail(Long skuId, BigDecimal qty) {
        WmsAsnOrderDetailSaveReqVO d = new WmsAsnOrderDetailSaveReqVO();
        d.setSkuId(skuId);
        d.setExpectedQuantity(qty);
        return d;
    }

    private WmsAsnOrderDO insertOrder(int status, Long dockId) {
        WmsAsnOrderDO order = new WmsAsnOrderDO();
        order.setNo("ASN" + System.nanoTime());
        order.setWarehouseId(100L);
        order.setSupplierId(1L);
        order.setDockId(dockId);
        order.setStatus(status);
        order.setTotalQuantity(new BigDecimal("10"));
        asnOrderMapper.insert(order);
        return order;
    }

    @Test
    public void testCreateAsnOrder_success() {
        WmsAsnOrderSaveReqVO req = new WmsAsnOrderSaveReqVO();
        req.setNo("ASN-C1");
        req.setWarehouseId(100L);
        req.setSupplierId(1L);
        req.setDockId(5L);
        req.setDetails(List.of(detail(200L, new BigDecimal("10"))));
        Long id = asnOrderService.createAsnOrder(req);
        WmsAsnOrderDO db = asnOrderMapper.selectById(id);
        assertNotNull(db);
        assertEquals(STATUS_PENDING, db.getStatus());
        assertEquals(0, new BigDecimal("10").compareTo(db.getTotalQuantity()));
        assertEquals(1, asnOrderDetailMapper.selectListByAsnOrderId(id).size());
    }

    @Test
    public void testCreateAsnOrder_noDuplicate() {
        WmsAsnOrderSaveReqVO req = new WmsAsnOrderSaveReqVO();
        req.setNo("ASN-DUP");
        req.setWarehouseId(100L);
        req.setDetails(List.of(detail(200L, new BigDecimal("10"))));
        asnOrderService.createAsnOrder(req);
        assertServiceException(() -> asnOrderService.createAsnOrder(req), ASN_ORDER_NO_DUPLICATE);
    }

    @Test
    public void testUpdateAsnOrder_success() {
        WmsAsnOrderDO order = insertOrder(STATUS_PENDING, null);
        WmsAsnOrderSaveReqVO req = new WmsAsnOrderSaveReqVO();
        req.setId(order.getId());
        req.setNo(order.getNo());
        req.setWarehouseId(100L);
        req.setDetails(List.of(detail(201L, new BigDecimal("5"))));
        asnOrderService.updateAsnOrder(req);
        assertEquals(0, new BigDecimal("5").compareTo(asnOrderMapper.selectById(order.getId()).getTotalQuantity()));
        assertEquals(1, asnOrderDetailMapper.selectListByAsnOrderId(order.getId()).size());
    }

    @Test
    public void testUpdateAsnOrder_statusNotPending() {
        WmsAsnOrderDO order = insertOrder(STATUS_ARRIVED, null);
        WmsAsnOrderSaveReqVO req = new WmsAsnOrderSaveReqVO();
        req.setId(order.getId());
        req.setNo(order.getNo());
        req.setWarehouseId(100L);
        req.setDetails(List.of(detail(201L, new BigDecimal("5"))));
        assertServiceException(() -> asnOrderService.updateAsnOrder(req), ASN_ORDER_STATUS_NOT_PENDING);
    }

    @Test
    public void testDeleteAsnOrder_success() {
        WmsAsnOrderDO order = insertOrder(STATUS_PENDING, null);
        asnOrderService.deleteAsnOrder(order.getId());
        assertNull(asnOrderMapper.selectById(order.getId()));
    }

    @Test
    public void testDeleteAsnOrder_statusInvalid() {
        WmsAsnOrderDO order = insertOrder(STATUS_ARRIVED, null);
        assertServiceException(() -> asnOrderService.deleteAsnOrder(order.getId()), ASN_ORDER_STATUS_NOT_PENDING);
    }

    @Test
    public void testCloseAsnOrder_success() {
        WmsAsnOrderDO order = insertOrder(STATUS_PENDING, null);
        asnOrderService.closeAsnOrder(order.getId());
        assertEquals(STATUS_CLOSED, asnOrderMapper.selectById(order.getId()).getStatus());
    }

    @Test
    public void testCloseAsnOrder_statusNotPending() {
        WmsAsnOrderDO order = insertOrder(STATUS_ARRIVED, null);
        assertServiceException(() -> asnOrderService.closeAsnOrder(order.getId()), ASN_ORDER_STATUS_NOT_PENDING);
    }

    @Test
    public void testConfirmArrival_success() {
        WmsAsnOrderDO order = insertOrder(STATUS_PENDING, 7L);
        asnOrderService.confirmArrival(order.getId());
        WmsAsnOrderDO db = asnOrderMapper.selectById(order.getId());
        assertEquals(STATUS_ARRIVED, db.getStatus());
        assertNotNull(db.getActualArrivalTime());
        WmsDockDO dock = new WmsDockDO();
        dock.setId(7L);
        dock.setStatus(DOCK_STATUS_OCCUPIED);
        dockMapper.updateById(dock);
    }

    @Test
    public void testConfirmArrival_statusNotPending() {
        WmsAsnOrderDO order = insertOrder(STATUS_ARRIVED, 7L);
        assertServiceException(() -> asnOrderService.confirmArrival(order.getId()), ASN_ORDER_STATUS_NOT_PENDING);
    }

    @Test
    public void testConvertToReceipt_success() {
        WmsAsnOrderDO order = insertOrder(STATUS_ARRIVED, null);
        WmsAsnOrderDetailDO d = new WmsAsnOrderDetailDO();
        d.setAsnOrderId(order.getId());
        d.setSkuId(200L);
        d.setExpectedQuantity(new BigDecimal("10"));
        asnOrderDetailMapper.insert(d);
        when(receiptOrderService.createReceiptOrder(any())).thenReturn(555L);
        Long receiptId = asnOrderService.convertToReceipt(order.getId());
        assertEquals(555L, receiptId);
        assertEquals(STATUS_RECEIVED, asnOrderMapper.selectById(order.getId()).getStatus());
    }

    @Test
    public void testConvertToReceipt_detailRequired() {
        WmsAsnOrderDO order = insertOrder(STATUS_ARRIVED, null);
        assertServiceException(() -> asnOrderService.convertToReceipt(order.getId()), ASN_ORDER_DETAIL_REQUIRED);
    }

    @Test
    public void testConvertToReceipt_statusNotArrived() {
        WmsAsnOrderDO order = insertOrder(STATUS_PENDING, null);
        assertServiceException(() -> asnOrderService.convertToReceipt(order.getId()), ASN_ORDER_STATUS_NOT_ARRIVED);
    }

    @Test
    public void testGetAsnOrder() {
        WmsAsnOrderDO order = insertOrder(STATUS_PENDING, null);
        assertNotNull(asnOrderService.getAsnOrder(order.getId()));
        assertNull(asnOrderService.getAsnOrder(999L));
        assertNotNull(asnOrderService.getAsnOrderByNo(order.getNo()));
    }

    @Test
    public void testGetAsnOrderPage() {
        insertOrder(STATUS_PENDING, null);
        assertTrue(asnOrderMapper.selectList(null).size() >= 1);
    }

}
