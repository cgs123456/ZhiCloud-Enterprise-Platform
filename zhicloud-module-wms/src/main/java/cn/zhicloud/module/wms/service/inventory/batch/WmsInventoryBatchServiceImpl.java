package cn.zhicloud.module.wms.service.inventory.batch;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.wms.controller.admin.inventory.batch.vo.WmsInventoryBatchPageReqVO;
import cn.zhicloud.module.wms.controller.admin.inventory.batch.vo.WmsInventoryBatchSaveReqVO;
import cn.zhicloud.module.wms.controller.admin.inventory.batch.vo.WmsInventoryBatchStrategyRespVO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryBatchDO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryDO;
import cn.zhicloud.module.wms.dal.mysql.inventory.WmsInventoryBatchMapper;
import cn.zhicloud.module.wms.dal.mysql.inventory.WmsInventoryMapper;
import cn.zhicloud.module.wms.enums.inventory.WmsInventoryBatchStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.INVENTORY_BATCH_NOT_EXISTS;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.INVENTORY_NOT_EXISTS;

/**
 * WMS 库存批次 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
@Slf4j
public class WmsInventoryBatchServiceImpl implements WmsInventoryBatchService {

    @Resource
    private WmsInventoryBatchMapper inventoryBatchMapper;
    @Resource
    private WmsInventoryMapper inventoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBatch(WmsInventoryBatchSaveReqVO createReqVO) {
        // 1. 校验库存存在
        validateInventoryExists(createReqVO.getInventoryId());
        // 2. 校验同一库存下批次号唯一
        validateBatchNoUnique(null, createReqVO.getInventoryId(), createReqVO.getBatchNo());
        // 3. 插入批次
        WmsInventoryBatchDO batch = BeanUtils.toBean(createReqVO, WmsInventoryBatchDO.class);
        if (batch.getStatus() == null) {
            batch.setStatus(WmsInventoryBatchStatusEnum.AVAILABLE.getStatus());
        }
        if (batch.getLockedQuantity() == null) {
            batch.setLockedQuantity(BigDecimal.ZERO);
        }
        inventoryBatchMapper.insert(batch);
        return batch.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBatch(WmsInventoryBatchSaveReqVO updateReqVO) {
        // 1. 校验存在
        validateBatchExists(updateReqVO.getId());
        // 2. 校验同一库存下批次号唯一
        validateBatchNoUnique(updateReqVO.getId(), updateReqVO.getInventoryId(), updateReqVO.getBatchNo());
        // 3. 更新
        WmsInventoryBatchDO updateObj = BeanUtils.toBean(updateReqVO, WmsInventoryBatchDO.class);
        inventoryBatchMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(Long id) {
        validateBatchExists(id);
        inventoryBatchMapper.deleteById(id);
    }

    @Override
    public WmsInventoryBatchDO getBatch(Long id) {
        return inventoryBatchMapper.selectById(id);
    }

    @Override
    public PageResult<WmsInventoryBatchDO> getBatchPage(WmsInventoryBatchPageReqVO pageReqVO) {
        return inventoryBatchMapper.selectPage(pageReqVO);
    }

    @Override
    public List<WmsInventoryBatchDO> getBatchesByInventoryId(Long inventoryId) {
        return inventoryBatchMapper.selectListByInventoryId(inventoryId);
    }

    @Override
    public List<WmsInventoryBatchDO> getExpiringBatches(Integer days) {
        if (days == null || days < 0) {
            days = 0;
        }
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(days);
        return inventoryBatchMapper.selectListExpiringBetween(today, deadline);
    }

    @Override
    public List<WmsInventoryBatchDO> getExpiredBatches() {
        LocalDate today = LocalDate.now();
        return inventoryBatchMapper.selectListExpiredBefore(today.minusDays(1));
    }

    @Override
    public WmsInventoryBatchStrategyRespVO applyFifoStrategy(Long inventoryId, BigDecimal quantity) {
        // 1. 查询可用批次（状态为 AVAILABLE 且可用数量 > 0）
        List<WmsInventoryBatchDO> batches = getAvailableBatches(inventoryId);
        // 2. FIFO：按生产日期升序（生产日期为空时按创建时间升序）
        batches.sort(Comparator.comparing(WmsInventoryBatchDO::getProductionDate,
                Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(WmsInventoryBatchDO::getId));
        // 3. 分配数量
        return allocateBatches(inventoryId, quantity, batches, "FIFO");
    }

    @Override
    public WmsInventoryBatchStrategyRespVO applyFefoStrategy(Long inventoryId, BigDecimal quantity) {
        // 1. 查询可用批次（状态为 AVAILABLE 且可用数量 > 0）
        List<WmsInventoryBatchDO> batches = getAvailableBatches(inventoryId);
        // 2. FEFO：按过期日期升序（过期日期为空时按创建时间升序）
        batches.sort(Comparator.comparing(WmsInventoryBatchDO::getExpiryDate,
                Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(WmsInventoryBatchDO::getId));
        // 3. 分配数量
        return allocateBatches(inventoryId, quantity, batches, "FEFO");
    }

    // ==================== 校验方法 ====================

    private WmsInventoryDO validateInventoryExists(Long inventoryId) {
        WmsInventoryDO inventory = inventoryMapper.selectById(inventoryId);
        if (inventory == null) {
            throw exception(INVENTORY_NOT_EXISTS);
        }
        return inventory;
    }

    private WmsInventoryBatchDO validateBatchExists(Long id) {
        WmsInventoryBatchDO batch = inventoryBatchMapper.selectById(id);
        if (batch == null) {
            throw exception(INVENTORY_BATCH_NOT_EXISTS);
        }
        return batch;
    }

    private void validateBatchNoUnique(Long id, Long inventoryId, String batchNo) {
        WmsInventoryBatchDO batch = inventoryBatchMapper.selectByInventoryIdAndBatchNo(inventoryId, batchNo);
        if (batch == null) {
            return;
        }
        if (id == null || ObjectUtil.notEqual(batch.getId(), id)) {
            throw exception(INVENTORY_BATCH_NOT_EXISTS, "批次号重复");
        }
    }

    // ==================== 策略分配 ====================

    private List<WmsInventoryBatchDO> getAvailableBatches(Long inventoryId) {
        List<WmsInventoryBatchDO> allBatches = inventoryBatchMapper.selectListByInventoryId(inventoryId);
        if (CollUtil.isEmpty(allBatches)) {
            return new ArrayList<>();
        }
        // 只保留可用状态且可用数量 > 0 的批次
        List<WmsInventoryBatchDO> available = new ArrayList<>();
        for (WmsInventoryBatchDO batch : allBatches) {
            if (!WmsInventoryBatchStatusEnum.AVAILABLE.getStatus().equals(batch.getStatus())) {
                continue;
            }
            if (calcAvailableQuantity(batch).compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            available.add(batch);
        }
        return available;
    }

    private WmsInventoryBatchStrategyRespVO allocateBatches(Long inventoryId, BigDecimal demandQuantity,
                                                            List<WmsInventoryBatchDO> sortedBatches, String strategy) {
        WmsInventoryBatchStrategyRespVO respVO = new WmsInventoryBatchStrategyRespVO();
        respVO.setInventoryId(inventoryId);
        respVO.setDemandQuantity(demandQuantity);
        respVO.setStrategy(strategy);
        respVO.setAllocations(new ArrayList<>());
        if (demandQuantity == null || demandQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            respVO.setAllocatedQuantity(BigDecimal.ZERO);
            respVO.setSufficient(false);
            return respVO;
        }
        BigDecimal remaining = demandQuantity;
        int sequence = 1;
        for (WmsInventoryBatchDO batch : sortedBatches) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal available = calcAvailableQuantity(batch);
            if (available.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal allocate = remaining.min(available);
            WmsInventoryBatchStrategyRespVO.BatchAllocation allocation = new WmsInventoryBatchStrategyRespVO.BatchAllocation();
            allocation.setSequence(sequence++);
            allocation.setBatchId(batch.getId());
            allocation.setBatchNo(batch.getBatchNo());
            allocation.setProductionDate(batch.getProductionDate());
            allocation.setExpiryDate(batch.getExpiryDate());
            allocation.setAvailableQuantity(available);
            allocation.setAllocateQuantity(allocate);
            respVO.getAllocations().add(allocation);
            remaining = remaining.subtract(allocate);
        }
        BigDecimal allocated = demandQuantity.subtract(remaining);
        respVO.setAllocatedQuantity(allocated);
        respVO.setSufficient(remaining.compareTo(BigDecimal.ZERO) <= 0);
        return respVO;
    }

    private BigDecimal calcAvailableQuantity(WmsInventoryBatchDO batch) {
        BigDecimal qty = batch.getQuantity() == null ? BigDecimal.ZERO : batch.getQuantity();
        BigDecimal locked = batch.getLockedQuantity() == null ? BigDecimal.ZERO : batch.getLockedQuantity();
        return qty.subtract(locked);
    }

}
