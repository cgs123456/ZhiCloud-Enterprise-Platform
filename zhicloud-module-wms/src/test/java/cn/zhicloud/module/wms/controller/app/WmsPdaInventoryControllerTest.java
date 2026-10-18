package cn.zhicloud.module.wms.controller.app;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.wms.controller.admin.inventory.batch.vo.WmsInventoryBatchStrategyRespVO;
import cn.zhicloud.module.wms.controller.app.vo.WmsPdaConfirmMoveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryBatchDO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryDO;
import cn.zhicloud.module.wms.dal.dataobject.md.item.WmsItemDO;
import cn.zhicloud.module.wms.dal.dataobject.md.item.WmsItemSkuDO;
import cn.zhicloud.module.wms.dal.dataobject.md.location.WmsLocationDO;
import cn.zhicloud.module.wms.dal.mysql.inventory.WmsInventoryBatchMapper;
import cn.zhicloud.module.wms.dal.mysql.inventory.WmsInventoryMapper;
import cn.zhicloud.module.wms.dal.mysql.md.location.WmsLocationMapper;
import cn.zhicloud.module.wms.enums.md.WmsLocationStatusEnum;
import cn.zhicloud.module.wms.service.inventory.WmsInventoryHistoryService;
import cn.zhicloud.module.wms.service.inventory.WmsInventoryServiceImpl;
import cn.zhicloud.module.wms.service.inventory.batch.WmsInventoryBatchService;
import cn.zhicloud.module.wms.service.inventory.slotting.WmsSlottingService;
import cn.zhicloud.module.wms.service.inventory.transfer.WmsInventoryTransferServiceImpl;
import cn.zhicloud.module.wms.service.md.item.WmsItemService;
import cn.zhicloud.module.wms.service.md.item.WmsItemSkuService;
import cn.zhicloud.module.wms.service.md.location.WmsLocationServiceImpl;
import cn.zhicloud.module.wms.service.md.warehouse.WmsWarehouseService;
import cn.zhicloud.module.wms.service.md.zone.WmsZoneService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.INVENTORY_BATCH_FEFO_INSUFFICIENT;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.INVENTORY_NOT_EXISTS;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.INVENTORY_QUANTITY_NOT_ENOUGH;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.LOCATION_DISABLED;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.LOCATION_LOCKED;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.LOCATION_MOVE_CROSS_WAREHOUSE;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.LOCATION_MOVE_SAME;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.LOCATION_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link WmsPdaInventoryController} 移库真实化测试（V211 / ADR 269 D1–D3）
 *
 * 覆盖：来源/目标库位不存在、锁定、禁用拒绝（V205 门禁沿用）；同库位（008）/跨仓（009）
 * 拒绝；源行缺失（INVENTORY_NOT_EXISTS）；成功转移（源行扣减 + 目标行建行入账 + 占用派生）。
 */
@Import({WmsPdaInventoryController.class, WmsLocationServiceImpl.class,
        WmsInventoryTransferServiceImpl.class, WmsInventoryServiceImpl.class})
public class WmsPdaInventoryControllerTest extends BaseDbUnitTest {

    private static final AtomicInteger LOCATION_SEQ = new AtomicInteger(1);

    @Resource
    private WmsPdaInventoryController controller;
    @Resource
    private WmsLocationMapper locationMapper;
    @Resource
    private WmsInventoryMapper inventoryMapper;
    @Resource
    private WmsInventoryBatchMapper inventoryBatchMapper;

    @MockitoBean
    private WmsItemService itemService;
    @MockitoBean
    private WmsItemSkuService itemSkuService;
    @MockitoBean
    private WmsWarehouseService warehouseService;
    @MockitoBean
    private WmsZoneService zoneService;
    @MockitoBean
    private WmsInventoryHistoryService inventoryHistoryService;
    @MockitoBean
    private WmsInventoryBatchService inventoryBatchService;
    @MockitoBean
    private WmsSlottingService slottingService;

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
    public void testConfirmMove_sameLocation() {
        Long idle = insertLocation(WmsLocationStatusEnum.IDLE.getStatus());
        assertServiceException(() -> controller.confirmMove(buildMoveReqVO(idle, idle, 500L)),
                LOCATION_MOVE_SAME);
    }

    @Test
    public void testConfirmMove_crossWarehouse() {
        Long from = insertLocation(WmsLocationStatusEnum.IDLE.getStatus(), 1L);
        Long to = insertLocation(WmsLocationStatusEnum.IDLE.getStatus(), 2L);
        assertServiceException(() -> controller.confirmMove(buildMoveReqVO(from, to, 500L)),
                LOCATION_MOVE_CROSS_WAREHOUSE);
    }

    @Test
    public void testConfirmMove_sourceInventoryNotExists() {
        Long from = insertLocation(WmsLocationStatusEnum.IDLE.getStatus());
        Long to = insertLocation(WmsLocationStatusEnum.IDLE.getStatus());
        assertServiceException(() -> controller.confirmMove(buildMoveReqVO(from, to, 500L)),
                INVENTORY_NOT_EXISTS);
    }

    @Test
    public void testConfirmMove_batchFefoInsufficient() {
        // V211（ADR 269 D3）：带批次行源行 —— FEFO 前置检查不足整体拒绝（与拣货同码同参），源行/批次零副作用
        Long from = insertLocation(WmsLocationStatusEnum.IDLE.getStatus());
        Long to = insertLocation(WmsLocationStatusEnum.IDLE.getStatus());
        WmsInventoryDO source = WmsInventoryDO.builder().skuId(500L).warehouseId(1L).locationId(from)
                .quantity(new BigDecimal("3.00")).availableQuantity(new BigDecimal("3.00")).build();
        inventoryMapper.insert(source);
        inventoryBatchMapper.insert(WmsInventoryBatchDO.builder()
                .inventoryId(source.getId()).batchNo("PDA-BATCH-001")
                .quantity(new BigDecimal("3.00")).build());
        WmsInventoryBatchStrategyRespVO insufficient = new WmsInventoryBatchStrategyRespVO();
        insufficient.setSufficient(false);
        insufficient.setAllocatedQuantity(new BigDecimal("3.00"));
        insufficient.setAllocations(Collections.emptyList());
        Mockito.when(inventoryBatchService.applyFefoStrategy(source.getId(), new BigDecimal("99.00")))
                .thenReturn(insufficient);

        WmsPdaConfirmMoveReqVO reqVO = buildMoveReqVO(from, to, 500L);
        reqVO.setQuantity(new BigDecimal("99.00"));
        assertServiceException(() -> controller.confirmMove(reqVO), INVENTORY_BATCH_FEFO_INSUFFICIENT,
                source.getId(), source.getId(), new BigDecimal("99.00"), new BigDecimal("3.00"));

        assertEquals(0, new BigDecimal("3.00").compareTo(
                inventoryMapper.selectById(source.getId()).getQuantity()));
        assertEquals(0, new BigDecimal("3.00").compareTo(
                inventoryBatchMapper.selectListByInventoryId(source.getId()).get(0).getQuantity()));
    }

    @Test
    public void testConfirmMove_quantityNotEnoughWithoutBatches() {
        // V211（ADR 269 D3）：无批次行源行 —— 兼容分支跳过 FEFO，行级数量不足按既有码拒绝（1060300000）
        Long from = insertLocation(WmsLocationStatusEnum.IDLE.getStatus());
        Long to = insertLocation(WmsLocationStatusEnum.IDLE.getStatus());
        inventoryMapper.insert(WmsInventoryDO.builder().skuId(500L).warehouseId(1L).locationId(from)
                .quantity(new BigDecimal("3.00")).availableQuantity(new BigDecimal("3.00")).build());
        Mockito.when(itemSkuService.validateItemSkuExists(500L))
                .thenReturn(WmsItemSkuDO.builder().id(1L).itemId(1L).name("测试规格").build());
        Mockito.when(itemService.validateItemExists(1L))
                .thenReturn(WmsItemDO.builder().id(1L).name("测试商品").build());

        WmsPdaConfirmMoveReqVO reqVO = buildMoveReqVO(from, to, 500L);
        reqVO.setQuantity(new BigDecimal("99.00"));
        assertServiceException(() -> controller.confirmMove(reqVO), INVENTORY_QUANTITY_NOT_ENOUGH,
                "测试商品", "测试规格", 1L, new BigDecimal("3.00"), new BigDecimal("99.00"));
    }

    @Test
    public void testConfirmMove_success() {
        // V211（ADR 269 D1–D3）：库内同仓跨位真转移——源行扣减 + 目标行建行入账 + 占用派生
        Long from = insertLocation(WmsLocationStatusEnum.IDLE.getStatus());
        Long to = insertLocation(WmsLocationStatusEnum.IDLE.getStatus());
        inventoryMapper.insert(WmsInventoryDO.builder().skuId(500L).warehouseId(1L).locationId(from)
                .quantity(new BigDecimal("10.00")).availableQuantity(new BigDecimal("10.00")).build());

        assertTrue(controller.confirmMove(buildMoveReqVO(from, to, 500L)).getData());

        WmsInventoryDO source = inventoryMapper.selectBySkuIdAndWarehouseIdAndLocationId(500L, 1L, from);
        assertNotNull(source);
        assertEquals(0, new BigDecimal("9.00").compareTo(source.getQuantity()));
        assertEquals(0, new BigDecimal("9.00").compareTo(source.getAvailableQuantity()));
        WmsInventoryDO target = inventoryMapper.selectBySkuIdAndWarehouseIdAndLocationId(500L, 1L, to);
        assertNotNull(target);
        assertEquals(0, new BigDecimal("1.00").compareTo(target.getQuantity()));
        assertEquals(0, new BigDecimal("1.00").compareTo(target.getAvailableQuantity()));
        assertEquals(WmsLocationStatusEnum.OCCUPIED.getStatus(), locationMapper.selectById(from).getStatus());
        assertEquals(WmsLocationStatusEnum.OCCUPIED.getStatus(), locationMapper.selectById(to).getStatus());
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
        return insertLocation(status, 1L);
    }

    private Long insertLocation(Integer status, Long warehouseId) {
        WmsLocationDO location = WmsLocationDO.builder()
                .zoneId(1L)
                .warehouseId(warehouseId)
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
