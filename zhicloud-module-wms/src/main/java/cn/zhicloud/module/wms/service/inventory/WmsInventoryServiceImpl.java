package cn.zhicloud.module.wms.service.inventory;

import cn.zhicloud.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.ObjectUtil;
import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.number.MoneyUtils;
import cn.zhicloud.module.wms.controller.admin.inventory.vo.WmsInventoryListReqVO;
import cn.zhicloud.module.wms.controller.admin.inventory.vo.WmsInventoryPageReqVO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryDO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryHistoryDO;
import cn.zhicloud.module.wms.dal.dataobject.md.item.WmsItemDO;
import cn.zhicloud.module.wms.dal.dataobject.md.item.WmsItemSkuDO;
import cn.zhicloud.module.wms.dal.dataobject.md.location.WmsLocationDO;
import cn.zhicloud.module.wms.dal.mysql.inventory.WmsInventoryMapper;
import cn.zhicloud.module.wms.enums.inventory.WmsInventoryChangeTypeEnum;
import cn.zhicloud.module.wms.enums.md.WmsLocationStatusEnum;
import cn.zhicloud.module.wms.service.inventory.dto.WmsInventoryChangeReqDTO;
import cn.zhicloud.module.wms.service.inventory.dto.WmsInventoryCheckReqDTO;
import cn.zhicloud.module.wms.service.md.item.WmsItemService;
import cn.zhicloud.module.wms.service.md.item.WmsItemSkuService;
import cn.zhicloud.module.wms.service.md.location.WmsLocationService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertList;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.*;

/**
 * WMS 库存 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
@Slf4j
public class WmsInventoryServiceImpl implements WmsInventoryService {

    @Resource
    private WmsInventoryMapper inventoryMapper;

    @Resource
    private WmsInventoryHistoryService inventoryHistoryService;

    @Resource
    @Lazy // 延迟注入：打破 inventory ↔ itemSku 循环依赖
    private WmsItemSkuService itemSkuService;
    @Resource
    private WmsItemService itemService;

    @Resource
    private WmsLocationService locationService; // V205 库位维度：冻结库位批查 + 占用派生（ADR 259 D3/D4）

    @Override
    public PageResult<WmsInventoryDO> getInventoryPage(WmsInventoryPageReqVO pageReqVO) {
        return inventoryMapper.selectPage(pageReqVO);
    }

    @Override
    public List<WmsInventoryDO> getInventoryList(WmsInventoryListReqVO listReqVO) {
        return inventoryMapper.selectList(listReqVO);
    }

    @Override
    public long getInventoryCountBySkuId(Long skuId) {
        return inventoryMapper.selectCountBySkuId(skuId);
    }

    @Override
    public long getInventoryCountByWarehouseId(Long warehouseId) {
        return inventoryMapper.selectCountByWarehouseId(warehouseId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkInventory(WmsInventoryCheckReqDTO reqDTO) {
        if (reqDTO == null || CollUtil.isEmpty(reqDTO.getItems())) {
            return;
        }

        // 1. 逐条复核账面库存，并计算库存调整和库存流水
        List<WmsInventoryDO> updateInventories = new ArrayList<>(reqDTO.getItems().size());
        List<WmsInventoryHistoryDO> histories = new ArrayList<>(reqDTO.getItems().size());
        Set<Long> touchedLocationIds = new HashSet<>(); // V205：盘点触及的定位库位（ADR 259 D4）
        for (WmsInventoryCheckReqDTO.Item item : reqDTO.getItems()) {
            // 1.1 锁定或创建库存余额行
            WmsInventoryDO inventory = getOrCreateCheckInventory(item);
            // 1.2 无盈亏时不更新库存，也不生成库存流水
            BigDecimal beforeQuantity = inventory.getQuantity();
            BigDecimal afterQuantity = item.getCheckQuantity();
            if (beforeQuantity.compareTo(afterQuantity) == 0) {
                continue;
            }
            // 盘点调整 quantity 后，同步可用量 = quantity - 已锁定 - 已冻结，避免可用量长期失真
            BigDecimal locked = inventory.getLockedQuantity() == null ? BigDecimal.ZERO : inventory.getLockedQuantity();
            BigDecimal frozen = inventory.getFrozenQuantity() == null ? BigDecimal.ZERO : inventory.getFrozenQuantity();
            BigDecimal afterAvailable = afterQuantity.subtract(locked).subtract(frozen);
            updateInventories.add(new WmsInventoryDO().setId(inventory.getId())
                    .setQuantity(afterQuantity).setAvailableQuantity(afterAvailable));
            // V205：盘点定位行收集占用派生素材（ADR 259 D4）
            if (inventory.getLocationId() != null && inventory.getLocationId() != 0L) {
                touchedLocationIds.add(inventory.getLocationId());
            }
            histories.add(buildInventoryHistory(reqDTO, item, beforeQuantity, afterQuantity));
        }

        // 2.1 批量更新库存余额
        if (CollUtil.isNotEmpty(updateInventories)) {
            inventoryMapper.updateBatch(updateInventories);
            // V205：盘点定位行落库后派生库位占用/释放（ADR 259 D4）
            locationService.refreshOccupancyStatus(touchedLocationIds);
        }
        // 2.2 批量写入库存流水
        if (CollUtil.isNotEmpty(histories)) {
            inventoryHistoryService.createInventoryHistoryList(histories);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeInventory(WmsInventoryChangeReqDTO reqDTO) {
        if (reqDTO == null || CollUtil.isEmpty(reqDTO.getItems())) {
            return;
        }

        // 1. 补齐并锁定本次涉及的库存余额行，再计算库存变更
        Map<WmsInventoryChangeReqDTO.Item, Tuple> resultMap = changeInventoryList(reqDTO.getItems());

        // 2. 批量写入库存流水
        List<WmsInventoryHistoryDO> histories = new ArrayList<>(reqDTO.getItems().size());
        for (WmsInventoryChangeReqDTO.Item item : reqDTO.getItems()) {
            histories.add(buildInventoryHistory(reqDTO, item, resultMap.get(item)));
        }
        inventoryHistoryService.createInventoryHistoryList(histories);
    }

    /**
     * 批量变更库存余额
     *
     * 1. 库存行按 ID 批量加锁
     * 2. 库存变更先在内存计算并校验，全部通过后批量覆盖库存数量。
     *
     * P0-7 修复：根据 {@link WmsInventoryChangeReqDTO.Item#getChangeType()} 更新三维字段
     *（available/locked/frozen），覆盖入库/出库/锁定/解锁/冻结/解冻六大场景。
     *
     * V205 库位维度接入（ADR 259 D1/D3）：
     *  - item.locationId 非空（含显式 0）→ 精确该行（三元键）；OUT 落在锁定/禁用库位的行上时，
     *    如实抛出 LOCATION_LOCKED / LOCATION_DISABLED（锁定位货不动，门禁语义对齐入库面）；
     *  - item.locationId 为 null 的 OUT → P-deduct 跨位分配（{@link #distributeOut}）：
     *    (sku, warehouse) 全部行按「0 行优先、其余 location_id 升序」贪心扣减，跳过锁定/禁用
     *    库位的行，供需双口径（quantity 与 available）合计不足即整体拒绝（拒绝路径零副作用）；
     *  - 其余类型（IN/LOCK/UNLOCK/FREEZE/UNFREEZE/PLAIN）保持未定位行为（基线语义不变）。
     *
     * @param items 库存变更明细列表
     * @return 每条变更明细对应的变更前数量、变更后数量
     */
    private Map<WmsInventoryChangeReqDTO.Item, Tuple> changeInventoryList(List<WmsInventoryChangeReqDTO.Item> items) {
        // 1.1 创建或锁定库存行（V205：P-deduct 需求的 (sku, warehouse) 全部行并入行集）
        List<WmsInventoryDO> inventories = getOrCreateInventoryList(items);
        // 1.2 锁定库存：避免 quantity 变更时的并发问题，导致 quantity 前后计算不对
        inventories = inventoryMapper.selectListByIdsForUpdate(convertSet(inventories, WmsInventoryDO::getId));
        // 1.3 V205：锁定/禁用库位集批查（分配与主行选择共同排除的冻结面，ADR 259 D3/D8）
        Set<Long> frozenLocationIds = locationService.selectFrozenLocationIds(
                convertSet(inventories, WmsInventoryDO::getLocationId));

        // 2.1 校验库存充足 + 计算三维字段
        Map<WmsInventoryChangeReqDTO.Item, Tuple> resultMap = new IdentityHashMap<>(items.size());
        Set<Long> touchedLocationIds = new HashSet<>(); // V205：占用派生素材定位库位集（ADR 259 D4）
        for (WmsInventoryChangeReqDTO.Item item : items) {
            if (isDistributedOut(item)) {
                resultMap.put(item, distributeOut(item, inventories, frozenLocationIds, touchedLocationIds));
                continue;
            }
            WmsInventoryDO inventory = findInventory(inventories, item);
            if (inventory == null) {
                throw new ServiceException(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR, "库存行不存在，skuId=" + item.getSkuId()
                        + ", warehouseId=" + item.getWarehouseId() + ", locationId=" + item.getLocationIdOrZero());
            }
            // V205：显式定位行 OUT 的库位状态如实拒绝（锁定位货不动）
            if (WmsInventoryChangeTypeEnum.OUT.equals(item.getChangeType())
                    && item.getLocationIdOrZero() != 0L
                    && frozenLocationIds.contains(item.getLocationId())) {
                WmsLocationDO location = locationService.validateLocationExists(item.getLocationId());
                throw exception(WmsLocationStatusEnum.DISABLED.getStatus().equals(location.getStatus())
                        ? LOCATION_DISABLED : LOCATION_LOCKED);
            }
            BigDecimal beforeQuantity = inventory.getQuantity();
            BigDecimal qty = item.getQuantity();
            WmsInventoryChangeTypeEnum changeType = item.getChangeType();
            if (changeType == null) {
                changeType = WmsInventoryChangeTypeEnum.PLAIN;
            }
            BigDecimal afterQuantity;
            switch (changeType) {
                case IN: {
                    // 入库：quantity += qty, available += qty
                    afterQuantity = beforeQuantity.add(qty);
                    inventory.setQuantity(afterQuantity);
                    inventory.setAvailableQuantity(addNonNull(inventory.getAvailableQuantity(), qty));
                    break;
                }
                case OUT: {
                    // 出库：quantity -= qty, available -= qty（校验 available >= qty）
                    afterQuantity = beforeQuantity.subtract(qty);
                    if (afterQuantity.compareTo(BigDecimal.ZERO) < 0) {
                        throw buildInventoryQuantityNotEnoughException(item, beforeQuantity);
                    }
                    BigDecimal afterAvailable = subtractNonNull(inventory.getAvailableQuantity(), qty);
                    if (afterAvailable.compareTo(BigDecimal.ZERO) < 0) {
                        throw buildInventoryQuantityNotEnoughException(item, inventory.getAvailableQuantity());
                    }
                    inventory.setQuantity(afterQuantity);
                    inventory.setAvailableQuantity(afterAvailable);
                    break;
                }
                case LOCK: {
                    // 锁定：available -= qty, locked += qty（quantity 不变，校验 available >= qty）
                    BigDecimal afterAvailable = subtractNonNull(inventory.getAvailableQuantity(), qty);
                    if (afterAvailable.compareTo(BigDecimal.ZERO) < 0) {
                        throw buildInventoryQuantityNotEnoughException(item, inventory.getAvailableQuantity());
                    }
                    inventory.setAvailableQuantity(afterAvailable);
                    inventory.setLockedQuantity(addNonNull(inventory.getLockedQuantity(), qty));
                    afterQuantity = beforeQuantity; // quantity 不变
                    break;
                }
                case UNLOCK: {
                    // 解锁：available += qty, locked -= qty（quantity 不变，校验 locked >= qty）
                    BigDecimal afterLocked = subtractNonNull(inventory.getLockedQuantity(), qty);
                    if (afterLocked.compareTo(BigDecimal.ZERO) < 0) {
                        throw buildInventoryQuantityNotEnoughException(item, inventory.getLockedQuantity());
                    }
                    inventory.setAvailableQuantity(addNonNull(inventory.getAvailableQuantity(), qty));
                    inventory.setLockedQuantity(afterLocked);
                    afterQuantity = beforeQuantity; // quantity 不变
                    break;
                }
                case FREEZE: {
                    // 冻结：available -= qty, frozen += qty（quantity 不变，校验 available >= qty）
                    BigDecimal afterAvailable = subtractNonNull(inventory.getAvailableQuantity(), qty);
                    if (afterAvailable.compareTo(BigDecimal.ZERO) < 0) {
                        throw buildInventoryQuantityNotEnoughException(item, inventory.getAvailableQuantity());
                    }
                    inventory.setAvailableQuantity(afterAvailable);
                    inventory.setFrozenQuantity(addNonNull(inventory.getFrozenQuantity(), qty));
                    afterQuantity = beforeQuantity; // quantity 不变
                    break;
                }
                case UNFREEZE: {
                    // 解冻：available += qty, frozen -= qty（quantity 不变，校验 frozen >= qty）
                    BigDecimal afterFrozen = subtractNonNull(inventory.getFrozenQuantity(), qty);
                    if (afterFrozen.compareTo(BigDecimal.ZERO) < 0) {
                        throw buildInventoryQuantityNotEnoughException(item, inventory.getFrozenQuantity());
                    }
                    inventory.setAvailableQuantity(addNonNull(inventory.getAvailableQuantity(), qty));
                    inventory.setFrozenQuantity(afterFrozen);
                    afterQuantity = beforeQuantity; // quantity 不变
                    break;
                }
                case PLAIN:
                default: {
                    // 兼容旧逻辑：仅按 quantity 正负增减（不动三维字段）
                    afterQuantity = beforeQuantity.add(qty);
                    if (afterQuantity.compareTo(BigDecimal.ZERO) < 0) {
                        throw buildInventoryQuantityNotEnoughException(item, beforeQuantity);
                    }
                    inventory.setQuantity(afterQuantity);
                    break;
                }
            }
            resultMap.put(item, new Tuple(beforeQuantity, afterQuantity));
            // V205：由变更过的定位行收集占用派生素材（refreshOccupancyStatus 自行忽略 0/管理态/null）
            if (inventory.getLocationId() != null && inventory.getLocationId() != 0L) {
                touchedLocationIds.add(inventory.getLocationId());
            }
        }
        // 2.2 批量更新库存数量（加锁安全）
        if (CollUtil.isNotEmpty(inventories)) {
            inventoryMapper.updateBatch(inventories); // P0-7：全字段更新（含三维字段）
            // V205：变更定位行落库后派生库位占用/释放（ADR 259 D4）
            locationService.refreshOccupancyStatus(touchedLocationIds);
        }
        return resultMap;
    }

    /**
     * V205（ADR 259 D3）：P-deduct 跨位分配仅由「OUT 且 item.locationId == null」触发；
     * 显式 0 库位（含拣货执行链按行取出的未定位行）与 PLAIN 不参与分配——分配与否决定
     * 批次扣减（applyFefoAndDeductBatches）与库存行扣减是否同源，不可混淆。
     */
    private static boolean isDistributedOut(WmsInventoryChangeReqDTO.Item item) {
        return WmsInventoryChangeTypeEnum.OUT.equals(item.getChangeType())
                && item.getLocationId() == null;
    }

    /**
     * V205 P-deduct 跨位分配（ADR 259 D3）：未定位 OUT 在 (sku, warehouse) 的全部行上贪心扣减。
     *
     * <p>分配序：0 行（未定位）优先、其余行 location_id 升序、同位行 id 升序稳序；
     * 锁定/禁用库位的行直接跳过（锁定位货不动）。
     * 供需校验（quantity 与 available 双口径 all-or-nothing 预检）在逐行落地前完成，
     * 不足即整体拒绝（复用既有 {@code INVENTORY_QUANTITY_NOT_ENOUGH} 语义，拒绝路径零副作用）。
     *
     * <p>流水口径：该明细的 before/after = 参与行合计原量与差额
     *（{@code after = Σbefore - demand}；行级细粒度流水属后续项，不以牺牲一致性为代价）。
     *
     * @param touchedLocationIds 参与分配的定位行所属库位集合（占用派生素材，直接复用调用方集合）
     */
    private Tuple distributeOut(WmsInventoryChangeReqDTO.Item item,
                                List<WmsInventoryDO> inventories,
                                Set<Long> frozenLocationIds,
                                Set<Long> touchedLocationIds) {
        // 1. 圈定同 (sku, warehouse) 的候选行并合计供需
        BigDecimal demandQuantity = item.getQuantity();
        List<WmsInventoryDO> targets = new ArrayList<>();
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalAvailable = BigDecimal.ZERO;
        for (WmsInventoryDO row : inventories) {
            if (!ObjectUtil.equal(row.getSkuId(), item.getSkuId())
                    || !ObjectUtil.equal(row.getWarehouseId(), item.getWarehouseId())) {
                continue;
            }
            Long rowLocation = row.getLocationId() == null ? 0L : row.getLocationId();
            if (rowLocation != 0L && frozenLocationIds.contains(rowLocation)) {
                // 锁定/禁用库位的行不参与分配（锁定位货不动）
                continue;
            }
            targets.add(row);
            totalQuantity = addNonNull(totalQuantity, row.getQuantity());
            totalAvailable = addNonNull(totalAvailable, row.getAvailableQuantity());
        }
        // 2. 供需双口径合计校验（all-or-nothing，拒绝路径零副作用）
        if (totalQuantity.compareTo(demandQuantity) < 0) {
            throw buildInventoryQuantityNotEnoughException(item, totalQuantity);
        }
        if (totalAvailable.compareTo(demandQuantity) < 0) {
            throw buildInventoryQuantityNotEnoughException(item, totalAvailable);
        }
        // 3. 分配序：0 行（未定位）天然最小、其余 location_id 升序、id 升序稳序
        targets.sort((a, b) -> {
            long locationA = a.getLocationId() == null ? 0L : a.getLocationId();
            long locationB = b.getLocationId() == null ? 0L : b.getLocationId();
            int result = Long.compare(locationA, locationB);
            if (result != 0) {
                return result;
            }
            if (a.getId() != null && b.getId() != null) {
                return Long.compare(a.getId(), b.getId());
            }
            return 0;
        });
        // 4. 贪心逐行扣减（quantity 与 available 同步改写，三维口径与单行路径一致）
        BigDecimal remainingQuantity = demandQuantity;
        BigDecimal beforeSum = BigDecimal.ZERO;
        for (WmsInventoryDO row : targets) {
            if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal rowQuantity = row.getQuantity() == null ? BigDecimal.ZERO : row.getQuantity();
            BigDecimal rowAvailable = row.getAvailableQuantity() == null ? BigDecimal.ZERO : row.getAvailableQuantity();
            BigDecimal take = rowAvailable.min(remainingQuantity);
            if (take.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            row.setQuantity(rowQuantity.subtract(take));
            row.setAvailableQuantity(rowAvailable.subtract(take));
            beforeSum = addNonNull(beforeSum, rowQuantity);
            remainingQuantity = remainingQuantity.subtract(take);
            if (row.getLocationId() != null && row.getLocationId() != 0L) {
                touchedLocationIds.add(row.getLocationId());
            }
        }
        if (remainingQuantity.compareTo(BigDecimal.ZERO) > 0) {
            // 理论不可达：步骤 2 合计校验已保证 Σmin(available, remaining) ≥ demand；
            // 防御性兜底如实抛出（与既有缺货语义一致）
            throw buildInventoryQuantityNotEnoughException(item, totalAvailable);
        }
        return new Tuple(beforeSum, beforeSum.subtract(demandQuantity));
    }

    private static BigDecimal addNonNull(BigDecimal a, BigDecimal b) {
        if (a == null) {
            a = BigDecimal.ZERO;
        }
        if (b == null) {
            b = BigDecimal.ZERO;
        }
        return a.add(b);
    }

    private static BigDecimal subtractNonNull(BigDecimal a, BigDecimal b) {
        if (a == null) {
            a = BigDecimal.ZERO;
        }
        if (b == null) {
            b = BigDecimal.ZERO;
        }
        return a.subtract(b);
    }

    private List<WmsInventoryDO> getOrCreateInventoryList(List<WmsInventoryChangeReqDTO.Item> items) {
        // 1.1 先按库存维度在内存去重，避免同一批明细重复查询或重复补行
        //     V205：维度键升级为 (sku, warehouse, location) 三元，未定位 = 0 / null 归一（ADR 259 D1/D8）；
        //     null-OUT 属 P-deduct 分配，仅以 0 基准行注册维度，跨位行集由下方 2.1 段补齐
        List<WmsInventoryDO> inventoryDimensions = new ArrayList<>(items.size());
        for (WmsInventoryChangeReqDTO.Item item : items) {
            WmsInventoryDO key = new WmsInventoryDO().setSkuId(item.getSkuId())
                    .setWarehouseId(item.getWarehouseId())
                    .setLocationId(item.getLocationIdOrZero());
            if (findInventory(inventoryDimensions, key) == null) {
                inventoryDimensions.add(key);
            }
        }
        // 1.2 批量查询已存在的库存行（这里不加锁，后续统一按库存 ID 批量加锁）
        List<WmsInventoryDO> inventories = inventoryMapper.selectListByKeys(inventoryDimensions);

        // 2.1 V205：P-deduct 需求的 (sku, warehouse) 全部既有行（含定位行）并入行集，
        //     保证分配器能看到同仓全量行（一次批查，避免逐 key 重复 SQL）
        Set<String> processedOutPairs = new HashSet<>();
        for (WmsInventoryChangeReqDTO.Item item : items) {
            if (!isDistributedOut(item)) {
                continue;
            }
            String pair = item.getSkuId() + "|" + item.getWarehouseId();
            if (!processedOutPairs.add(pair)) {
                continue;
            }
            for (WmsInventoryDO row : inventoryMapper.selectListBySkuIdAndWarehouseId(
                    item.getSkuId(), item.getWarehouseId())) {
                if (findInventory(inventories, row) == null) {
                    inventories.add(row);
                }
            }
        }
        // 2.2 对比库存维度，找出数据库中还不存在的库存行
        List<WmsInventoryDO> missingInventories = new ArrayList<>(inventoryDimensions.size());
        for (WmsInventoryDO inventoryDimension : inventoryDimensions) {
            if (findInventory(inventories, inventoryDimension) == null) {
                missingInventories.add(inventoryDimension);
            }
        }
        // 2.3 如果库存行都已存在，直接返回待加锁库存列表
        if (CollUtil.isEmpty(missingInventories)) {
            return inventories;
        }
        // 2.4 对缺失库存行执行创建；并发冲突时，内部会按唯一索引回查已创建的库存行
        inventories.addAll(createMissingInventoryList(missingInventories));
        return inventories;
    }

    private List<WmsInventoryDO> createMissingInventoryList(List<WmsInventoryDO> missingInventories) {
        List<WmsInventoryDO> createdInventories = new ArrayList<>(missingInventories.size());
        for (WmsInventoryDO missingInventory : missingInventories) {
            // P0-7：新建库存行时初始化三维字段为 0；V205：三元键落库（未定位 = 0，null 归一，ADR 259 D1）
            WmsInventoryDO inventory = new WmsInventoryDO().setSkuId(missingInventory.getSkuId())
                    .setWarehouseId(missingInventory.getWarehouseId())
                    .setLocationId(missingInventory.getLocationId() == null ? 0L : missingInventory.getLocationId())
                    .setQuantity(BigDecimal.ZERO)
                    .setAvailableQuantity(BigDecimal.ZERO)
                    .setLockedQuantity(BigDecimal.ZERO)
                    .setFrozenQuantity(BigDecimal.ZERO);
            try {
                inventoryMapper.insert(inventory);
            } catch (DuplicateKeyException ex) {
                inventory = inventoryMapper.selectBySkuIdAndWarehouseIdAndLocationId(
                        missingInventory.getSkuId(), missingInventory.getWarehouseId(),
                        missingInventory.getLocationId() == null ? 0L : missingInventory.getLocationId());
                log.warn("[createMissingInventoryList][missingInventory({}) 插入库存行冲突，回查已有库存行]", missingInventory);
                if (inventory == null) {
                    throw ex;
                }
            }
            createdInventories.add(inventory);
        }
        return createdInventories;
    }

    private WmsInventoryHistoryDO buildInventoryHistory(WmsInventoryChangeReqDTO reqDTO,
                                                       WmsInventoryChangeReqDTO.Item item,
                                                       Tuple result) {
        return new WmsInventoryHistoryDO()
                .setWarehouseId(item.getWarehouseId()).setSkuId(item.getSkuId())
                .setQuantity(item.getQuantity()).setBeforeQuantity(result.get(0)).setAfterQuantity(result.get(1))
                .setPrice(item.getPrice()).setTotalPrice(item.getTotalPrice()).setRemark(item.getRemark())
                .setOrderId(reqDTO.getOrderId()).setOrderNo(reqDTO.getOrderNo()).setOrderType(reqDTO.getOrderType());
    }

    private WmsInventoryHistoryDO buildInventoryHistory(WmsInventoryCheckReqDTO reqDTO,
                                                       WmsInventoryCheckReqDTO.Item item,
                                                       BigDecimal beforeQuantity,
                                                       BigDecimal afterQuantity) {
        BigDecimal quantity = afterQuantity.subtract(beforeQuantity);
        return new WmsInventoryHistoryDO().setWarehouseId(item.getWarehouseId()).setSkuId(item.getSkuId())
                .setQuantity(quantity).setBeforeQuantity(beforeQuantity).setAfterQuantity(afterQuantity)
                .setPrice(item.getPrice()).setTotalPrice(MoneyUtils.priceMultiply(item.getPrice(), quantity))
                .setOrderId(reqDTO.getOrderId()).setOrderNo(reqDTO.getOrderNo()).setOrderType(reqDTO.getOrderType())
                .setRemark(item.getRemark());
    }

    private WmsInventoryDO getOrCreateCheckInventory(WmsInventoryCheckReqDTO.Item item) {
        if (item.getInventoryId() == null) {
            return createCheckInventory(item);
        }
        WmsInventoryDO inventory = inventoryMapper.selectByIdForUpdate(item.getInventoryId());
        if (inventory == null || !isSameInventory(inventory, item)
                || inventory.getQuantity().compareTo(item.getQuantity()) != 0) {
            throw exception(CHECK_ORDER_INVENTORY_CHANGED);
        }
        return inventory;
    }

    private WmsInventoryDO createCheckInventory(WmsInventoryCheckReqDTO.Item item) {
        WmsInventoryDO inventory = new WmsInventoryDO()
                .setSkuId(item.getSkuId()).setWarehouseId(item.getWarehouseId())
                .setLocationId(0L) // V205：无 inventoryId 兜底建行默认未定位（ADR 259 D1）
                .setQuantity(item.getCheckQuantity())
                .setAvailableQuantity(BigDecimal.ZERO)
                .setLockedQuantity(BigDecimal.ZERO)
                .setFrozenQuantity(BigDecimal.ZERO);
        try {
            inventoryMapper.insert(inventory);
        } catch (DuplicateKeyException ex) {
            throw exception(CHECK_ORDER_INVENTORY_CHANGED);
        }
        // 内存中重置为 0，让主循环按 0 -> checkQuantity 生成盘库流水
        inventory.setQuantity(BigDecimal.ZERO);
        return inventory;
    }

    private static WmsInventoryDO findInventory(List<WmsInventoryDO> inventories, WmsInventoryChangeReqDTO.Item item) {
        return CollUtil.findOne(inventories, inventory -> isSameInventory(inventory, item));
    }

    private static WmsInventoryDO findInventory(List<WmsInventoryDO> inventories, WmsInventoryDO key) {
        return CollUtil.findOne(inventories, inventory -> isSameInventory(inventory, key));
    }

    private static boolean isSameInventory(WmsInventoryDO inventory, WmsInventoryChangeReqDTO.Item item) {
        // V205：行身份 = (sku, warehouse, location) 三元键；item.locationId 为 null 时归一为 0（未定位）
        long rowLocation = inventory.getLocationId() == null ? 0L : inventory.getLocationId();
        return ObjectUtil.equal(inventory.getSkuId(), item.getSkuId())
                && ObjectUtil.equal(inventory.getWarehouseId(), item.getWarehouseId())
                && rowLocation == item.getLocationIdOrZero();
    }

    private static boolean isSameInventory(WmsInventoryDO inventory, WmsInventoryCheckReqDTO.Item item) {
        return ObjectUtil.equal(inventory.getSkuId(), item.getSkuId())
                && ObjectUtil.equal(inventory.getWarehouseId(), item.getWarehouseId());
    }

    private static boolean isSameInventory(WmsInventoryDO inventory, WmsInventoryDO key) {
        // V205：键行身份三元键（location 为 null 时按 0 未定位归一，ADR 259 D1/D8）
        long rowLocation = inventory.getLocationId() == null ? 0L : inventory.getLocationId();
        long keyLocation = key.getLocationId() == null ? 0L : key.getLocationId();
        return ObjectUtil.equal(inventory.getSkuId(), key.getSkuId())
                && ObjectUtil.equal(inventory.getWarehouseId(), key.getWarehouseId())
                && rowLocation == keyLocation;
    }

    private ServiceException buildInventoryQuantityNotEnoughException(WmsInventoryChangeReqDTO.Item item,
                                                                      BigDecimal beforeQuantity) {
        WmsItemSkuDO skuDO = itemSkuService.validateItemSkuExists(item.getSkuId());
        WmsItemDO itemDO = itemService.validateItemExists(skuDO.getItemId());
        return exception(INVENTORY_QUANTITY_NOT_ENOUGH, itemDO.getName(), skuDO.getName(),
                item.getWarehouseId(), beforeQuantity, item.getQuantity());
    }

}
