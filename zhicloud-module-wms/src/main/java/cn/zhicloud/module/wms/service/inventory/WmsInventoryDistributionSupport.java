package cn.zhicloud.module.wms.service.inventory;

import cn.hutool.core.util.ObjectUtil;
import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryDO;
import cn.zhicloud.module.wms.enums.inventory.WmsInventoryChangeTypeEnum;
import cn.zhicloud.module.wms.service.inventory.dto.WmsInventoryChangeReqDTO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * WMS 库存分配静态协作者（包内）
 *
 * <p>V211（ADR 269 D3/D8）：自 {@link WmsInventoryServiceImpl} 抽出 P-deduct 跨位分配与非空
 * 加减工具——抽件使主类回落结构红线（单文件 &lt;500 行），行为零变化；缺货异常由调用方注入
 * 工厂构造，保持本类零 service 依赖（第一个真实复用方出现时再议上移）。
 *
 * @author 智云
 */
final class WmsInventoryDistributionSupport {

    private WmsInventoryDistributionSupport() {
    }

    /**
     * V205（ADR 259 D3）：P-deduct 跨位分配仅由「OUT 且 item.locationId == null」触发；
     * 显式 0 库位（含拣货执行链按行取出的未定位行）与 PLAIN 不参与分配——分配与否决定
     * 批次扣减（applyFefoAndDeductBatches）与库存行扣减是否同源，不可混淆。
     */
    static boolean isDistributedOut(WmsInventoryChangeReqDTO.Item item) {
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
     * <p>流水口径（V211，ADR 269 D7）：返回参与行逐行记录（行库位 + 行级前后量 + 该行 take），
     * 聚合单条废除；行级细粒度流水落地。
     *
     * @param touchedLocationIds 参与分配的定位行所属库位集合（占用派生素材，直接复用调用方集合）
     * @param notEnoughFactory   缺货异常工厂（item × 现有量 → 异常），由调用方注入以保持本类零 service 依赖
     */
    static List<WmsInventoryRowChange> distributeOut(WmsInventoryChangeReqDTO.Item item,
                                                     List<WmsInventoryDO> inventories,
                                                     Set<Long> frozenLocationIds,
                                                     Set<Long> touchedLocationIds,
                                                     BiFunction<WmsInventoryChangeReqDTO.Item, BigDecimal, ServiceException> notEnoughFactory) {
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
            throw notEnoughFactory.apply(item, totalQuantity);
        }
        if (totalAvailable.compareTo(demandQuantity) < 0) {
            throw notEnoughFactory.apply(item, totalAvailable);
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
        // 4. 贪心逐行扣减（quantity 与 available 同步改写，三维口径与单行路径一致；逐行记录流水素材）
        BigDecimal remainingQuantity = demandQuantity;
        List<WmsInventoryRowChange> rowChanges = new ArrayList<>(targets.size());
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
            // V211：本行 take 与行级前后量（行库位随记录；未定位行归一 0）
            rowChanges.add(new WmsInventoryRowChange(row.getLocationId() == null ? 0L : row.getLocationId(),
                    rowQuantity, rowQuantity.subtract(take), take));
            remainingQuantity = remainingQuantity.subtract(take);
            if (row.getLocationId() != null && row.getLocationId() != 0L) {
                touchedLocationIds.add(row.getLocationId());
            }
        }
        if (remainingQuantity.compareTo(BigDecimal.ZERO) > 0) {
            // 理论不可达：步骤 2 合计校验已保证 Σmin(available, remaining) ≥ demand；
            // 防御性兜底如实抛出（与既有缺货语义一致）
            throw notEnoughFactory.apply(item, totalAvailable);
        }
        return rowChanges;
    }

    static BigDecimal addNonNull(BigDecimal a, BigDecimal b) {
        if (a == null) {
            a = BigDecimal.ZERO;
        }
        if (b == null) {
            b = BigDecimal.ZERO;
        }
        return a.add(b);
    }

    static BigDecimal subtractNonNull(BigDecimal a, BigDecimal b) {
        if (a == null) {
            a = BigDecimal.ZERO;
        }
        if (b == null) {
            b = BigDecimal.ZERO;
        }
        return a.subtract(b);
    }

}
