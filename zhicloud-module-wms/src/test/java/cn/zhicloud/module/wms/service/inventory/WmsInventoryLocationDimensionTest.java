package cn.zhicloud.module.wms.service.inventory;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryBatchDO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryDO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryHistoryDO;
import cn.zhicloud.module.wms.dal.dataobject.md.item.WmsItemDO;
import cn.zhicloud.module.wms.dal.dataobject.md.item.WmsItemSkuDO;
import cn.zhicloud.module.wms.dal.dataobject.md.location.WmsLocationDO;
import cn.zhicloud.module.wms.dal.mysql.inventory.WmsInventoryBatchMapper;
import cn.zhicloud.module.wms.dal.mysql.inventory.WmsInventoryHistoryMapper;
import cn.zhicloud.module.wms.dal.mysql.inventory.WmsInventoryMapper;
import cn.zhicloud.module.wms.dal.mysql.md.item.WmsItemMapper;
import cn.zhicloud.module.wms.dal.mysql.md.item.WmsItemSkuMapper;
import cn.zhicloud.module.wms.dal.mysql.md.location.WmsLocationMapper;
import cn.zhicloud.module.wms.enums.inventory.WmsInventoryChangeTypeEnum;
import cn.zhicloud.module.wms.enums.md.WmsLocationStatusEnum;
import cn.zhicloud.module.wms.enums.order.WmsOrderTypeEnum;
import cn.zhicloud.module.wms.service.inventory.dto.WmsInventoryChangeReqDTO;
import cn.zhicloud.module.wms.service.md.item.WmsItemService;
import cn.zhicloud.module.wms.service.md.item.WmsItemSkuService;
import cn.zhicloud.module.wms.service.md.location.WmsLocationService;
import cn.zhicloud.module.wms.service.md.location.WmsLocationServiceImpl;
import cn.zhicloud.module.wms.service.md.zone.WmsZoneService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.INVENTORY_QUANTITY_NOT_ENOUGH;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.LOCATION_DISABLED;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.LOCATION_HAS_INVENTORY;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.LOCATION_LOCKED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * V205 库位维度接入的单测层验证（§12.259，ADR 259 D3/D4/D7/D9/D12）
 *
 * @author 智云
 */
@Import({WmsInventoryServiceImpl.class, WmsInventoryHistoryServiceImpl.class, WmsLocationServiceImpl.class})
public class WmsInventoryLocationDimensionTest extends BaseDbUnitTest {

    @Resource
    private WmsInventoryServiceImpl inventoryService;
    @Resource
    private WmsLocationService locationService;

    @Resource
    private WmsInventoryMapper inventoryMapper;
    @Resource
    private WmsInventoryBatchMapper inventoryBatchMapper;
    @Resource
    private WmsInventoryHistoryMapper inventoryHistoryMapper;
    @Resource
    private WmsLocationMapper locationMapper;
    @Resource
    private WmsItemMapper itemMapper;
    @Resource
    private WmsItemSkuMapper skuMapper;

    @MockitoBean
    private WmsItemSkuService itemSkuService;
    @MockitoBean
    private WmsItemService itemService;
    @MockitoBean
    private WmsZoneService zoneService;

    @Test
    public void testPickPrimarySelectionOrder() {
        // V205（ADR 259 D8）：主行选择器——0 行有货优先 → 定位行可用量降序（排除冻结库位）→ 0 行兜底
        Long skuId = 9001L;
        WmsInventoryDO zeroStocked = WmsInventoryDO.builder().id(1L).skuId(skuId).warehouseId(100L)
                .quantity(new BigDecimal("1.00")).availableQuantity(new BigDecimal("1.00")).build();
        WmsInventoryDO locatedLow = WmsInventoryDO.builder().id(2L).skuId(skuId).warehouseId(100L).locationId(11L)
                .quantity(new BigDecimal("2.00")).availableQuantity(new BigDecimal("2.00")).build();
        WmsInventoryDO locatedHigh = WmsInventoryDO.builder().id(3L).skuId(skuId).warehouseId(100L).locationId(12L)
                .quantity(new BigDecimal("5.00")).availableQuantity(new BigDecimal("5.00")).build();
        List<WmsInventoryDO> rows = new ArrayList<>(Arrays.asList(zeroStocked, locatedLow, locatedHigh));

        // 1. 0 行有货优先
        assertEquals(1L, WmsInventoryMapper.pickPrimary(rows, null).getId());
        // 2. 无 0 行 → 定位行可用量最大者
        assertEquals(3L, WmsInventoryMapper.pickPrimary(new ArrayList<>(List.of(locatedLow, locatedHigh)), null).getId());
        // 3. 锁定/禁用库位排除（12 号被冻结 → 退而选择 11 号）
        assertEquals(2L, WmsInventoryMapper.pickPrimary(new ArrayList<>(List.of(locatedLow, locatedHigh)),
                Set.of(12L)).getId());
        // 4. 定位行全部被排除且 0 行无货 → 0 行兜底（上游复现缺货报错语义）
        WmsInventoryDO zeroEmpty = WmsInventoryDO.builder().id(9L).skuId(skuId).warehouseId(100L)
                .quantity(BigDecimal.ZERO).availableQuantity(BigDecimal.ZERO).build();
        assertEquals(9L, WmsInventoryMapper.pickPrimary(new ArrayList<>(List.of(locatedLow, zeroEmpty)),
                Set.of(11L)).getId());
        // 5. 空行集 → null
        assertNull(WmsInventoryMapper.pickPrimary(new ArrayList<>(), null));
    }

    @Test
    public void testChangeInventory_locatedRowAndOccupancyDerivation() {
        WmsItemDO item = createItem("ITEM-101", "定位入库出库");
        WmsItemSkuDO sku = createSku(item.getId(), "SKU-101", "10kg");
        WmsLocationDO location = createLocation("A-01-01", WmsLocationStatusEnum.IDLE.getStatus());

        // 1. IN 显式定位 → 三元键定位行落库 + 库位派生为占用（ADR 259 D4）
        inventoryService.changeInventory(changeReq(1L, "RK202601010001", sku.getId(), "6.00",
                location.getId(), WmsInventoryChangeTypeEnum.IN));

        WmsInventoryDO row = inventoryMapper.selectBySkuIdAndWarehouseIdAndLocationId(
                sku.getId(), 100L, location.getId());
        assertNotNull(row);
        assertEquals(0, new BigDecimal("6.00").compareTo(row.getQuantity()));
        assertEquals(0, new BigDecimal("6.00").compareTo(row.getAvailableQuantity()));
        assertEquals(WmsLocationStatusEnum.OCCUPIED.getStatus(),
                locationMapper.selectById(location.getId()).getStatus());
        // V211（ADR 269 D7）：定位行流水随行库位（真库位口径）
        List<WmsInventoryHistoryDO> locatedHistories = inventoryHistoryMapper.selectList();
        assertEquals(1, locatedHistories.size());
        assertEquals(location.getId(), locatedHistories.get(0).getLocationId());

        // 2. OUT 显式定位（拣货链同源语义，ADR 259 D3/D7）全量清空 → 行归零 + 库位回退空闲
        inventoryService.changeInventory(changeReq(2L, "CK202601010001", sku.getId(), "6.00",
                location.getId(), WmsInventoryChangeTypeEnum.OUT));

        assertEquals(0, BigDecimal.ZERO.compareTo(inventoryMapper.selectById(row.getId()).getQuantity()));
        assertEquals(WmsLocationStatusEnum.IDLE.getStatus(),
                locationMapper.selectById(location.getId()).getStatus());
    }

    @Test
    public void testChangeInventory_pdeductDistributesAcrossLocations() {
        WmsItemDO item = createItem("ITEM-102", "跨位分配");
        WmsItemSkuDO sku = createSku(item.getId(), "SKU-102", "10kg");
        WmsLocationDO locA = createLocation("A-01-02", WmsLocationStatusEnum.IDLE.getStatus());
        WmsLocationDO locB = createLocation("A-01-03", WmsLocationStatusEnum.IDLE.getStatus());
        // 缺货拒绝路径构造异常消息依赖 SKU/物料装配（与既有 WmsInventoryServiceImplTest 同款 stub）
        when(itemSkuService.validateItemSkuExists(sku.getId())).thenReturn(sku);
        when(itemService.validateItemExists(item.getId())).thenReturn(item);
        // 行布置：0 行（未定位）qty 2；A 行 qty 3；B 行 qty 4
        inventoryMapper.insert(WmsInventoryDO.builder().skuId(sku.getId()).warehouseId(100L)
                .quantity(new BigDecimal("2.00")).availableQuantity(new BigDecimal("2.00")).build());
        inventoryMapper.insert(WmsInventoryDO.builder().skuId(sku.getId()).warehouseId(100L).locationId(locA.getId())
                .quantity(new BigDecimal("3.00")).availableQuantity(new BigDecimal("3.00")).build());
        inventoryMapper.insert(WmsInventoryDO.builder().skuId(sku.getId()).warehouseId(100L).locationId(locB.getId())
                .quantity(new BigDecimal("4.00")).availableQuantity(new BigDecimal("4.00")).build());

        // 1. OUT locationId == null → P-deduct：0 行优先清零 → location_id 升序贪心（ADR 259 D3）
        inventoryService.changeInventory(changeReq(3L, "CK202601010002", sku.getId(), "6.00",
                null, WmsInventoryChangeTypeEnum.OUT));

        assertEquals(0, new BigDecimal("0.00").compareTo(inventoryMapper
                .selectBySkuIdAndWarehouseIdAndLocationId(sku.getId(), 100L, 0L).getQuantity()));
        assertEquals(0, new BigDecimal("0.00").compareTo(inventoryMapper
                .selectBySkuIdAndWarehouseIdAndLocationId(sku.getId(), 100L, locA.getId()).getQuantity()));
        assertEquals(0, new BigDecimal("3.00").compareTo(inventoryMapper
                .selectBySkuIdAndWarehouseIdAndLocationId(sku.getId(), 100L, locB.getId()).getQuantity()));
        // 2. 库位占用派生：清空行回退 IDLE，有货行保持 OCCUPIED
        assertEquals(WmsLocationStatusEnum.IDLE.getStatus(), locationMapper.selectById(locA.getId()).getStatus());
        assertEquals(WmsLocationStatusEnum.OCCUPIED.getStatus(), locationMapper.selectById(locB.getId()).getStatus());
        // 2.1 V211（ADR 269 D7）：行级流水——分配参与行逐行一条（0 行 take 2、A 行 take 3、B 行 take 1）
        List<WmsInventoryHistoryDO> pdeductHistories = inventoryHistoryMapper.selectList();
        pdeductHistories.sort(Comparator.comparing(WmsInventoryHistoryDO::getId));
        assertEquals(3, pdeductHistories.size());
        assertEquals(0L, pdeductHistories.get(0).getLocationId());
        assertEquals(0, new BigDecimal("2.00").compareTo(pdeductHistories.get(0).getQuantity()));
        assertEquals(0, new BigDecimal("2.00").compareTo(pdeductHistories.get(0).getBeforeQuantity()));
        assertEquals(0, BigDecimal.ZERO.compareTo(pdeductHistories.get(0).getAfterQuantity()));
        assertEquals(locA.getId(), pdeductHistories.get(1).getLocationId());
        assertEquals(0, new BigDecimal("3.00").compareTo(pdeductHistories.get(1).getQuantity()));
        assertEquals(0, new BigDecimal("3.00").compareTo(pdeductHistories.get(1).getBeforeQuantity()));
        assertEquals(0, BigDecimal.ZERO.compareTo(pdeductHistories.get(1).getAfterQuantity()));
        assertEquals(locB.getId(), pdeductHistories.get(2).getLocationId());
        assertEquals(0, new BigDecimal("1.00").compareTo(pdeductHistories.get(2).getQuantity()));
        assertEquals(0, new BigDecimal("4.00").compareTo(pdeductHistories.get(2).getBeforeQuantity()));
        assertEquals(0, new BigDecimal("3.00").compareTo(pdeductHistories.get(2).getAfterQuantity()));

        // 3. B 锁定后 demand 4.00 → 锁定行跳过分配，合计可用 0.00 < 4.00 → 整单拒绝（零副作用）
        locationMapper.updateById(new WmsLocationDO().setId(locB.getId())
                .setStatus(WmsLocationStatusEnum.LOCKED.getStatus()));
        assertServiceException(() -> inventoryService.changeInventory(changeReq(4L, "CK202601010003",
                sku.getId(), "4.00", null, WmsInventoryChangeTypeEnum.OUT)),
                INVENTORY_QUANTITY_NOT_ENOUGH, item.getName(), sku.getName(), 100L,
                new BigDecimal("0.00"), new BigDecimal("4.00"));
        assertEquals(0, new BigDecimal("3.00").compareTo(inventoryMapper
                .selectBySkuIdAndWarehouseIdAndLocationId(sku.getId(), 100L, locB.getId()).getQuantity()));
        assertEquals(WmsLocationStatusEnum.LOCKED.getStatus(), locationMapper.selectById(locB.getId()).getStatus());
        assertEquals(3L, inventoryHistoryMapper.selectCount()); // 拒绝路径零流水

        // 4. 人工解锁（IDLE）后恢复分配：B 剩余 3.00 全额出清并回退空闲
        locationMapper.updateById(new WmsLocationDO().setId(locB.getId())
                .setStatus(WmsLocationStatusEnum.IDLE.getStatus()));
        inventoryService.changeInventory(changeReq(5L, "CK202601010004", sku.getId(), "3.00",
                null, WmsInventoryChangeTypeEnum.OUT));
        assertEquals(0, BigDecimal.ZERO.compareTo(inventoryMapper
                .selectBySkuIdAndWarehouseIdAndLocationId(sku.getId(), 100L, locB.getId()).getQuantity()));
        assertEquals(WmsLocationStatusEnum.IDLE.getStatus(), locationMapper.selectById(locB.getId()).getStatus());
        assertEquals(4L, inventoryHistoryMapper.selectCount()); // B 行 take 3.00 追加一条行级流水
    }

    @Test
    public void testChangeInventory_explicitLocatedOut_onFrozenLocationRefused() {
        WmsItemDO item = createItem("ITEM-103", "锁定位拒扣");
        WmsItemSkuDO sku = createSku(item.getId(), "SKU-103", "10kg");
        WmsLocationDO lockedLoc = createLocation("A-02-01", WmsLocationStatusEnum.LOCKED.getStatus());
        WmsLocationDO disabledLoc = createLocation("A-02-02", WmsLocationStatusEnum.DISABLED.getStatus());
        inventoryMapper.insert(WmsInventoryDO.builder().skuId(sku.getId()).warehouseId(100L).locationId(lockedLoc.getId())
                .quantity(new BigDecimal("5.00")).availableQuantity(new BigDecimal("5.00")).build());
        inventoryMapper.insert(WmsInventoryDO.builder().skuId(sku.getId()).warehouseId(100L).locationId(disabledLoc.getId())
                .quantity(new BigDecimal("5.00")).availableQuantity(new BigDecimal("5.00")).build());

        // 1. 显式定位 OUT 落在 LOCKED 库位 → 如实拒绝（锁定位货不动）
        assertServiceException(() -> inventoryService.changeInventory(changeReq(6L, "CK202601010005",
                sku.getId(), "1.00", lockedLoc.getId(), WmsInventoryChangeTypeEnum.OUT)), LOCATION_LOCKED);
        // 2. 落在 DISABLED 库位 → LOCATION_DISABLED
        assertServiceException(() -> inventoryService.changeInventory(changeReq(7L, "CK202601010006",
                sku.getId(), "1.00", disabledLoc.getId(), WmsInventoryChangeTypeEnum.OUT)), LOCATION_DISABLED);
        // 3. 拒绝路径零副作用：两行库存余额不变
        assertEquals(0, new BigDecimal("5.00").compareTo(inventoryMapper
                .selectBySkuIdAndWarehouseIdAndLocationId(sku.getId(), 100L, lockedLoc.getId()).getQuantity()));
        assertEquals(0, new BigDecimal("5.00").compareTo(inventoryMapper
                .selectBySkuIdAndWarehouseIdAndLocationId(sku.getId(), 100L, disabledLoc.getId()).getQuantity()));
    }

    @Test
    public void testDeleteLocation_rejectsOccupiedThenWashesZeroRows() {
        WmsItemDO item = createItem("ITEM-104", "库位删除门禁");
        WmsItemSkuDO sku = createSku(item.getId(), "SKU-104", "10kg");
        WmsLocationDO stocked = createLocation("A-03-01", WmsLocationStatusEnum.IDLE.getStatus());
        inventoryMapper.insert(WmsInventoryDO.builder().skuId(sku.getId()).warehouseId(100L).locationId(stocked.getId())
                .quantity(new BigDecimal("3.00")).availableQuantity(new BigDecimal("3.00")).build());

        // 1. 有货行 → LOCATION_HAS_INVENTORY（ADR 259 D9，休眠码激活）
        assertServiceException(() -> locationService.deleteLocation(stocked.getId()), LOCATION_HAS_INVENTORY);

        // 2. 持有锁占用的行（防御分支：locked > 0） → 同样拒删
        WmsLocationDO lockedHold = createLocation("A-03-02", WmsLocationStatusEnum.IDLE.getStatus());
        inventoryMapper.insert(WmsInventoryDO.builder().skuId(sku.getId()).warehouseId(100L).locationId(lockedHold.getId())
                .quantity(BigDecimal.ZERO).availableQuantity(new BigDecimal("-2.00"))
                .lockedQuantity(new BigDecimal("2.00")).build());
        assertServiceException(() -> locationService.deleteLocation(lockedHold.getId()), LOCATION_HAS_INVENTORY);

        // 3. 纯零行 → 物理冲洗 + 库位逻辑删（V211：批次行随行冲洗）
        WmsLocationDO washable = createLocation("A-03-03", WmsLocationStatusEnum.IDLE.getStatus());
        WmsInventoryDO washRow = WmsInventoryDO.builder().skuId(sku.getId()).warehouseId(100L)
                .locationId(washable.getId())
                .quantity(BigDecimal.ZERO).availableQuantity(BigDecimal.ZERO).build();
        inventoryMapper.insert(washRow);
        inventoryBatchMapper.insert(WmsInventoryBatchDO.builder()
                .inventoryId(washRow.getId()).batchNo("BATCH-WASH-001")
                .quantity(BigDecimal.ZERO).productionDate(LocalDate.of(2026, 1, 1))
                .expiryDate(LocalDate.of(2027, 1, 1)).build());
        locationService.deleteLocation(washable.getId());
        assertNull(locationMapper.selectById(washable.getId()));
        assertTrue(inventoryMapper.selectListByLocationId(washable.getId()).isEmpty());
        assertTrue(inventoryBatchMapper.selectListByInventoryId(washRow.getId()).isEmpty());
        // 物理冲洗实证：同 (sku, warehouse, location) 三元再插不撞唯一键（逻辑删 tombstone 已被物理清除）
        inventoryMapper.insert(WmsInventoryDO.builder().skuId(sku.getId()).warehouseId(100L).locationId(washable.getId())
                .quantity(BigDecimal.ZERO).availableQuantity(BigDecimal.ZERO).build());
        // 物理冲洗实证（批次）：同 (inventory_id, batch_no) 再插不撞唯一键（uk 不含 deleted，tombstone 必须已物理清除）
        inventoryBatchMapper.insert(WmsInventoryBatchDO.builder()
                .inventoryId(washRow.getId()).batchNo("BATCH-WASH-001")
                .quantity(BigDecimal.ZERO).productionDate(LocalDate.of(2026, 1, 1))
                .expiryDate(LocalDate.of(2027, 1, 1)).build());
    }

    private WmsItemDO createItem(String code, String name) {
        WmsItemDO item = WmsItemDO.builder().code(code).name(name).unit("箱").categoryId(1L).build();
        itemMapper.insert(item);
        return item;
    }

    private WmsItemSkuDO createSku(Long itemId, String code, String name) {
        WmsItemSkuDO sku = WmsItemSkuDO.builder().itemId(itemId).code(code).name(name).barCode("69010002").build();
        skuMapper.insert(sku);
        return sku;
    }

    private WmsLocationDO createLocation(String code, Integer status) {
        WmsLocationDO location = WmsLocationDO.builder().zoneId(1L).warehouseId(100L)
                .code(code).name(code).status(status).build();
        locationMapper.insert(location);
        return location;
    }

    private static WmsInventoryChangeReqDTO changeReq(Long orderId, String orderNo, Long skuId,
                                                      String quantity, Long locationId,
                                                      WmsInventoryChangeTypeEnum changeType) {
        return new WmsInventoryChangeReqDTO()
                .setOrderId(orderId)
                .setOrderNo(orderNo)
                .setOrderType(WmsOrderTypeEnum.SHIPMENT.getType())
                .setItems(List.of(new WmsInventoryChangeReqDTO.Item()
                        .setSkuId(skuId)
                        .setWarehouseId(100L)
                        .setQuantity(new BigDecimal(quantity))
                        .setPrice(new BigDecimal("10.00"))
                        .setTotalPrice(new BigDecimal(quantity).multiply(new BigDecimal("10.00")))
                        .setLocationId(locationId)
                        .setChangeType(changeType)));
    }

}