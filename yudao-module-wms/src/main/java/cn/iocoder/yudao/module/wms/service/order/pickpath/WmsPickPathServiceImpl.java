package cn.iocoder.yudao.module.wms.service.order.pickpath;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.module.wms.controller.admin.pickpath.vo.WmsPickPathLocationRespVO;
import cn.iocoder.yudao.module.wms.controller.admin.pickpath.vo.WmsPickPathRespVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.inventory.WmsInventoryDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.item.WmsItemDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.item.WmsItemSkuDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.warehouse.WmsWarehouseDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.shipment.WmsShipmentOrderDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.shipment.WmsShipmentOrderDetailDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.wave.WmsWaveOrderDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.wave.WmsWaveOrderDetailDO;
import cn.iocoder.yudao.module.wms.dal.mysql.inventory.WmsInventoryMapper;
import cn.iocoder.yudao.module.wms.dal.mysql.order.shipment.WmsShipmentOrderDetailMapper;
import cn.iocoder.yudao.module.wms.dal.mysql.order.shipment.WmsShipmentOrderMapper;
import cn.iocoder.yudao.module.wms.dal.mysql.order.wave.WmsWaveOrderDetailMapper;
import cn.iocoder.yudao.module.wms.dal.mysql.order.wave.WmsWaveOrderMapper;
import cn.iocoder.yudao.module.wms.service.md.item.WmsItemService;
import cn.iocoder.yudao.module.wms.service.md.item.WmsItemSkuService;
import cn.iocoder.yudao.module.wms.service.md.warehouse.WmsWarehouseService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.SHIPMENT_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.WAVE_ORDER_NOT_EXISTS;

/**
 * 拣货路径优化 Service 实现类
 *
 * <p>算法:最近邻(Nearest Neighbor)启发式算法
 * <p>原理:从起点出发,每次选择距离当前位置最近的未访问库位,直至全部访问完毕返回起点
 * <p>距离:曼哈顿距离(适用于矩形仓库布局,沿通道行走)
 * <p>复杂度:O(n²),n 为库位数量,适合中小规模拣货场景
 * <p>局限性:不保证全局最优,但解质量与计算效率平衡良好
 *
 * <p>说明:由于当前库存模型暂未独立维护库位(通道/货架/层/位),此处以库存行作为拣货节点,
 * 并基于库存 ID 生成伪坐标用于距离计算。后续接入真实库位表后,只需替换坐标来源即可复用本算法。
 * 对于大规模拣货场景(n > 数千)或需全局最优解时,建议后续扩展为遗传算法 / 蚁群算法 / TSP 精确求解。
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class WmsPickPathServiceImpl implements WmsPickPathService {

    /**
     * 伪坐标网格大小，用于将库存 ID 映射到二维平面
     */
    private static final int COORD_GRID = 100;

    @Resource
    private WmsShipmentOrderMapper shipmentOrderMapper;
    @Resource
    private WmsShipmentOrderDetailMapper shipmentOrderDetailMapper;
    @Resource
    private WmsWaveOrderMapper waveOrderMapper;
    @Resource
    private WmsWaveOrderDetailMapper waveOrderDetailMapper;
    @Resource
    private WmsInventoryMapper inventoryMapper;
    @Resource
    private WmsWarehouseService warehouseService;
    @Resource
    private WmsItemSkuService itemSkuService;
    @Resource
    private WmsItemService itemService;

    @Override
    @Transactional(rollbackFor = Exception.class, readOnly = true)
    public WmsPickPathRespVO optimizePickPath(Long shipmentOrderId) {
        // 1. 校验出库单存在
        WmsShipmentOrderDO order = shipmentOrderMapper.selectById(shipmentOrderId);
        if (order == null) {
            throw exception(SHIPMENT_ORDER_NOT_EXISTS);
        }
        // 2. 读取出库单明细
        List<WmsShipmentOrderDetailDO> details = shipmentOrderDetailMapper.selectListByOrderId(shipmentOrderId);
        if (CollUtil.isEmpty(details)) {
            return buildEmptyPath(order.getId(), order.getNo(), order.getWarehouseId());
        }
        // 3. 查询各 SKU 对应的库存行（库位）
        List<WmsPickPathLocationRespVO> locations = buildLocationsFromShipmentDetails(details);
        // 4. 执行最近邻排序
        applyNearestNeighbor(locations);
        // 5. 组装返回
        return buildPickPathRespVO(order.getId(), order.getNo(), order.getWarehouseId(), locations);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, readOnly = true)
    public WmsPickPathRespVO optimizeBatchPickPath(Long waveOrderId) {
        // 1. 校验波次单存在
        WmsWaveOrderDO waveOrder = waveOrderMapper.selectById(waveOrderId);
        if (waveOrder == null) {
            throw exception(WAVE_ORDER_NOT_EXISTS);
        }
        // 2. 读取波次单明细
        List<WmsWaveOrderDetailDO> details = waveOrderDetailMapper.selectListByWaveOrderId(waveOrderId);
        if (CollUtil.isEmpty(details)) {
            return buildEmptyPath(waveOrder.getId(), waveOrder.getNo(), waveOrder.getWarehouseId());
        }
        // 3. 查询各 SKU 对应的库存行（库位）
        List<WmsPickPathLocationRespVO> locations = buildLocationsFromWaveDetails(details, waveOrder.getWarehouseId());
        // 4. 执行最近邻排序
        applyNearestNeighbor(locations);
        // 5. 组装返回
        return buildPickPathRespVO(waveOrder.getId(), waveOrder.getNo(), waveOrder.getWarehouseId(), locations);
    }

    // ==================== 库位构建 ====================

    /**
     * 根据出库单明细构建库位节点列表
     */
    private List<WmsPickPathLocationRespVO> buildLocationsFromShipmentDetails(List<WmsShipmentOrderDetailDO> details) {
        List<WmsPickPathLocationRespVO> locations = new ArrayList<>();
        Set<Long> processedInventoryIds = new HashSet<>();
        for (WmsShipmentOrderDetailDO detail : details) {
            WmsInventoryDO inventory = inventoryMapper.selectBySkuIdAndWarehouseId(
                    detail.getSkuId(), detail.getWarehouseId());
            if (inventory == null) {
                log.warn("[buildLocationsFromShipmentDetails][出库明细 SKU={} 仓库={} 无库存行，跳过]",
                        detail.getSkuId(), detail.getWarehouseId());
                continue;
            }
            if (!processedInventoryIds.add(inventory.getId())) {
                continue;
            }
            locations.add(buildLocation(inventory, detail.getQuantity()));
        }
        return locations;
    }

    /**
     * 根据波次单明细构建库位节点列表
     */
    private List<WmsPickPathLocationRespVO> buildLocationsFromWaveDetails(List<WmsWaveOrderDetailDO> details,
                                                                          Long warehouseId) {
        List<WmsPickPathLocationRespVO> locations = new ArrayList<>();
        Set<Long> processedInventoryIds = new HashSet<>();
        for (WmsWaveOrderDetailDO detail : details) {
            WmsInventoryDO inventory = inventoryMapper.selectBySkuIdAndWarehouseId(
                    detail.getSkuId(), warehouseId);
            if (inventory == null) {
                log.warn("[buildLocationsFromWaveDetails][波次明细 SKU={} 仓库={} 无库存行，跳过]",
                        detail.getSkuId(), warehouseId);
                continue;
            }
            if (!processedInventoryIds.add(inventory.getId())) {
                continue;
            }
            locations.add(buildLocation(inventory, detail.getPickQuantity()));
        }
        return locations;
    }

    private WmsPickPathLocationRespVO buildLocation(WmsInventoryDO inventory, BigDecimal pickQuantity) {
        WmsPickPathLocationRespVO location = new WmsPickPathLocationRespVO();
        location.setInventoryId(inventory.getId());
        location.setWarehouseId(inventory.getWarehouseId());
        location.setSkuId(inventory.getSkuId());
        location.setPickQuantity(pickQuantity);
        location.setAvailableQuantity(inventory.getAvailableQuantity());
        // 基于库存 ID 生成伪坐标，模拟通道/货架位置
        long invId = inventory.getId() == null ? 0L : inventory.getId();
        location.setCoordX((int) (invId % COORD_GRID));
        location.setCoordY((int) ((invId / COORD_GRID) % COORD_GRID));
        return location;
    }

    // ==================== 最近邻算法 ====================

    /**
     * 最近邻（Nearest Neighbor）启发式算法生成访问顺序
     *
     * <p>从起点 (0, 0) 出发，每次选择距离当前节点最近且未访问的库位，
     * 直到所有库位都被访问，最后回到起点。
     *
     * <p>距离计算采用曼哈顿距离（|Δx| + |Δy|），适用于矩形仓库布局沿通道行走的场景。
     *
     * <p>算法复杂度：时间复杂度 O(n²)，空间复杂度 O(n)，n 为库位数量。
     * 适用场景：中小规模拣货路径优化，贪心近似解，不保证全局最优。
     */
    private void applyNearestNeighbor(List<WmsPickPathLocationRespVO> locations) {
        if (CollUtil.isEmpty(locations)) {
            return;
        }
        int n = locations.size();
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
                WmsPickPathLocationRespVO loc = locations.get(i);
                double dist = distance(currentX, currentY, loc.getCoordX(), loc.getCoordY());
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearestIdx = i;
                }
            }
            if (nearestIdx < 0) {
                break;
            }
            visited[nearestIdx] = true;
            WmsPickPathLocationRespVO chosen = locations.get(nearestIdx);
            chosen.setSequence(seq);
            currentX = chosen.getCoordX();
            currentY = chosen.getCoordY();
        }
        // 按 sequence 排序
        locations.sort((a, b) -> ObjectUtil.defaultIfNull(a.getSequence(), 0)
                .compareTo(ObjectUtil.defaultIfNull(b.getSequence(), 0)));
    }

    private static double distance(int x1, int y1, int x2, int y2) {
        // 曼哈顿距离：|Δx| + |Δy|，适用于矩形仓库布局（沿通道行走）
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    // ==================== 组装返回 ====================

    private WmsPickPathRespVO buildPickPathRespVO(Long orderId, String orderNo, Long warehouseId,
                                                  List<WmsPickPathLocationRespVO> locations) {
        WmsPickPathRespVO respVO = new WmsPickPathRespVO();
        respVO.setOrderId(orderId);
        respVO.setOrderNo(orderNo);
        respVO.setWarehouseId(warehouseId);
        respVO.setStrategy("NEAREST_NEIGHBOR");
        respVO.setLocationCount(locations.size());
        // 填充名称
        fillLocationNames(locations, warehouseId);
        // 汇总数量与路径长度
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalDistance = BigDecimal.ZERO;
        int prevX = 0;
        int prevY = 0;
        for (WmsPickPathLocationRespVO loc : locations) {
            if (loc.getPickQuantity() != null) {
                totalQuantity = totalQuantity.add(loc.getPickQuantity());
            }
            totalDistance = totalDistance.add(BigDecimal.valueOf(distance(prevX, prevY, loc.getCoordX(), loc.getCoordY())));
            prevX = loc.getCoordX();
            prevY = loc.getCoordY();
        }
        // 最后回到起点（打包台），补算返程距离
        totalDistance = totalDistance.add(BigDecimal.valueOf(distance(prevX, prevY, 0, 0)));
        respVO.setTotalQuantity(totalQuantity);
        respVO.setEstimatedDistance(totalDistance.setScale(2, RoundingMode.HALF_UP));
        respVO.setLocations(locations);
        return respVO;
    }

    private void fillLocationNames(List<WmsPickPathLocationRespVO> locations, Long defaultWarehouseId) {
        if (CollUtil.isEmpty(locations)) {
            return;
        }
        Set<Long> skuIds = new HashSet<>();
        Set<Long> warehouseIds = new HashSet<>();
        if (defaultWarehouseId != null) {
            warehouseIds.add(defaultWarehouseId);
        }
        for (WmsPickPathLocationRespVO loc : locations) {
            if (loc.getSkuId() != null) {
                skuIds.add(loc.getSkuId());
            }
            if (loc.getWarehouseId() != null) {
                warehouseIds.add(loc.getWarehouseId());
            }
        }
        Map<Long, WmsItemSkuDO> skuMap = itemSkuService.getItemSkuMap(skuIds);
        Map<Long, WmsItemDO> itemMap = itemService.getItemMap(
                convertSet(skuMap.values(), WmsItemSkuDO::getItemId));
        Map<Long, WmsWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(warehouseIds);
        for (WmsPickPathLocationRespVO loc : locations) {
            MapUtils.findAndThen(skuMap, loc.getSkuId(), sku -> {
                loc.setSkuCode(sku.getCode());
                loc.setSkuName(sku.getName());
                loc.setItemId(sku.getItemId());
                MapUtils.findAndThen(itemMap, sku.getItemId(), item -> {
                    loc.setItemCode(item.getCode());
                    loc.setItemName(item.getName());
                });
            });
            MapUtils.findAndThen(warehouseMap, loc.getWarehouseId(),
                    warehouse -> loc.setWarehouseName(warehouse.getName()));
        }
    }

    private WmsPickPathRespVO buildEmptyPath(Long orderId, String orderNo, Long warehouseId) {
        WmsPickPathRespVO respVO = new WmsPickPathRespVO();
        respVO.setOrderId(orderId);
        respVO.setOrderNo(orderNo);
        respVO.setWarehouseId(warehouseId);
        respVO.setStrategy("NEAREST_NEIGHBOR");
        respVO.setLocationCount(0);
        respVO.setTotalQuantity(BigDecimal.ZERO);
        respVO.setEstimatedDistance(BigDecimal.ZERO);
        respVO.setLocations(new ArrayList<>());
        if (warehouseId != null) {
            Map<Long, WmsWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(Collections.singleton(warehouseId));
            MapUtils.findAndThen(warehouseMap, warehouseId,
                    warehouse -> respVO.setWarehouseName(warehouse.getName()));
        }
        return respVO;
    }

}
