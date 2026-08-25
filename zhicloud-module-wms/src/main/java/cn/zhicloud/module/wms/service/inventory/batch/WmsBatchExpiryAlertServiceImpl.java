package cn.zhicloud.module.wms.service.inventory.batch;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.wms.controller.admin.inventory.batch.vo.WmsBatchExpiryAlertPageReqVO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryAlertDO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryBatchDO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryDO;
import cn.zhicloud.module.wms.dal.mysql.inventory.WmsInventoryAlertMapper;
import cn.zhicloud.module.wms.dal.mysql.inventory.WmsInventoryBatchMapper;
import cn.zhicloud.module.wms.dal.mysql.inventory.WmsInventoryMapper;
import cn.zhicloud.module.wms.enums.inventory.WmsInventoryAlertTypeEnum;
import cn.zhicloud.module.wms.enums.inventory.WmsInventoryBatchStatusEnum;
import cn.zhicloud.module.wms.service.inventory.alert.WmsInventoryAlertService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * WMS 批次效期预警 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
@Slf4j
public class WmsBatchExpiryAlertServiceImpl implements WmsBatchExpiryAlertService {

    /**
     * 临期预警天数
     */
    private static final int NEAR_EXPIRY_DAYS = 30;

    @Resource
    private WmsInventoryBatchMapper inventoryBatchMapper;
    @Resource
    private WmsInventoryMapper inventoryMapper;
    @Resource
    private WmsInventoryAlertMapper inventoryAlertMapper;
    @Resource
    private WmsInventoryAlertService inventoryAlertService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int scanExpiryAlerts() {
        LocalDate today = LocalDate.now();
        LocalDate nearExpiryDeadline = today.plusDays(NEAR_EXPIRY_DAYS);
        // 1. 查询所有设置了过期日期的批次
        List<WmsInventoryBatchDO> batches = inventoryBatchMapper.selectListWithExpiryDate();
        if (CollUtil.isEmpty(batches)) {
            log.info("[scanExpiryAlerts][无设置过期日期的批次]");
            return 0;
        }
        // 2. 批量查询关联的库存（拿到 warehouseId 和 skuId）
        List<Long> inventoryIds = batches.stream()
                .map(WmsInventoryBatchDO::getInventoryId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, WmsInventoryDO> inventoryMap = inventoryMapper.selectByIds(inventoryIds).stream()
                .collect(Collectors.toMap(WmsInventoryDO::getId, i -> i));
        // 3. 遍历批次，计算效期状态并生成预警
        List<WmsInventoryAlertDO> alerts = new ArrayList<>();
        for (WmsInventoryBatchDO batch : batches) {
            // 已冻结批次跳过
            if (WmsInventoryBatchStatusEnum.FROZEN.getStatus().equals(batch.getStatus())) {
                continue;
            }
            WmsInventoryDO inventory = inventoryMap.get(batch.getInventoryId());
            if (inventory == null) {
                continue;
            }
            String newStatus = calcBatchStatus(batch.getExpiryDate(), today, nearExpiryDeadline);
            // 状态变更才更新
            if (!newStatus.equals(batch.getStatus())) {
                WmsInventoryBatchDO updateObj = new WmsInventoryBatchDO();
                updateObj.setId(batch.getId());
                updateObj.setStatus(newStatus);
                inventoryBatchMapper.updateById(updateObj);
                log.info("[scanExpiryAlerts][批次 {} 状态变更：{} -> {}]", batch.getBatchNo(), batch.getStatus(), newStatus);
            }
            // 已过期或临期 → 生成预警
            if (WmsInventoryBatchStatusEnum.EXPIRED.getStatus().equals(newStatus)) {
                alerts.add(buildAlert(WmsInventoryAlertTypeEnum.EXPIRED, inventory.getWarehouseId(),
                        inventory.getSkuId(), batch, today, "批次已过期"));
            } else if (WmsInventoryBatchStatusEnum.NEAR_EXPIRY.getStatus().equals(newStatus)) {
                alerts.add(buildAlert(WmsInventoryAlertTypeEnum.NEAR_EXPIRY, inventory.getWarehouseId(),
                        inventory.getSkuId(), batch, today, "批次即将过期"));
            }
        }
        // 4. 批量创建预警
        if (CollUtil.isNotEmpty(alerts)) {
            inventoryAlertService.createInventoryAlertList(alerts);
        }
        log.info("[scanExpiryAlerts][扫描完成，共生成 {} 条效期预警]", alerts.size());
        return alerts.size();
    }

    @Override
    public PageResult<WmsInventoryAlertDO> getExpiryAlertPage(WmsBatchExpiryAlertPageReqVO pageReqVO) {
        return inventoryAlertMapper.selectPageByAlertTypes(pageReqVO,
                Arrays.asList(WmsInventoryAlertTypeEnum.NEAR_EXPIRY.getType(),
                        WmsInventoryAlertTypeEnum.EXPIRED.getType()));
    }

    // ==================== 内部方法 ====================

    /**
     * 计算批次状态
     *
     * @param expiryDate 过期日期
     * @param today 今天
     * @param nearExpiryDeadline 临期截止日期（today + 30）
     * @return 批次状态
     */
    private String calcBatchStatus(LocalDate expiryDate, LocalDate today, LocalDate nearExpiryDeadline) {
        if (expiryDate == null) {
            return WmsInventoryBatchStatusEnum.AVAILABLE.getStatus();
        }
        // 已过期：过期日期 < 今天
        if (expiryDate.isBefore(today)) {
            return WmsInventoryBatchStatusEnum.EXPIRED.getStatus();
        }
        // 临期：今天 <= 过期日期 <= today+30
        if (!expiryDate.isAfter(nearExpiryDeadline)) {
            return WmsInventoryBatchStatusEnum.NEAR_EXPIRY.getStatus();
        }
        // 正常
        return WmsInventoryBatchStatusEnum.AVAILABLE.getStatus();
    }

    private WmsInventoryAlertDO buildAlert(WmsInventoryAlertTypeEnum type, Long warehouseId, Long skuId,
                                            WmsInventoryBatchDO batch, LocalDate today, String remark) {
        BigDecimal thresholdValue = type == WmsInventoryAlertTypeEnum.EXPIRED
                ? BigDecimal.ZERO
                : new BigDecimal(NEAR_EXPIRY_DAYS);
        return WmsInventoryAlertDO.builder()
                .alertType(type.getType())
                .warehouseId(warehouseId)
                .productId(skuId)
                .batchNo(batch.getBatchNo())
                .currentQuantity(batch.getQuantity())
                .thresholdValue(thresholdValue)
                .alertTime(LocalDateTime.now())
                .status(0)
                .remark(remark)
                .build();
    }

}
