package cn.zhicloud.module.wms.controller.app;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.wms.controller.app.vo.WmsPdaConfirmMoveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryDO;
import cn.zhicloud.module.wms.dal.dataobject.md.location.WmsLocationDO;
import cn.zhicloud.module.wms.dal.mysql.inventory.WmsInventoryMapper;
import cn.zhicloud.module.wms.dal.mysql.md.location.WmsLocationMapper;
import cn.zhicloud.module.wms.enums.md.WmsLocationStatusEnum;
import cn.zhicloud.module.wms.service.md.item.WmsItemService;
import cn.zhicloud.module.wms.service.md.location.WmsLocationServiceImpl;
import cn.zhicloud.module.wms.service.md.warehouse.WmsWarehouseService;
import cn.zhicloud.module.wms.service.md.zone.WmsZoneService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.LOCATION_DISABLED;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.LOCATION_LOCKED;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.LOCATION_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link WmsPdaInventoryController} 库位状态门禁测试（业务断点 4）
 *
 * 覆盖：来源/目标库位不存在、锁定、禁用拒绝；空闲/占用放行。
 */
@Import({WmsPdaInventoryController.class, WmsLocationServiceImpl.class})
public class WmsPdaInventoryControllerTest extends BaseDbUnitTest {

    private static final AtomicInteger LOCATION_SEQ = new AtomicInteger(1);

    @Resource
    private WmsPdaInventoryController controller;
    @Resource
    private WmsLocationMapper locationMapper;
    @Resource
    private WmsInventoryMapper inventoryMapper;

    @MockitoBean
    private WmsItemService itemService;
    @MockitoBean
    private WmsWarehouseService warehouseService;
    @MockitoBean
    private WmsZoneService zoneService;

    @Test
    public void testConfirmMove_fromLocationNotExists() {
        assertServiceException(() -> controller.confirmMove(buildMoveReqVO(999998L, 999999L, 500L)),
                LOCATION_NOT_EXISTS);
    }

    @Test
    public void testConfirmMove_toLocationNotExists() {
        Long from = insertLocation(WmsLocationStatusEnum.IDLE.getStatus());
        assertServiceException(() -> controller.confirmMove(buildMoveReqVO(from, 999997L, 500L)),
                LOCATION_NOT_EXISTS);
    }

    @Test
    public void testConfirmMove_fromLocationLocked() {
        Long locked = insertLocation(WmsLocationStatusEnum.LOCKED.getStatus());
        Long idle = insertLocation(WmsLocationStatusEnum.IDLE.getStatus());
        assertServiceException(() -> controller.confirmMove(buildMoveReqVO(locked, idle, 500L)),
                LOCATION_LOCKED);
    }

    @Test
    public void testConfirmMove_toLocationDisabled() {
        Long idle = insertLocation(WmsLocationStatusEnum.IDLE.getStatus());
        Long disabled = insertLocation(WmsLocationStatusEnum.DISABLED.getStatus());
        assertServiceException(() -> controller.confirmMove(buildMoveReqVO(idle, disabled, 500L)),
                LOCATION_DISABLED);
    }

    @Test
    public void testConfirmMove_success() {
        Long from = insertLocation(WmsLocationStatusEnum.IDLE.getStatus());
        Long to = insertLocation(WmsLocationStatusEnum.OCCUPIED.getStatus());
        WmsInventoryDO inventory = new WmsInventoryDO();
        inventory.setSkuId(500L);
        inventory.setWarehouseId(1L);
        inventory.setQuantity(new BigDecimal("10.00"));
        inventory.setAvailableQuantity(new BigDecimal("10.0000"));
        inventoryMapper.insert(inventory);

        assertTrue(controller.confirmMove(buildMoveReqVO(from, to, 500L)).getData());
    }

    private static WmsPdaConfirmMoveReqVO buildMoveReqVO(Long fromLocationId, Long toLocationId, Long skuId) {
        WmsPdaConfirmMoveReqVO reqVO = new WmsPdaConfirmMoveReqVO();
        reqVO.setFromLocationId(fromLocationId);
        reqVO.setToLocationId(toLocationId);
        reqVO.setSkuId(skuId);
        reqVO.setQuantity(new BigDecimal("1.00"));
        return reqVO;
    }

    private Long insertLocation(Integer status) {
        WmsLocationDO location = WmsLocationDO.builder()
                .zoneId(1L)
                .warehouseId(1L)
                .code("PDA-LOC-" + LOCATION_SEQ.getAndIncrement())
                .name("测试库位")
                .type(10)
                .status(status)
                .sort(1)
                .build();
        locationMapper.insert(location);
        return location.getId();
    }

}
