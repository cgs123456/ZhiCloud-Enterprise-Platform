package cn.zhicloud.module.wms.service.order.wave;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.wms.controller.admin.order.wave.vo.order.WmsWaveOrderPageReqVO;
import cn.zhicloud.module.wms.controller.admin.order.wave.vo.order.WmsWaveOrderSaveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.order.shipment.WmsShipmentOrderDO;
import cn.zhicloud.module.wms.dal.dataobject.order.shipment.WmsShipmentOrderDetailDO;
import cn.zhicloud.module.wms.dal.dataobject.order.wave.WmsWaveOrderDO;
import cn.zhicloud.module.wms.dal.mysql.order.shipment.WmsShipmentOrderDetailMapper;
import cn.zhicloud.module.wms.dal.mysql.order.shipment.WmsShipmentOrderMapper;
import cn.zhicloud.module.wms.dal.mysql.order.wave.WmsWaveOrderDetailMapper;
import cn.zhicloud.module.wms.dal.mysql.order.wave.WmsWaveOrderMapper;
import cn.zhicloud.module.wms.enums.order.WmsOrderStatusEnum;
import cn.zhicloud.module.wms.enums.order.WmsShipmentOrderTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.SHIPMENT_ORDER_NOT_EXISTS;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.WAVE_ORDER_DETAIL_REQUIRED;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.WAVE_ORDER_NO_SHIPMENT_SELECTED;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.WAVE_ORDER_NOT_EXISTS;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.WAVE_ORDER_STATUS_NOT_DELETABLE;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.WAVE_ORDER_STATUS_NOT_PREPARE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(WmsWaveOrderServiceImpl.class)
public class WmsWaveOrderServiceImplTest extends BaseDbUnitTest {

    @Resource
    private WmsWaveOrderServiceImpl waveOrderService;
    @Resource
    private WmsWaveOrderMapper waveOrderMapper;
    @Resource
    private WmsWaveOrderDetailMapper waveOrderDetailMapper;
    @Resource
    private WmsShipmentOrderMapper shipmentOrderMapper;
    @Resource
    private WmsShipmentOrderDetailMapper shipmentOrderDetailMapper;

    private Long insertShipmentOrder(Long warehouseId) {
        WmsShipmentOrderDO order = new WmsShipmentOrderDO();
        order.setNo("CK" + System.nanoTime());
        order.setType(WmsShipmentOrderTypeEnum.SALE.getType());
        order.setWarehouseId(warehouseId);
        order.setOrderTime(LocalDateTime.now());
        order.setTotalQuantity(new BigDecimal("2"));
        order.setTotalPrice(new BigDecimal("40"));
        order.setStatus(WmsOrderStatusEnum.PREPARE.getStatus());
        shipmentOrderMapper.insert(order);
        return order.getId();
    }

    private void insertShipmentDetail(Long orderId, Long skuId, BigDecimal qty, BigDecimal price) {
        WmsShipmentOrderDetailDO d = WmsShipmentOrderDetailDO.builder()
                .orderId(orderId)
                .skuId(skuId)
                .warehouseId(100L)
                .quantity(qty)
                .price(price)
                .totalPrice(qty.multiply(price))
                .build();
        shipmentOrderDetailMapper.insert(d);
    }

    private Long createWave(WmsOrderStatusEnum status) {
        Long whId = 100L;
        Long shipId = insertShipmentOrder(whId);
        insertShipmentDetail(shipId, 200L, new BigDecimal("2"), new BigDecimal("20"));
        WmsWaveOrderSaveReqVO req = new WmsWaveOrderSaveReqVO();
        req.setWarehouseId(whId);
        req.setStrategy(1);
        req.setShipmentOrderIds(Collections.singletonList(shipId));
        Long id = waveOrderService.createWaveOrder(req);
        if (!WmsOrderStatusEnum.PREPARE.equals(status)) {
            WmsWaveOrderDO upd = new WmsWaveOrderDO();
            upd.setId(id);
            upd.setStatus(status.getStatus());
            waveOrderMapper.updateById(upd);
        }
        return id;
    }

    @Test
    public void testCreateWaveOrder_success() {
        Long whId = 100L;
        Long shipId = insertShipmentOrder(whId);
        insertShipmentDetail(shipId, 200L, new BigDecimal("2"), new BigDecimal("20"));
        WmsWaveOrderSaveReqVO req = new WmsWaveOrderSaveReqVO();
        req.setWarehouseId(whId);
        req.setStrategy(1);
        req.setShipmentOrderIds(Collections.singletonList(shipId));
        Long id = waveOrderService.createWaveOrder(req);
        WmsWaveOrderDO db = waveOrderMapper.selectById(id);
        assertNotNull(db);
        assertEquals(1, db.getShipmentCount());
        assertEquals(1, db.getSkuCount());
        assertEquals(0, new BigDecimal("2").compareTo(db.getTotalQuantity()));
        assertEquals(0, new BigDecimal("40").compareTo(db.getTotalPrice()));
        assertEquals(1, waveOrderDetailMapper.selectListByWaveOrderId(id).size());
    }

    @Test
    public void testCreateWaveOrder_noShipmentSelected() {
        WmsWaveOrderSaveReqVO req = new WmsWaveOrderSaveReqVO();
        req.setWarehouseId(100L);
        req.setShipmentOrderIds(Collections.emptyList());
        assertServiceException(() -> waveOrderService.createWaveOrder(req), WAVE_ORDER_NO_SHIPMENT_SELECTED);
    }

    @Test
    public void testCreateWaveOrder_shipmentNotExists() {
        WmsWaveOrderSaveReqVO req = new WmsWaveOrderSaveReqVO();
        req.setWarehouseId(100L);
        req.setShipmentOrderIds(List.of(999L));
        assertServiceException(() -> waveOrderService.createWaveOrder(req), SHIPMENT_ORDER_NOT_EXISTS);
    }

    @Test
    public void testCreateWaveOrder_warehouseMismatch() {
        Long shipId = insertShipmentOrder(100L);
        WmsWaveOrderSaveReqVO req = new WmsWaveOrderSaveReqVO();
        req.setWarehouseId(200L);
        req.setShipmentOrderIds(List.of(shipId));
        assertServiceException(() -> waveOrderService.createWaveOrder(req), WAVE_ORDER_DETAIL_REQUIRED);
    }

    @Test
    public void testUpdateWaveOrder_success() {
        Long whId = 100L;
        Long shipId = insertShipmentOrder(whId);
        insertShipmentDetail(shipId, 200L, new BigDecimal("2"), new BigDecimal("20"));
        WmsWaveOrderSaveReqVO create = new WmsWaveOrderSaveReqVO();
        create.setWarehouseId(whId);
        create.setStrategy(1);
        create.setShipmentOrderIds(Collections.singletonList(shipId));
        Long id = waveOrderService.createWaveOrder(create);

        Long shipId2 = insertShipmentOrder(whId);
        insertShipmentDetail(shipId2, 201L, new BigDecimal("3"), new BigDecimal("10"));
        WmsWaveOrderSaveReqVO upd = new WmsWaveOrderSaveReqVO();
        upd.setId(id);
        upd.setWarehouseId(whId);
        upd.setStrategy(1);
        upd.setShipmentOrderIds(Collections.singletonList(shipId2));
        upd.setRemark("updated");
        waveOrderService.updateWaveOrder(upd);
        WmsWaveOrderDO db = waveOrderMapper.selectById(id);
        assertEquals("updated", db.getRemark());
        // updateWaveOrder 仅从 reqVO 更新主表，不会重算 totalQuantity（保持创建时的聚合值）
        assertEquals(2, db.getTotalQuantity().intValue());
    }

    @Test
    public void testUpdateWaveOrder_notExists() {
        WmsWaveOrderSaveReqVO upd = new WmsWaveOrderSaveReqVO();
        upd.setId(999L);
        upd.setWarehouseId(100L);
        upd.setShipmentOrderIds(List.of(1L));
        assertServiceException(() -> waveOrderService.updateWaveOrder(upd), WAVE_ORDER_NOT_EXISTS);
    }

    @Test
    public void testUpdateWaveOrder_statusNotPrepare() {
        Long id = createWave(WmsOrderStatusEnum.FINISHED);
        Long shipId = insertShipmentOrder(100L);
        WmsWaveOrderSaveReqVO upd = new WmsWaveOrderSaveReqVO();
        upd.setId(id);
        upd.setWarehouseId(100L);
        upd.setShipmentOrderIds(List.of(shipId));
        assertServiceException(() -> waveOrderService.updateWaveOrder(upd), WAVE_ORDER_STATUS_NOT_PREPARE);
    }

    @Test
    public void testDeleteWaveOrder_prepare() {
        Long id = createWave(WmsOrderStatusEnum.PREPARE);
        waveOrderService.deleteWaveOrder(id);
        assertNull(waveOrderMapper.selectById(id));
    }

    @Test
    public void testDeleteWaveOrder_canceled() {
        Long id = createWave(WmsOrderStatusEnum.CANCELED);
        waveOrderService.deleteWaveOrder(id);
        assertNull(waveOrderMapper.selectById(id));
    }

    @Test
    public void testDeleteWaveOrder_statusNotDeletable() {
        Long id = createWave(WmsOrderStatusEnum.FINISHED);
        assertServiceException(() -> waveOrderService.deleteWaveOrder(id), WAVE_ORDER_STATUS_NOT_DELETABLE);
    }

    @Test
    public void testGetWaveOrder() {
        Long id = createWave(WmsOrderStatusEnum.PREPARE);
        assertNotNull(waveOrderService.getWaveOrder(id));
        assertNull(waveOrderService.getWaveOrder(999L));
    }

    @Test
    public void testGetWaveOrderPage() {
        createWave(WmsOrderStatusEnum.PREPARE);
        WmsWaveOrderPageReqVO pageReq = new WmsWaveOrderPageReqVO();
        assertTrue(waveOrderService.getWaveOrderPage(pageReq).getTotal() >= 1);
    }

    @Test
    public void testGetWaveOrderDetailList() {
        Long id = createWave(WmsOrderStatusEnum.PREPARE);
        assertEquals(1, waveOrderService.getWaveOrderDetailListByWaveOrderId(id).size());
    }

}
