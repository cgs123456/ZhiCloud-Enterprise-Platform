package cn.iocoder.yudao.module.wms.service.abc;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.module.wms.controller.admin.abc.vo.WmsAbcAnalysisReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.abc.vo.WmsAbcReportItemRespVO;
import cn.iocoder.yudao.module.wms.controller.admin.abc.vo.WmsAbcReportRespVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.inventory.WmsInventoryHistoryDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.item.WmsItemDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.item.WmsItemSkuDO;
import cn.iocoder.yudao.module.wms.dal.mysql.inventory.WmsInventoryHistoryMapper;
import cn.iocoder.yudao.module.wms.dal.mysql.md.item.WmsItemMapper;
import cn.iocoder.yudao.module.wms.enums.order.WmsOrderTypeEnum;
import cn.iocoder.yudao.module.wms.service.md.item.WmsItemService;
import cn.iocoder.yudao.module.wms.service.md.item.WmsItemSkuService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * WMS ABC 分类分析 Service 实现类
 *
 * <p>ABC 分类法（帕累托分析法）：
 * <ul>
 *   <li>A 类：累计出库量 0%-80% 的 SKU（高频，重点管理）</li>
 *   <li>B 类：累计出库量 80%-95% 的 SKU（中频，常规管理）</li>
 *   <li>C 类：累计出库量 95%-100% 的 SKU（低频，简化管理）</li>
 * </ul>
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class WmsAbcAnalysisServiceImpl implements WmsAbcAnalysisService {

    /**
     * A 类累计占比上限
     */
    private static final BigDecimal CLASS_A_THRESHOLD = new BigDecimal("0.80");
    /**
     * B 类累计占比上限
     */
    private static final BigDecimal CLASS_B_THRESHOLD = new BigDecimal("0.95");

    @Resource
    private WmsInventoryHistoryMapper inventoryHistoryMapper;
    @Resource
    private WmsItemMapper itemMapper;
    @Resource
    private WmsItemSkuService itemSkuService;
    @Resource
    private WmsItemService itemService;

    /**
     * 最近一次分析结果缓存（单机内存，避免频繁重算）
     */
    private volatile WmsAbcReportRespVO lastReport;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsAbcReportRespVO analyzeAbcClassification(WmsAbcAnalysisReqVO reqVO) {
        // 1. 查询时间窗口内的出库流水
        List<WmsInventoryHistoryDO> histories = inventoryHistoryMapper.selectListByOrderTypeAndCreateTimeBetween(
                WmsOrderTypeEnum.SHIPMENT.getType(), reqVO.getStartDate(), reqVO.getEndDate());
        // 2. 按 SKU 聚合出库频次、出库量、出库金额
        Map<Long, SkuAggregate> skuAggregateMap = aggregateBySku(histories);
        if (skuAggregateMap.isEmpty()) {
            lastReport = buildEmptyReport(reqVO.getStartDate(), reqVO.getEndDate());
            return lastReport;
        }
        // 3. 通过 SKU 映射到商品，按商品维度聚合
        Set<Long> skuIds = skuAggregateMap.keySet();
        Map<Long, WmsItemSkuDO> skuMap = itemSkuService.getItemSkuMap(skuIds);
        Map<Long, ItemAggregate> itemAggregateMap = aggregateByItem(skuAggregateMap, skuMap);
        // 4. 按出库量降序排序，计算累计占比
        List<ItemAggregate> sortedAggregates = itemAggregateMap.values().stream()
                .sorted((a, b) -> b.outQuantity.compareTo(a.outQuantity))
                .collect(Collectors.toList());
        BigDecimal totalOutQuantity = sortedAggregates.stream()
                .map(a -> a.outQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOutAmount = sortedAggregates.stream()
                .map(a -> a.outAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        fillCumulativePercent(sortedAggregates, totalOutQuantity);
        // 5. ABC 分类
        classifyAbc(sortedAggregates);
        // 6. 更新商品 ABC 分类字段
        updateItemAbcClassification(sortedAggregates);
        // 7. 构建报告
        lastReport = buildReport(reqVO.getStartDate(), reqVO.getEndDate(), sortedAggregates,
                totalOutQuantity, totalOutAmount);
        return lastReport;
    }

    @Override
    public WmsAbcReportRespVO getAbcReport() {
        if (lastReport != null) {
            return lastReport;
        }
        // 没有执行过分析时，返回空报告
        WmsAbcReportRespVO report = new WmsAbcReportRespVO();
        report.setAnalysisTime(LocalDateTime.now());
        report.setItems(new ArrayList<>());
        report.setTotalSkuCount(0);
        report.setTotalOutQuantity(BigDecimal.ZERO);
        report.setTotalOutAmount(BigDecimal.ZERO);
        report.setClassACount(0);
        report.setClassBCount(0);
        report.setClassCCount(0);
        report.setClassAPercent(BigDecimal.ZERO);
        report.setClassBPercent(BigDecimal.ZERO);
        report.setClassCPercent(BigDecimal.ZERO);
        return report;
    }

    // ==================== 聚合逻辑 ====================

    private Map<Long, SkuAggregate> aggregateBySku(List<WmsInventoryHistoryDO> histories) {
        Map<Long, SkuAggregate> map = new HashMap<>();
        if (CollUtil.isEmpty(histories)) {
            return map;
        }
        for (WmsInventoryHistoryDO history : histories) {
            Long skuId = history.getSkuId();
            if (skuId == null) {
                continue;
            }
            SkuAggregate agg = map.computeIfAbsent(skuId, k -> new SkuAggregate());
            agg.outCount++;
            // 出库流水 quantity 为负数，取绝对值
            BigDecimal qty = history.getQuantity() == null ? BigDecimal.ZERO : history.getQuantity().abs();
            agg.outQuantity = agg.outQuantity.add(qty);
            BigDecimal amt = history.getTotalPrice() == null ? BigDecimal.ZERO : history.getTotalPrice().abs();
            agg.outAmount = agg.outAmount.add(amt);
        }
        return map;
    }

    private Map<Long, ItemAggregate> aggregateByItem(Map<Long, SkuAggregate> skuAggregateMap,
                                                      Map<Long, WmsItemSkuDO> skuMap) {
        Map<Long, ItemAggregate> itemMap = new HashMap<>();
        for (Map.Entry<Long, SkuAggregate> entry : skuAggregateMap.entrySet()) {
            Long skuId = entry.getKey();
            SkuAggregate skuAgg = entry.getValue();
            WmsItemSkuDO sku = skuMap.get(skuId);
            if (sku == null || sku.getItemId() == null) {
                log.warn("[aggregateByItem][SKU={} 找不到商品 SKU，跳过]", skuId);
                continue;
            }
            Long itemId = sku.getItemId();
            ItemAggregate itemAgg = itemMap.computeIfAbsent(itemId, k -> new ItemAggregate());
            itemAgg.itemId = itemId;
            itemAgg.outCount += skuAgg.outCount;
            itemAgg.outQuantity = itemAgg.outQuantity.add(skuAgg.outQuantity);
            itemAgg.outAmount = itemAgg.outAmount.add(skuAgg.outAmount);
        }
        return itemMap;
    }

    private void fillCumulativePercent(List<ItemAggregate> sortedAggregates, BigDecimal totalOutQuantity) {
        BigDecimal cumulative = BigDecimal.ZERO;
        for (ItemAggregate agg : sortedAggregates) {
            BigDecimal percent = totalOutQuantity.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : agg.outQuantity.divide(totalOutQuantity, 6, RoundingMode.HALF_UP);
            agg.quantityPercent = percent;
            cumulative = cumulative.add(percent);
            agg.cumulativePercent = cumulative;
        }
    }

    private void classifyAbc(List<ItemAggregate> sortedAggregates) {
        for (ItemAggregate agg : sortedAggregates) {
            if (agg.cumulativePercent.compareTo(CLASS_A_THRESHOLD) <= 0) {
                agg.abcClassification = "A";
            } else if (agg.cumulativePercent.compareTo(CLASS_B_THRESHOLD) <= 0) {
                agg.abcClassification = "B";
            } else {
                agg.abcClassification = "C";
            }
        }
    }

    private void updateItemAbcClassification(List<ItemAggregate> sortedAggregates) {
        if (CollUtil.isEmpty(sortedAggregates)) {
            return;
        }
        Set<Long> itemIds = new HashSet<>();
        List<WmsItemDO> updates = new ArrayList<>(sortedAggregates.size());
        for (ItemAggregate agg : sortedAggregates) {
            itemIds.add(agg.itemId);
            WmsItemDO update = new WmsItemDO();
            update.setId(agg.itemId);
            update.setAbcClassification(agg.abcClassification);
            updates.add(update);
        }
        // 更新已分类的商品
        itemMapper.updateBatch(updates);
        // 将未出现在出库流水中的商品归为 C 类
        resetUnclassifiedItemsAbc(itemIds);
    }

    private void resetUnclassifiedItemsAbc(Set<Long> classifiedItemIds) {
        // 查询所有商品，将未分类的设置为 C
        List<WmsItemDO> allItems = itemMapper.selectList();
        if (CollUtil.isEmpty(allItems)) {
            return;
        }
        List<WmsItemDO> toReset = new ArrayList<>();
        for (WmsItemDO item : allItems) {
            if (!classifiedItemIds.contains(item.getId())) {
                WmsItemDO update = new WmsItemDO();
                update.setId(item.getId());
                update.setAbcClassification("C");
                toReset.add(update);
            }
        }
        if (CollUtil.isNotEmpty(toReset)) {
            itemMapper.updateBatch(toReset);
        }
    }

    // ==================== 报告构建 ====================

    private WmsAbcReportRespVO buildReport(LocalDateTime startDate, LocalDateTime endDate,
                                           List<ItemAggregate> sortedAggregates,
                                           BigDecimal totalOutQuantity, BigDecimal totalOutAmount) {
        WmsAbcReportRespVO report = new WmsAbcReportRespVO();
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setAnalysisTime(LocalDateTime.now());
        report.setTotalSkuCount(sortedAggregates.size());
        report.setTotalOutQuantity(totalOutQuantity);
        report.setTotalOutAmount(totalOutAmount);
        // 统计 A/B/C 数量与占比
        int classA = 0, classB = 0, classC = 0;
        BigDecimal classAQty = BigDecimal.ZERO, classBQty = BigDecimal.ZERO, classCQty = BigDecimal.ZERO;
        for (ItemAggregate agg : sortedAggregates) {
            if ("A".equals(agg.abcClassification)) {
                classA++;
                classAQty = classAQty.add(agg.outQuantity);
            } else if ("B".equals(agg.abcClassification)) {
                classB++;
                classBQty = classBQty.add(agg.outQuantity);
            } else {
                classC++;
                classCQty = classCQty.add(agg.outQuantity);
            }
        }
        report.setClassACount(classA);
        report.setClassBCount(classB);
        report.setClassCCount(classC);
        report.setClassAPercent(calcPercent(classAQty, totalOutQuantity));
        report.setClassBPercent(calcPercent(classBQty, totalOutQuantity));
        report.setClassCPercent(calcPercent(classCQty, totalOutQuantity));
        // 构建明细
        Map<Long, WmsItemDO> itemMap = itemService.getItemMap(
                sortedAggregates.stream().map(a -> a.itemId).collect(Collectors.toSet()));
        List<WmsAbcReportItemRespVO> items = new ArrayList<>(sortedAggregates.size());
        for (ItemAggregate agg : sortedAggregates) {
            WmsAbcReportItemRespVO item = new WmsAbcReportItemRespVO();
            item.setItemId(agg.itemId);
            MapUtils.findAndThen(itemMap, agg.itemId, doItem -> {
                item.setItemCode(doItem.getCode());
                item.setItemName(doItem.getName());
                item.setUnit(doItem.getUnit());
            });
            item.setOutCount(agg.outCount);
            item.setOutQuantity(agg.outQuantity);
            item.setOutAmount(agg.outAmount);
            item.setQuantityPercent(agg.quantityPercent.multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP));
            item.setCumulativePercent(agg.cumulativePercent.multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP));
            item.setAbcClassification(agg.abcClassification);
            item.setAnalysisTime(report.getAnalysisTime());
            items.add(item);
        }
        report.setItems(items);
        return report;
    }

    private BigDecimal calcPercent(BigDecimal qty, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return qty.divide(total, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private WmsAbcReportRespVO buildEmptyReport(LocalDateTime startDate, LocalDateTime endDate) {
        WmsAbcReportRespVO report = new WmsAbcReportRespVO();
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setAnalysisTime(LocalDateTime.now());
        report.setTotalSkuCount(0);
        report.setTotalOutQuantity(BigDecimal.ZERO);
        report.setTotalOutAmount(BigDecimal.ZERO);
        report.setClassACount(0);
        report.setClassBCount(0);
        report.setClassCCount(0);
        report.setClassAPercent(BigDecimal.ZERO);
        report.setClassBPercent(BigDecimal.ZERO);
        report.setClassCPercent(BigDecimal.ZERO);
        report.setItems(Collections.emptyList());
        return report;
    }

    // ==================== 内部聚合对象 ====================

    private static class SkuAggregate {
        long outCount = 0L;
        BigDecimal outQuantity = BigDecimal.ZERO;
        BigDecimal outAmount = BigDecimal.ZERO;
    }

    private static class ItemAggregate {
        Long itemId;
        long outCount = 0L;
        BigDecimal outQuantity = BigDecimal.ZERO;
        BigDecimal outAmount = BigDecimal.ZERO;
        BigDecimal quantityPercent = BigDecimal.ZERO;
        BigDecimal cumulativePercent = BigDecimal.ZERO;
        String abcClassification;
    }

}
