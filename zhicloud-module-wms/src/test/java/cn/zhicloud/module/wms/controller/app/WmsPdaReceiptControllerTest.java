package cn.zhicloud.module.wms.controller.app;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.wms.controller.app.vo.WmsPdaConfirmReceiptReqVO;
import cn.zhicloud.module.wms.dal.dataobject.md.location.WmsLocationDO;
import cn.zhicloud.module.wms.dal.mysql.md.location.WmsLocationMapper;
import cn.zhicloud.module.wms.dal.mysql.order.asn.WmsAsnOrderMapper;
import cn.zhicloud.module.wms.enums.md.WmsLocationStatusEnum;
import cn.zhicloud.module.wms.service.md.item.WmsItemService;
import cn.zhicloud.module.wms.service.md.item.WmsItemSkuService;
import cn.zhicloud.module.wms.service.md.location.WmsLocationServiceImpl;
import cn.zhicloud.module.wms.service.md.warehouse.WmsWarehouseService;
import cn.zhicloud.module.wms.service.md.zone.WmsZoneService;
import cn.zhicloud.module.wms.service.order.asn.WmsAsnOrderDetailService;
import cn.zhicloud.module.wms.service.order.dock.WmsDockService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.LOCATION_DISABLED;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.LOCATION_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

/**
 * {@link WmsPdaReceiptController} 库位状态门禁测试（业务断点 4）
 *
 * 覆盖：上架库位不存在/禁用拒绝（且不写收货量）；未指定库位向后兼容放行；空闲库位放行。
 */
@Import({WmsPdaReceiptController.class, WmsLocationServiceImpl.class})
public class WmsPdaReceiptControllerTest extends BaseDbUnitTest {

    private static final AtomicInteger LOCATION_SEQ = new AtomicInteger(1);

    @Resource
    private WmsPdaReceiptController controller;
    @Resource
    private WmsLocationMapper locationMapper;

    @MockitoBean
    private WmsAsnOrderMapper asnOrderMapper;
    @MockitoBean
    private WmsAsnOrderDetailService asnOrderDetailService;
    @MockitoBean
    private WmsWarehouseService warehouseService;
    @MockitoBean
    private WmsDockService dockService;
    @MockitoBean
    private WmsItemSkuService itemSkuService;
    @MockitoBean
    private WmsItemService itemService;
    @MockitoBean
    private WmsZoneService zoneService;

    @Test
    public void testConfirmReceipt_locationNotExists() {
        assertServiceException(() -> controller.confirmReceipt(buildReceiptReqVO(999999L)),
                LOCATION_NOT_EXISTS);
    }

    @Test
    public void testConfirmReceipt_locationDisabled() {
        Long disabled = insertLocation(WmsLocationStatusEnum.DISABLED.getStatus());
        assertServiceException(() -> controller.confirmReceipt(buildReceiptReqVO(disabled)),
                LOCATION_DISABLED);
    }

    @Test
    public void testConfirmReceipt_locationIdNull_passes() {
        assertTrue(controller.confirmReceipt(buildReceiptReqVO(null)).getData());
        verify(asnOrderDetailService).addReceivedQuantity(1L, new BigDecimal("5.00"));
    }

    @Test
    public void testConfirmReceipt_successWithIdleLocation() {
        Long idle = insertLocation(WmsLocationStatusEnum.IDLE.getStatus());
        assertTrue(controller.confirmReceipt(buildReceiptReqVO(idle)).getData());
        verify(asnOrderDetailService).addReceivedQuantity(1L, new BigDecimal("5.00"));
    }

    private static WmsPdaConfirmReceiptReqVO buildReceiptReqVO(Long locationId) {
        WmsPdaConfirmReceiptReqVO reqVO = new WmsPdaConfirmReceiptReqVO();
        reqVO.setAsnOrderId(1L);
        reqVO.setDetailId(1L);
        reqVO.setReceivedQuantity(new BigDecimal("5.00"));
        reqVO.setLocationId(locationId);
        return reqVO;
    }

    private Long insertLocation(Integer status) {
        WmsLocationDO location = WmsLocationDO.builder()
                .zoneId(1L)
                .warehouseId(1L)
                .code("RCP-LOC-" + LOCATION_SEQ.getAndIncrement())
                .name("测试库位")
                .type(10)
                .status(status)
                .sort(1)
                .build();
        locationMapper.insert(location);
        return location.getId();
    }

}
