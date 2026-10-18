package cn.zhicloud.module.wms.service.inventory.transfer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.zhicloud.module.wms.controller.admin.inventory.batch.vo.WmsInventoryBatchStrategyRespVO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryBatchDO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryDO;
import cn.zhicloud.module.wms.dal.dataobject.md.location.WmsLocationDO;
import cn.zhicloud.module.wms.dal.mysql.inventory.WmsInventoryBatchMapper;
import cn.zhicloud.module.wms.dal.mysql.inventory.WmsInventoryMapper;
import cn.zhicloud.module.wms.enums.inventory.WmsInventoryChangeTypeEnum;
import cn.zhicloud.module.wms.enums.order.WmsOrderTypeEnum;
import cn.zhicloud.module.wms.service.inventory.WmsInventoryService;
import cn.zhicloud.module.wms.service.inventory.batch.WmsInventoryBatchService;
import cn.zhicloud.module.wms.service.inventory.dto.WmsInventoryChangeReqDTO;
import cn.zhicloud.module.wms.service.inventory.slotting.WmsSlottingService;
import cn.zhicloud.module.wms.service.md.location.WmsLocationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.INVENTORY_BATCH_FEFO_INSUFFICIENT;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.INVENTORY_BATCH_QUANTITY_NOT_ENOUGH;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.INVENTORY_NOT_EXISTS;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.LOCATION_MOVE_CROSS_WAREHOUSE;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.LOCATION_MOVE_SAME;

/**
 * WMS 库存移库 Service 实现类
 *
 * <p>V211（ADR 269 D3）：PDA confirm-move 真实化——库内同仓跨位真转移（from ≠ to）：
 * <ol>
 *   <li>两验门禁：来源/目标库位可用性（存在且未锁定/禁用，错误码 000/005/006 沿用）；</li>
 *   <li>同库位（008）/ 跨仓（009）拒绝——跨仓移库由移库单（WmsMovementOrder）承接；</li>
 *   <li>源行三元键存在校验（无源行如实 INVENTORY_NOT_EXISTS，拒绝路径零副作用）；</li>
 *   <li>批次随行：源行有批次行 → FEFO 强口径先扣后转（不足/扣减失败与拣货同码同参）；</li>
 *   <li>库存转移：changeInventory(OUT from + IN to) 同事务单次变更（orderType=MOVEMENT、
 *       orderNo=PDA-MOVE、orderId=null；占用派生复用其触达机制，零新码）；</li>
 *   <li>目标行批次随行合并（mergeOrCreateBatch，与上架同径）。</li>
 * </ol>
 *
 * @author 智云
 */
@Service
@Validated
@Slf4j
public class WmsInventoryTransferServiceImpl implements WmsInventoryTransferService {

    /**
     * 移库流水单据号（PDA 移库无单据载体，固定串；ADR 269 D3）
     */
    private static final String MOVE_ORDER_NO = "PDA-MOVE";

    @Resource
    private WmsLocationService locationService;
    @Resource
    private WmsInventoryMapper inventoryMapper;
    @Resource
    private WmsInventoryBatchMapper inventoryBatchMapper;
    @Resource
    private WmsInventoryBatchService inventoryBatchService;
    @Resource
    private WmsSlottingService slottingService;
    // ADR 269 R2：本接口必须 @Autowired（byType）——@Resource 按名会撞框架 starter
    // InventoryAutoConfiguration#inventoryService() 的同名 bean（既有 WMS 类同惯例）
    @Autowired
    private WmsInventoryService inventoryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transfer(Long fromLocationId, Long toLocationId, Long skuId, BigDecimal quantity) {
        // 1. 两验门禁：来源/目标库位可用（存在且未锁定/禁用，错误码 000/005/006 沿用）
        WmsLocationDO fromLocation = locationService.validateLocationUsable(fromLocationId);
        WmsLocationDO toLocation = locationService.validateLocationUsable(toLocationId);
        // 2. 同库位 / 跨仓拒绝（V211 新码 008/009；跨仓由移库单承接）
        if (ObjectUtil.equal(fromLocationId, toLocationId)) {
            throw exception(LOCATION_MOVE_SAME);
        }
        if (!ObjectUtil.equal(fromLocation.getWarehouseId(), toLocation.getWarehouseId())) {
            throw exception(LOCATION_MOVE_CROSS_WAREHOUSE);
        }
        Long warehouseId = fromLocation.getWarehouseId();
        // 3. 源行必须存在（三元键精确行；查无如实拒绝）
        WmsInventoryDO sourceRow = inventoryMapper.selectBySkuIdAndWarehouseIdAndLocationId(
                skuId, warehouseId, fromLocationId);
        if (sourceRow == null) {
            throw exception(INVENTORY_NOT_EXISTS);
        }
        // 4. 批次随行（FEFO 强口径）：源行有批次行则先按 FEFO 原子扣减（先扣后转，与拣货同源同序）
        List<WmsInventoryBatchStrategyRespVO.BatchAllocation> allocations = deductFefoBatches(sourceRow, quantity);
        // 5. 库存转移：OUT 源行 + IN 目标行，同事务单次变更（占用派生复用 changeInventory 触达机制）
        inventoryService.changeInventory(buildChangeDTO(skuId, warehouseId, fromLocationId, toLocationId, quantity));
        // 6. 目标行批次随行合并（IN 已确保目标行存在；与上架 mergeOrCreateBatch 同径）
        if (CollUtil.isNotEmpty(allocations)) {
            WmsInventoryDO targetRow = inventoryMapper.selectBySkuIdAndWarehouseIdAndLocationId(
                    skuId, warehouseId, toLocationId);
            if (targetRow == null) {
                // 理论不可达：步骤 5 的 IN 已确保目标行存在（防御性兜底）
                throw exception(INVENTORY_NOT_EXISTS);
            }
            for (WmsInventoryBatchStrategyRespVO.BatchAllocation alloc : allocations) {
                slottingService.mergeOrCreateBatch(targetRow, alloc.getBatchNo(),
                        alloc.getProductionDate(), alloc.getExpiryDate(), alloc.getAllocateQuantity());
            }
        }
        log.info("[transfer][PDA 移库成功 from={} to={} 仓库={} SKU={} 数量={} 批次分配数={}]",
                fromLocationId, toLocationId, warehouseId, skuId, quantity, allocations.size());
    }

    /**
     * 构建移库库存变更 DTO（OUT from + IN to 同批；orderType=MOVEMENT、orderNo=PDA-MOVE、orderId=null）
     */
    private static WmsInventoryChangeReqDTO buildChangeDTO(Long skuId, Long warehouseId,
                                                           Long fromLocationId, Long toLocationId,
                                                           BigDecimal quantity) {
        WmsInventoryChangeReqDTO reqDTO = new WmsInventoryChangeReqDTO();
        reqDTO.setOrderId(null);
        reqDTO.setOrderNo(MOVE_ORDER_NO);
        reqDTO.setOrderType(WmsOrderTypeEnum.MOVEMENT.getType());
        reqDTO.setItems(List.of(
                buildChangeItem(skuId, warehouseId, fromLocationId, quantity, WmsInventoryChangeTypeEnum.OUT),
                buildChangeItem(skuId, warehouseId, toLocationId, quantity, WmsInventoryChangeTypeEnum.IN)));
        return reqDTO;
    }

    private static WmsInventoryChangeReqDTO.Item buildChangeItem(Long skuId, Long warehouseId,
                                                                Long locationId, BigDecimal quantity,
                                                                WmsInventoryChangeTypeEnum changeType) {
        WmsInventoryChangeReqDTO.Item item = new WmsInventoryChangeReqDTO.Item();
        item.setSkuId(skuId);
        item.setWarehouseId(warehouseId);
        item.setLocationId(locationId);
        item.setQuantity(quantity);
        item.setChangeType(changeType);
        return item;
    }

    /**
     * 源行批次随行（FEFO 强口径）：源行有批次行 → 策略分配并逐分配原子扣减；
     * 无批次行 → 跳过（兼容无批次管理，纯行转移不阻断）。
     *
     * <p>与 {@code WmsPdaServiceImpl#applyFefoAndDeductBatches} 同源同序（不足/扣减失败与拣货同码同参）；
     * 二者暂不共用——拣货链为既有已验链不动，第三个消费方出现时再抽公共件（ADR 269 D4）。
     */
    private List<WmsInventoryBatchStrategyRespVO.BatchAllocation> deductFefoBatches(WmsInventoryDO sourceRow,
                                                                                    BigDecimal quantity) {
        // 1. 无批次记录则跳过 FEFO（兼容无批次管理场景）
        List<WmsInventoryBatchDO> batches = inventoryBatchMapper.selectListByInventoryId(sourceRow.getId());
        if (CollUtil.isEmpty(batches)) {
            return Collections.emptyList();
        }
        // 2. FEFO 策略（先到期先出）；不足整体拒绝（与拣货同码同参）
        WmsInventoryBatchStrategyRespVO strategy = inventoryBatchService.applyFefoStrategy(sourceRow.getId(), quantity);
        if (strategy == null || !strategy.getSufficient()) {
            throw exception(INVENTORY_BATCH_FEFO_INSUFFICIENT,
                    sourceRow.getId(), sourceRow.getId(), quantity,
                    strategy == null ? BigDecimal.ZERO : strategy.getAllocatedQuantity());
        }
        // 3. 逐分配原子扣减（quantity >= 扣减数才更新，防并发超扣；0 行更新同拣货错误语义）
        for (WmsInventoryBatchStrategyRespVO.BatchAllocation alloc : strategy.getAllocations()) {
            int updatedRows = inventoryBatchMapper.deductQuantity(alloc.getBatchId(), alloc.getAllocateQuantity());
            if (updatedRows == 0) {
                WmsInventoryBatchDO latest = inventoryBatchMapper.selectById(alloc.getBatchId());
                throw exception(INVENTORY_BATCH_QUANTITY_NOT_ENOUGH,
                        latest == null ? String.valueOf(alloc.getBatchId()) : latest.getBatchNo(),
                        latest == null ? BigDecimal.ZERO
                                : (latest.getQuantity() == null ? BigDecimal.ZERO : latest.getQuantity()),
                        alloc.getAllocateQuantity());
            }
            log.info("[deductFefoBatches][移库 FEFO 原子扣减批次 batchId={} deduct={}]",
                    alloc.getBatchId(), alloc.getAllocateQuantity());
        }
        return strategy.getAllocations();
    }

}
