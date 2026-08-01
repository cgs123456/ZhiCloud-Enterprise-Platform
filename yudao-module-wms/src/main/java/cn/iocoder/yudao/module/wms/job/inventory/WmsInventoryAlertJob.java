package cn.iocoder.yudao.module.wms.job.inventory;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.module.wms.dal.dataobject.inventory.WmsInventoryAlertDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.inventory.WmsInventoryBatchDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.inventory.WmsInventoryDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.inventory.WmsSafetyStockConfigDO;
import cn.iocoder.yudao.module.wms.dal.mysql.inventory.WmsInventoryBatchMapper;
import cn.iocoder.yudao.module.wms.dal.mysql.inventory.WmsInventoryMapper;
import cn.iocoder.yudao.module.wms.enums.inventory.WmsInventoryAlertTypeEnum;
import cn.iocoder.yudao.module.wms.service.inventory.alert.WmsInventoryAlertService;
import cn.iocoder.yudao.module.wms.service.inventory.alert.WmsSafetyStockConfigService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * WMS 库存预警 Job
 *
 * <p>定时扫描库存生成预警：
 * <ol>
 *   <li>LOW_STOCK：库存 < 安全库存</li>
 *   <li>HIGH_STOCK：库存 > 最高库存</li>
 *   <li>NEAR_EXPIRY：批次过期日期距今 ≤ 30 天</li>
 *   <li>EXPIRED：批次已过期</li>
 *   <li>DEAD_STOCK：库存最近 90 天未变动（基于 update_time 简化判定）</li>
 * </ol>
 *
 * 建议调度：每小时执行一次（cron: 0 0 * * * ?）
 *
 * @author 芋道源码
 */
@Component
@Slf4j
public class WmsInventoryAlertJob implements JobHandler {

    /**
     * 临期预警天数
     */
    private static final int NEAR_EXPIRY_DAYS = 30;
    /**
     * 呆滞料阈值天数
     */
    private static final int DEAD_STOCK_DAYS = 90;

    @Resource
    private WmsSafetyStockConfigService safetyStockConfigService;
    @Resource
    private WmsInventoryAlertService inventoryAlertService;
    @Resource
    private WmsInventoryMapper inventoryMapper;
    @Resource
    private WmsInventoryBatchMapper inventoryBatchMapper;

    @Override
    public String execute(String param) {
        int total = 0;
        total += scanSafetyStock();
        total += scanExpiry();
        total += scanDeadStock();
        log.info("[execute][库存预警扫描完成，共生成 {} 条预警]", total);
        return String.format("库存预警扫描完成，共生成 %s 条预警", total);
    }

    /**
     * 1. 扫描安全库存/最高库存
     */
    private int scanSafetyStock() {
        List<WmsSafetyStockConfigDO> configs = safetyStockConfigService.getSafetyStockConfigList();
        if (CollUtil.isEmpty(configs)) {
            return 0;
        }
        List<WmsInventoryAlertDO> alerts = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (WmsSafetyStockConfigDO config : configs) {
            WmsInventoryDO inventory = inventoryMapper.selectBySkuIdAndWarehouseId(
                    config.getProductId(), config.getWarehouseId());
            if (inventory == null) {
                // 库存记录不存在，视为 0 库存，若安全库存 > 0 则触发低库存预警
                if (config.getSafetyStock() != null
                        && config.getSafetyStock().compareTo(BigDecimal.ZERO) > 0) {
                    alerts.add(buildAlert(WmsInventoryAlertTypeEnum.LOW_STOCK, config.getWarehouseId(),
                            config.getProductId(), null, BigDecimal.ZERO, config.getSafetyStock(), now,
                            "库存不存在，低于安全库存"));
                }
                continue;
            }
            BigDecimal quantity = inventory.getQuantity() == null ? BigDecimal.ZERO : inventory.getQuantity();
            // 1.1 低库存预警
            if (config.getSafetyStock() != null
                    && quantity.compareTo(config.getSafetyStock()) < 0) {
                alerts.add(buildAlert(WmsInventoryAlertTypeEnum.LOW_STOCK, config.getWarehouseId(),
                        config.getProductId(), null, quantity, config.getSafetyStock(), now,
                        "库存低于安全库存"));
            }
            // 1.2 高库存预警
            if (config.getMaxStock() != null
                    && quantity.compareTo(config.getMaxStock()) > 0) {
                alerts.add(buildAlert(WmsInventoryAlertTypeEnum.HIGH_STOCK, config.getWarehouseId(),
                        config.getProductId(), null, quantity, config.getMaxStock(), now,
                        "库存高于最高库存"));
            }
        }
        inventoryAlertService.createInventoryAlertList(alerts);
        return alerts.size();
    }

    /**
     * 2. 扫描批次保质期（临期 + 已过期）
     */
    private int scanExpiry() {
        LocalDate today = LocalDate.now();
        LocalDate nearExpiryDate = today.plusDays(NEAR_EXPIRY_DAYS);
        // 2.1 已过期批次
        List<WmsInventoryBatchDO> expiredBatches = inventoryBatchMapper.selectListExpiredBefore(today);
        // 2.2 临期批次
        List<WmsInventoryBatchDO> nearExpiryBatches = inventoryBatchMapper.selectListExpiringBetween(today, nearExpiryDate);
        if (CollUtil.isEmpty(expiredBatches) && CollUtil.isEmpty(nearExpiryBatches)) {
            return 0;
        }
        // 收集所有 inventoryId 以批量查询 inventory 拿到 warehouseId 和 skuId
        List<Long> inventoryIds = new ArrayList<>();
        if (expiredBatches != null) {
            expiredBatches.forEach(b -> inventoryIds.add(b.getInventoryId()));
        }
        if (nearExpiryBatches != null) {
            nearExpiryBatches.forEach(b -> inventoryIds.add(b.getInventoryId()));
        }
        Map<Long, WmsInventoryDO> inventoryMap = inventoryMapper.selectByIds(inventoryIds).stream()
                .collect(Collectors.toMap(WmsInventoryDO::getId, i -> i));
        LocalDateTime now = LocalDateTime.now();
        List<WmsInventoryAlertDO> alerts = new ArrayList<>();
        if (expiredBatches != null) {
            for (WmsInventoryBatchDO batch : expiredBatches) {
                WmsInventoryDO inventory = inventoryMap.get(batch.getInventoryId());
                if (inventory == null) {
                    continue;
                }
                alerts.add(buildAlert(WmsInventoryAlertTypeEnum.EXPIRED, inventory.getWarehouseId(),
                        inventory.getSkuId(), batch.getBatchNo(), batch.getQuantity(),
                        batch.getExpiryDate() == null ? null : new BigDecimal(0),
                        now, "批次已过期"));
            }
        }
        if (nearExpiryBatches != null) {
            for (WmsInventoryBatchDO batch : nearExpiryBatches) {
                WmsInventoryDO inventory = inventoryMap.get(batch.getInventoryId());
                if (inventory == null) {
                    continue;
                }
                alerts.add(buildAlert(WmsInventoryAlertTypeEnum.NEAR_EXPIRY, inventory.getWarehouseId(),
                        inventory.getSkuId(), batch.getBatchNo(), batch.getQuantity(),
                        batch.getExpiryDate() == null ? null : new BigDecimal(NEAR_EXPIRY_DAYS),
                        now, "批次即将过期"));
            }
        }
        inventoryAlertService.createInventoryAlertList(alerts);
        return alerts.size();
    }

    /**
     * 3. 扫描呆滞料（最近 90 天未变动的库存）
     *
     * <p>简化：基于 inventory.update_time 判定，无 last_out_time 字段。
     */
    private int scanDeadStock() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(DEAD_STOCK_DAYS);
        // 全量扫描库存（仅查询 update_time <= threshold 的记录）
        List<WmsInventoryDO> allInventories = inventoryMapper.selectList();
        if (CollUtil.isEmpty(allInventories)) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        List<WmsInventoryAlertDO> alerts = new ArrayList<>();
        for (WmsInventoryDO inventory : allInventories) {
            if (inventory.getUpdateTime() == null
                    || inventory.getUpdateTime().isAfter(threshold)) {
                continue;
            }
            BigDecimal quantity = inventory.getQuantity() == null ? BigDecimal.ZERO : inventory.getQuantity();
            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            alerts.add(buildAlert(WmsInventoryAlertTypeEnum.DEAD_STOCK, inventory.getWarehouseId(),
                    inventory.getSkuId(), null, quantity, new BigDecimal(DEAD_STOCK_DAYS), now,
                    "库存最近 90 天未变动"));
        }
        inventoryAlertService.createInventoryAlertList(alerts);
        return alerts.size();
    }

    private WmsInventoryAlertDO buildAlert(WmsInventoryAlertTypeEnum type, Long warehouseId, Long productId,
                                           String batchNo, BigDecimal currentQuantity, BigDecimal thresholdValue,
                                           LocalDateTime alertTime, String remark) {
        return WmsInventoryAlertDO.builder()
                .alertType(type.getType())
                .warehouseId(warehouseId)
                .productId(productId)
                .batchNo(batchNo)
                .currentQuantity(currentQuantity)
                .thresholdValue(thresholdValue)
                .alertTime(alertTime)
                .status(0)
                .remark(remark)
                .build();
    }

}