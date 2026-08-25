package cn.zhicloud.module.wms.service.order.pickstrategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryDO;
import cn.zhicloud.module.wms.dal.dataobject.md.item.WmsItemDO;
import cn.zhicloud.module.wms.dal.dataobject.md.item.WmsItemSkuDO;
import cn.zhicloud.module.wms.dal.dataobject.order.pickstrategy.WmsPickTaskDO;
import cn.zhicloud.module.wms.dal.dataobject.order.shipment.WmsShipmentOrderDO;
import cn.zhicloud.module.wms.dal.dataobject.order.shipment.WmsShipmentOrderDetailDO;
import cn.zhicloud.module.wms.dal.mysql.inventory.WmsInventoryMapper;
import cn.zhicloud.module.wms.dal.mysql.order.pickstrategy.WmsPickTaskMapper;
import cn.zhicloud.module.wms.dal.mysql.order.shipment.WmsShipmentOrderDetailMapper;
import cn.zhicloud.module.wms.dal.mysql.order.shipment.WmsShipmentOrderMapper;
import cn.zhicloud.module.wms.enums.order.WmsOrderStatusEnum;
import cn.zhicloud.module.wms.service.md.item.WmsItemService;
import cn.zhicloud.module.wms.service.md.item.WmsItemSkuService;
import cn.zhicloud.module.wms.service.order.pickstrategy.dto.WmsPickTaskDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.PICK_TASK_GENERATE_NO_DETAIL;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.PICK_TASK_SHIPMENT_NOT_PICKABLE;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.SHIPMENT_ORDER_NOT_EXISTS;

/**
 * WMS 拣选策略引擎 Service 实现类
 *
 * <p>策略选择基于出库单类型、SKU 数量、库位分布：
 * <ul>
 *   <li>SKU 数量 ≤ {@link #SINGLE_PICK_SKU_THRESHOLD} 且库位数 ≤ {@link #SINGLE_PICK_LOC_THRESHOLD}：按单拣</li>
 *   <li>否则：批量拣；当库位数 > {@link #SORT_WHILE_PICK_LOC_THRESHOLD} 时采用二次分拣（边拣边分不可行）</li>
 * </ul>
 *
 * <p>路径优化：基于库位 ABC 分类（按库存 ID 伪坐标）+ 最近邻就近排序。
 *
 * @author 智云
 */
@Service
@Validated
@Slf4j
public class WmsPickStrategyServiceImpl implements WmsPickStrategyService {

    /**
     * 按单拣 SKU 数量阈值
     */
    private static final int SINGLE_PICK_SKU_THRESHOLD = 5;
    /**
     * 按单拣库位数量阈值
     */
    private static final int SINGLE_PICK_LOC_THRESHOLD = 5;
    /**
     * 二次分拣库位数量阈值（超过则边拣边分不可行）
     */
    private static final int SORT_WHILE_PICK_LOC_THRESHOLD = 10;
    /**
     * 伪坐标网格大小
     */
    private static final int COORD_GRID = 100;
    /**
     * 拣货任务状态：待拣
     */
    private static final int STATUS_PENDING = 10;

    @Resource
    private WmsShipmentOrderMapper shipmentOrderMapper;
    @Resource
    private WmsShipmentOrderDetailMapper shipmentOrderDetailMapper;
    @Resource
    private WmsInventoryMapper inventoryMapper;
    @Resource
    private WmsPickTaskMapper pickTaskMapper;
    @Resource
    private WmsItemSkuService itemSkuService;
    @Resource
    private WmsItemService itemService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> generatePickTasks(Long shipmentOrderId) {
        // 1. 校验出库单存在且可拣货
        WmsShipmentOrderDO order = shipmentOrderMapper.selectById(shipmentOrderId);
        if (order == null) {
            throw exception(SHIPMENT_ORDER_NOT_EXISTS);
        }
        if (!isPickable(order.getStatus())) {
            throw exception(PICK_TASK_SHIPMENT_NOT_PICKABLE);
        }
        // 2. 读取明细
        List<WmsShipmentOrderDetailDO> details = shipmentOrderDetailMapper.selectListByOrderId(shipmentOrderId);
        if (CollUtil.isEmpty(details)) {
            throw exception(PICK_TASK_GENERATE_NO_DETAIL);
        }
        // 3. 若已生成过拣货任务，直接返回（幂等）
        Long existCount = pickTaskMapper.selectCountByShipmentOrderId(shipmentOrderId);
        if (existCount != null && existCount > 0) {
            return pickTaskMapper.selectListByShipmentOrderId(shipmentOrderId).stream()
                    .map(WmsPickTaskDO::getId).collect(Collectors.toList());
        }
        // 4. 策略选择
        PickStrategy strategy = chooseStrategy(details, order.getWarehouseId());
        log.info("[generatePickTasks][出库单 {} 采用拣选策略：{}]", order.getNo(), strategy);
        // 5. 构建拣货任务 DTO（含库位与伪坐标）
        List<WmsPickTaskDTO> taskDTOs = buildPickTaskDTOs(details, order);
        // 6. 路径优化（就近排序）
        taskDTOs = optimizePickPath(taskDTOs);
        // 7. 持久化拣货任务
        List<WmsPickTaskDO> taskDOs = new ArrayList<>();
        for (WmsPickTaskDTO dto : taskDTOs) {
            WmsPickTaskDO taskDO = WmsPickTaskDO.builder()
                    .taskNo(dto.getTaskNo())
                    .shipmentOrderId(shipmentOrderId)
                    .skuId(dto.getSkuId())
                    .productName(dto.getProductName())
                    .quantity(dto.getQuantity())
                    .pickedQuantity(BigDecimal.ZERO)
                    .locationId(dto.getLocationId())
                    .pickSequence(dto.getPickSequence())
                    .status(STATUS_PENDING)
                    .remark(strategy.describe())
                    .build();
            pickTaskMapper.insert(taskDO);
            taskDOs.add(taskDO);
        }
        // 8. 出库单状态流转到拣货中
        WmsShipmentOrderDO updateObj = new WmsShipmentOrderDO();
        updateObj.setId(shipmentOrderId);
        updateObj.setStatus(WmsOrderStatusEnum.PICKING.getStatus());
        shipmentOrderMapper.updateById(updateObj);
        return taskDOs.stream().map(WmsPickTaskDO::getId).collect(Collectors.toList());
    }

    @Override
    public List<WmsPickTaskDTO> optimizePickPath(List<WmsPickTaskDTO> tasks) {
        if (CollUtil.isEmpty(tasks)) {
            return tasks;
        }
        // 1. 为每个任务填充伪坐标（基于库位 ID，ABC 分类就近排序）
        for (WmsPickTaskDTO task : tasks) {
            fillCoord(task);
        }
        // 2. 最近邻就近排序：从起点 (0,0) 出发，每次选择最近未访问节点
        int n = tasks.size();
        boolean[] visited = new boolean[n];
        int currentX = 0;
        int currentY = 0;
        for (int seq = 1; seq <= n; seq++) {
            int nearestIdx = -1;
            double nearestDist = Double.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (visited[i]) {
                    continue;
                }
                WmsPickTaskDTO t = tasks.get(i);
                double dist = Math.abs(currentX - t.getCoordX()) + Math.abs(currentY - t.getCoordY());
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearestIdx = i;
                }
            }
            if (nearestIdx < 0) {
                break;
            }
            visited[nearestIdx] = true;
            WmsPickTaskDTO chosen = tasks.get(nearestIdx);
            chosen.setPickSequence(seq);
            currentX = chosen.getCoordX();
            currentY = chosen.getCoordY();
        }
        // 3. 按 pickSequence 升序返回
        tasks.sort((a, b) -> ObjectUtil.defaultIfNull(a.getPickSequence(), 0)
                .compareTo(ObjectUtil.defaultIfNull(b.getPickSequence(), 0)));
        return tasks;
    }

    // ==================== 策略选择 ====================

    /**
     * 根据出库单类型、SKU 数量、库位分布选择拣选策略
     */
    private PickStrategy chooseStrategy(List<WmsShipmentOrderDetailDO> details, Long warehouseId) {
        int skuCount = details.size();
        // 统计库位分布（基于库存行）
        Set<Long> locationKeys = new HashSet<>();
        for (WmsShipmentOrderDetailDO detail : details) {
            WmsInventoryDO inventory = inventoryMapper.selectBySkuIdAndWarehouseId(
                    detail.getSkuId(), warehouseId);
            if (inventory != null) {
                locationKeys.add(inventory.getId());
            }
        }
        int locationCount = locationKeys.size();
        boolean wavePick = skuCount > SINGLE_PICK_SKU_THRESHOLD || locationCount > SINGLE_PICK_LOC_THRESHOLD;
        boolean sortWhilePick = locationCount <= SORT_WHILE_PICK_LOC_THRESHOLD;
        return new PickStrategy(wavePick, sortWhilePick, skuCount, locationCount);
    }

    private boolean isPickable(Integer status) {
        if (status == null) {
            return false;
        }
        // 草稿或拣货中状态可生成拣货任务
        return ObjectUtil.equal(status, WmsOrderStatusEnum.PREPARE.getStatus())
                || ObjectUtil.equal(status, WmsOrderStatusEnum.PICKING.getStatus());
    }

    // ==================== 任务构建 ====================

    private List<WmsPickTaskDTO> buildPickTaskDTOs(List<WmsShipmentOrderDetailDO> details,
                                                    WmsShipmentOrderDO order) {
        // 批量查询 SKU 与商品名称
        Set<Long> skuIds = details.stream().map(WmsShipmentOrderDetailDO::getSkuId)
                .collect(Collectors.toSet());
        Map<Long, WmsItemSkuDO> skuMap = itemSkuService.getItemSkuMap(skuIds);
        Set<Long> itemIds = skuMap.values().stream().map(WmsItemSkuDO::getItemId)
                .collect(Collectors.toSet());
        Map<Long, WmsItemDO> itemMap = itemService.getItemMap(itemIds);

        List<WmsPickTaskDTO> taskDTOs = new ArrayList<>();
        int seq = 1;
        for (WmsShipmentOrderDetailDO detail : details) {
            WmsPickTaskDTO dto = new WmsPickTaskDTO();
            dto.setTaskNo("PT-" + order.getNo() + "-" + String.format("%03d", seq++));
            dto.setShipmentOrderId(order.getId());
            dto.setSkuId(detail.getSkuId());
            dto.setQuantity(detail.getQuantity());
            // 填充商品名称
            WmsItemSkuDO sku = skuMap.get(detail.getSkuId());
            if (sku != null) {
                WmsItemDO item = itemMap.get(sku.getItemId());
                dto.setProductName(item != null ? item.getName() : sku.getName());
            }
            // 查询库存确定库位（库存模型无独立库位字段，使用库存行作为拣货节点）
            WmsInventoryDO inventory = inventoryMapper.selectBySkuIdAndWarehouseId(
                    detail.getSkuId(), order.getWarehouseId());
            if (inventory != null) {
                // 库位编号暂用库存编号作为占位（实际接入库位表后替换）
                dto.setLocationId(inventory.getId());
            }
            taskDTOs.add(dto);
        }
        return taskDTOs;
    }

    private void fillCoord(WmsPickTaskDTO task) {
        long key = task.getLocationId() == null ? 0L : task.getLocationId();
        task.setCoordX((int) (key % COORD_GRID));
        task.setCoordY((int) ((key / COORD_GRID) % COORD_GRID));
    }

    /**
     * 拣选策略描述
     */
    private static class PickStrategy {
        final boolean wavePick;
        final boolean sortWhilePick;
        final int skuCount;
        final int locationCount;

        PickStrategy(boolean wavePick, boolean sortWhilePick, int skuCount, int locationCount) {
            this.wavePick = wavePick;
            this.sortWhilePick = sortWhilePick;
            this.skuCount = skuCount;
            this.locationCount = locationCount;
        }

        String describe() {
            String mode = wavePick ? "批量拣" : "按单拣";
            String sort = sortWhilePick ? "边拣边分" : "二次分拣";
            return String.format("%s/%s（SKU=%d, 库位=%d）", mode, sort, skuCount, locationCount);
        }

        @Override
        public String toString() {
            return describe();
        }
    }

}
