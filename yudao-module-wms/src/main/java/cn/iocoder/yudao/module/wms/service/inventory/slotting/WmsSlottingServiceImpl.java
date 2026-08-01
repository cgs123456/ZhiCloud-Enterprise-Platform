package cn.iocoder.yudao.module.wms.service.inventory.slotting;

import cn.iocoder.yudao.module.wms.dal.dataobject.inventory.WmsInventoryBatchDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.inventory.WmsInventoryDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.warehouse.WmsWarehouseDO;
import cn.iocoder.yudao.module.wms.dal.mysql.inventory.WmsInventoryBatchMapper;
import cn.iocoder.yudao.module.wms.dal.mysql.inventory.WmsInventoryMapper;
import cn.iocoder.yudao.module.wms.dal.mysql.md.warehouse.WmsWarehouseMapper;
import cn.iocoder.yudao.module.wms.enums.inventory.WmsInventoryBatchStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.INVENTORY_BATCH_PUTAWAY_EXPIRED;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.SLOTTING_BATCH_MISMATCH;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.SLOTTING_NO_SUITABLE_WAREHOUSE;

/**
 * WMS 上架 Slotting Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class WmsSlottingServiceImpl implements WmsSlottingService {

    @Resource
    private WmsInventoryMapper inventoryMapper;
    @Resource
    private WmsInventoryBatchMapper inventoryBatchMapper;
    @Resource
    private WmsWarehouseMapper warehouseMapper;

    @Override
    public WmsSlottingRecommendationRespVO recommendPutaway(Long warehouseId, Long skuId, String batchNo,
                                                            BigDecimal quantity, LocalDate productionDate,
                                                            LocalDate expiryDate) {
        // 1. 校验仓库存在
        WmsWarehouseDO warehouse = warehouseMapper.selectById(warehouseId);
        if (warehouse == null) {
            throw exception(SLOTTING_NO_SUITABLE_WAREHOUSE, String.valueOf(skuId));
        }

        WmsSlottingRecommendationRespVO resp = new WmsSlottingRecommendationRespVO();
        resp.setRecommendedWarehouseId(warehouseId);
        resp.setRecommendedWarehouseCode(warehouse.getCode());
        resp.setRecommendedWarehouseName(warehouse.getName());

        // 2. 查找指定仓库下该 SKU 的库存行（合并到已有库存）
        WmsInventoryDO existingInventory = inventoryMapper.selectBySkuIdAndWarehouseId(skuId, warehouseId);
        if (existingInventory == null) {
            // 2.1 无库存行 → 新建库存行 + 新建批次
            resp.setReason(WmsSlottingRecommendationRespVO.REASON_NEW_INVENTORY);
            resp.setReasonText("仓库无该 SKU 库存，新建库存行与批次");
            return resp;
        }
        resp.setRecommendedInventoryId(existingInventory.getId());

        // 3. 若提供了 batchNo，检查该库存行下是否有相同批次的库存
        if (batchNo == null || batchNo.isEmpty()) {
            // 3.1 无批次号 → 在已有库存行下新建无批次信息的批次（兼容无批次管理场景）
            resp.setReason(WmsSlottingRecommendationRespVO.REASON_NEW_BATCH_TO_EXISTING_INVENTORY);
            resp.setReasonText("已有 SKU 库存行，新建批次（无批次号）");
            return resp;
        }

        WmsInventoryBatchDO existingBatch = inventoryBatchMapper.selectByInventoryIdAndBatchNo(
                existingInventory.getId(), batchNo);
        if (existingBatch == null) {
            // 3.2 无同批次 → 新建批次
            resp.setReason(WmsSlottingRecommendationRespVO.REASON_NEW_BATCH_TO_EXISTING_INVENTORY);
            resp.setReasonText("已有 SKU 库存行，新建批次：" + batchNo);
            return resp;
        }

        // 4. 找到同批次 → 合并，但需校验过期日期一致（避免误合并）
        if (expiryDate != null && existingBatch.getExpiryDate() != null
                && !Objects.equals(expiryDate, existingBatch.getExpiryDate())) {
            // 过期日期不同，视为不同批次，建议新建
            log.warn("[recommendPutaway][同批次号但过期日期不一致，batchNo={}, 已有={}, 当前={}]",
                    batchNo, existingBatch.getExpiryDate(), expiryDate);
            resp.setReason(WmsSlottingRecommendationRespVO.REASON_NEW_BATCH_TO_EXISTING_INVENTORY);
            resp.setReasonText("同批次号但过期日期不一致，建议新建批次：" + batchNo);
            return resp;
        }

        resp.setRecommendedBatchId(existingBatch.getId());
        resp.setExistingBatchExpiryDate(existingBatch.getExpiryDate());
        resp.setReason(WmsSlottingRecommendationRespVO.REASON_CONSOLIDATE_BATCH);
        resp.setReasonText("合并到已有批次：" + batchNo);
        return resp;
    }

    @Override
    public void validateBatchNotExpired(String batchNo, LocalDate expiryDate) {
        if (expiryDate == null) {
            // 无保质期管理，直接放行
            return;
        }
        LocalDate today = LocalDate.now();
        if (expiryDate.isBefore(today)) {
            throw exception(INVENTORY_BATCH_PUTAWAY_EXPIRED, batchNo, expiryDate);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long mergeOrCreateBatch(WmsInventoryDO inventory, String batchNo, LocalDate productionDate,
                                   LocalDate expiryDate, BigDecimal quantity) {
        // 1. 无批次号 → 直接新建批次（不合并）
        if (batchNo == null || batchNo.isEmpty()) {
            return createNewBatch(inventory.getId(), batchNo, productionDate, expiryDate, quantity);
        }
        // 2. 查找已有同批次
        WmsInventoryBatchDO existingBatch = inventoryBatchMapper.selectByInventoryIdAndBatchNo(
                inventory.getId(), batchNo);
        if (existingBatch == null) {
            // 2.1 无同批次 → 新建
            return createNewBatch(inventory.getId(), batchNo, productionDate, expiryDate, quantity);
        }
        // 2.2 已有同批次 → 校验过期日期一致性后合并
        if (expiryDate != null && existingBatch.getExpiryDate() != null
                && !Objects.equals(expiryDate, existingBatch.getExpiryDate())) {
            throw exception(SLOTTING_BATCH_MISMATCH, existingBatch.getBatchNo(), batchNo);
        }
        // 数量累加：existingBatch.quantity + quantity
        BigDecimal newQuantity = (existingBatch.getQuantity() == null ? BigDecimal.ZERO : existingBatch.getQuantity())
                .add(quantity);
        WmsInventoryBatchDO updateObj = new WmsInventoryBatchDO();
        updateObj.setId(existingBatch.getId());
        updateObj.setQuantity(newQuantity);
        inventoryBatchMapper.updateById(updateObj);
        log.info("[mergeOrCreateBatch][合并批次成功，batchId={} batchNo={} newQuantity={}]",
                existingBatch.getId(), batchNo, newQuantity);
        return existingBatch.getId();
    }

    /**
     * 新建批次记录
     */
    private Long createNewBatch(Long inventoryId, String batchNo, LocalDate productionDate,
                                LocalDate expiryDate, BigDecimal quantity) {
        WmsInventoryBatchDO batch = WmsInventoryBatchDO.builder()
                .inventoryId(inventoryId)
                .batchNo(batchNo)
                .productionDate(productionDate)
                .expiryDate(expiryDate)
                .quantity(quantity)
                .lockedQuantity(BigDecimal.ZERO)
                .status(WmsInventoryBatchStatusEnum.AVAILABLE.getStatus())
                .build();
        inventoryBatchMapper.insert(batch);
        log.info("[createNewBatch][新建批次成功，batchId={} batchNo={} quantity={}]",
                batch.getId(), batchNo, quantity);
        return batch.getId();
    }

}
