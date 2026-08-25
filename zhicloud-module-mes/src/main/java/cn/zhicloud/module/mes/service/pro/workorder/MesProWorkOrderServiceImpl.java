package cn.zhicloud.module.mes.service.pro.workorder;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderPageReqVO;
import cn.zhicloud.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderSaveReqVO;
import cn.zhicloud.module.mes.dal.dataobject.md.item.MesMdItemBatchConfigDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.zhicloud.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.zhicloud.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.zhicloud.module.mes.enums.wm.BarcodeBizTypeEnum;
import cn.zhicloud.module.mes.service.md.item.MesMdItemBatchConfigService;
import cn.zhicloud.module.mes.service.md.item.MesMdItemService;
import cn.zhicloud.module.mes.service.pro.task.MesProTaskService;
import cn.zhicloud.module.mes.service.wm.barcode.MesWmBarcodeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.*;

/**
 * MES 生产工单 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class MesProWorkOrderServiceImpl implements MesProWorkOrderService {

    @Resource
    private MesProWorkOrderMapper workOrderMapper;

    @Resource
    private MesProWorkOrderBomService workOrderBomService;
    @Resource
    private MesMdItemService itemService;
    @Resource
    private MesMdItemBatchConfigService itemBatchConfigService;
    @Resource
    private MesWmBarcodeService barcodeService;
    @Resource
    private MesProTaskService taskService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWorkOrder(MesProWorkOrderSaveReqVO createReqVO) {
        // 1. 校验数据
        validateWorkOrderSaveData(null, createReqVO);

        // 2.1 设置默认值
        if (createReqVO.getParentId() == null) {
            createReqVO.setParentId(MesProWorkOrderDO.PARENT_ID_NULL);
        }
        // 2.2 插入工单
        MesProWorkOrderDO workOrder = BeanUtils.toBean(createReqVO, MesProWorkOrderDO.class);
        workOrder.setStatus(MesProWorkOrderStatusEnum.PREPARE.getStatus());
        workOrderMapper.insert(workOrder);

        // 3. 自动生成 BOM：根据产品 BOM 生成工单 BOM
        workOrderBomService.generateWorkOrderBom(workOrder.getId(), createReqVO, false);

        // 4. 自动生成条码
        barcodeService.autoGenerateBarcode(BarcodeBizTypeEnum.WORKORDER.getValue(),
                workOrder.getId(), workOrder.getCode(), workOrder.getName());
        return workOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWorkOrder(MesProWorkOrderSaveReqVO updateReqVO) {
        // 1.1 校验存在 + 只有草稿状态才能编辑
        MesProWorkOrderDO oldWorkOrder = validateWorkOrderExists(updateReqVO.getId());
        if (ObjUtil.notEqual(oldWorkOrder.getStatus(), MesProWorkOrderStatusEnum.PREPARE.getStatus())) {
            throw exception(PRO_WORK_ORDER_NOT_PREPARE);
        }
        // 1.2 校验数据
        validateWorkOrderSaveData(updateReqVO.getId(), updateReqVO);

        // 2. 判断产品或数量是否变更，如果变更则重新生成 BOM（updated=true 会先清理旧数据）
        boolean productChanged = ObjUtil.notEqual(oldWorkOrder.getProductId(), updateReqVO.getProductId());
        boolean quantityChanged = oldWorkOrder.getQuantity().compareTo(updateReqVO.getQuantity()) != 0;
        if (productChanged || quantityChanged) {
            workOrderBomService.generateWorkOrderBom(updateReqVO.getId(), updateReqVO, true);
        }

        // 3. 更新
        MesProWorkOrderDO updateObj = BeanUtils.toBean(updateReqVO, MesProWorkOrderDO.class);
        updateObj.setVersion(oldWorkOrder.getVersion());
        workOrderMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkOrder(Long id) {
        // 1.1 校验存在
        MesProWorkOrderDO workOrder = validateWorkOrderExists(id);
        // 1.2 只能删除草稿状态的工单
        if (ObjUtil.notEqual(workOrder.getStatus(), MesProWorkOrderStatusEnum.PREPARE.getStatus())) {
            throw exception(PRO_WORK_ORDER_NOT_PREPARE);
        }
        // 1.3 校验是否有子工单
        Long childCount = workOrderMapper.selectCount(MesProWorkOrderDO::getParentId, id);
        if (childCount > 0) {
            throw exception(PRO_WORK_ORDER_HAS_CHILDREN);
        }

        // 2. 删除工单 + BOM
        workOrderMapper.deleteById(id);
        workOrderBomService.deleteWorkOrderBomByWorkOrderId(id);
    }

    @Override
    public MesProWorkOrderDO validateWorkOrderExists(Long id) {
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(id);
        if (workOrder == null) {
            throw exception(PRO_WORK_ORDER_NOT_EXISTS);
        }
        return workOrder;
    }

    @Override
    public MesProWorkOrderDO getWorkOrder(Long id) {
        return workOrderMapper.selectById(id);
    }

    @Override
    public MesProWorkOrderDO getWorkOrder(String code) {
        return workOrderMapper.selectByCode(code);
    }

    @Override
    public PageResult<MesProWorkOrderDO> getWorkOrderPage(MesProWorkOrderPageReqVO pageReqVO) {
        return workOrderMapper.selectPage(pageReqVO);
    }

    @Override
    public List<MesProWorkOrderDO> getWorkOrderList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return workOrderMapper.selectByIds(ids);
    }

    @Override
    public MesProWorkOrderDO validateWorkOrderConfirmed(Long id) {
        MesProWorkOrderDO workOrder = validateWorkOrderExists(id);
        if (ObjUtil.notEqual(workOrder.getStatus(), MesProWorkOrderStatusEnum.CONFIRMED.getStatus())) {
            throw exception(PRO_WORK_ORDER_NOT_CONFIRMED);
        }
        return workOrder;
    }

    @Override
    public void confirmWorkOrder(Long id) {
        // 1.1 校验存在
        MesProWorkOrderDO workOrder = validateWorkOrderExists(id);
        // 1.2 只有草稿状态才能确认
        if (ObjUtil.notEqual(workOrder.getStatus(), MesProWorkOrderStatusEnum.PREPARE.getStatus())) {
            throw exception(PRO_WORK_ORDER_NOT_PREPARE);
        }

        // 2. 更新状态为已确认
        workOrderMapper.updateById(new MesProWorkOrderDO().setId(id)
                .setStatus(MesProWorkOrderStatusEnum.CONFIRMED.getStatus())
                .setVersion(workOrder.getVersion()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispatchWorkOrder(Long id) {
        // 1. 校验存在 + 只有已确认状态才能派工
        MesProWorkOrderDO workOrder = validateWorkOrderExists(id);
        if (ObjUtil.notEqual(workOrder.getStatus(), MesProWorkOrderStatusEnum.CONFIRMED.getStatus())) {
            throw exception(PRO_WORK_ORDER_NOT_CONFIRMED);
        }

        // 2. 更新工单状态为已派工
        workOrderMapper.updateById(new MesProWorkOrderDO().setId(id)
                .setStatus(MesProWorkOrderStatusEnum.DISPATCHED.getStatus())
                .setVersion(workOrder.getVersion()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startWorkOrder(Long id) {
        // 1. 校验存在 + 只有已派工状态才能开工
        MesProWorkOrderDO workOrder = validateWorkOrderExists(id);
        if (ObjUtil.notEqual(workOrder.getStatus(), MesProWorkOrderStatusEnum.DISPATCHED.getStatus())) {
            throw exception(PRO_WORK_ORDER_NOT_DISPATCHED);
        }

        // 2. 更新工单状态为报工中
        workOrderMapper.updateById(new MesProWorkOrderDO().setId(id)
                .setStatus(MesProWorkOrderStatusEnum.REPORTING.getStatus())
                .setVersion(workOrder.getVersion()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishWorkOrder(Long id) {
        // 1. 校验存在
        MesProWorkOrderDO workOrder = validateWorkOrderExists(id);
        // 2. 状态机：放行 CONFIRMED / DISPATCHED / REPORTING，拦截 PREPARE / CANCELED / FINISHED / CLOSED
        //    原实现仅允许 CONFIRMED，导致已派工(DISPATCHED)或报工中(REPORTING)的工单被永久卡死无法完工
        if (ObjUtil.equal(workOrder.getStatus(), MesProWorkOrderStatusEnum.PREPARE.getStatus())
                || ObjUtil.equal(workOrder.getStatus(), MesProWorkOrderStatusEnum.CANCELED.getStatus())
                || ObjUtil.equal(workOrder.getStatus(), MesProWorkOrderStatusEnum.FINISHED.getStatus())
                || ObjUtil.equal(workOrder.getStatus(), MesProWorkOrderStatusEnum.CLOSED.getStatus())) {
            throw exception(PRO_WORK_ORDER_NOT_CONFIRMED);
        }

        // 3. 级联完成所有关联任务
        taskService.finishTaskByOrderId(id);

        // 4. 更新工单状态为已完成
        workOrderMapper.updateById(new MesProWorkOrderDO().setId(id)
                .setStatus(MesProWorkOrderStatusEnum.FINISHED.getStatus())
                .setFinishDate(LocalDateTime.now())
                .setVersion(workOrder.getVersion()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeWorkOrder(Long id) {
        // 1. 校验存在 + 只有已完成状态才能关闭
        MesProWorkOrderDO workOrder = validateWorkOrderExists(id);
        if (ObjUtil.notEqual(workOrder.getStatus(), MesProWorkOrderStatusEnum.FINISHED.getStatus())) {
            throw exception(PRO_WORK_ORDER_NOT_CLOSED);
        }

        // 2. 更新工单状态为已关闭
        workOrderMapper.updateById(new MesProWorkOrderDO().setId(id)
                .setStatus(MesProWorkOrderStatusEnum.CLOSED.getStatus())
                .setVersion(workOrder.getVersion()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelWorkOrder(Long id) {
        // 1. 校验存在 + 只有已确认状态才能取消
        MesProWorkOrderDO workOrder = validateWorkOrderExists(id);
        if (ObjUtil.notEqual(workOrder.getStatus(), MesProWorkOrderStatusEnum.CONFIRMED.getStatus())) {
            throw exception(PRO_WORK_ORDER_NOT_CONFIRMED);
        }

        // 2. 级联取消所有关联任务
        taskService.cancelTaskByOrderId(id);

        // 3. 更新工单状态为已取消
        workOrderMapper.updateById(new MesProWorkOrderDO().setId(id)
                .setStatus(MesProWorkOrderStatusEnum.CANCELED.getStatus())
                .setCancelDate(LocalDateTime.now())
                .setVersion(workOrder.getVersion()));
    }

    // ==================== 校验方法 ====================

    private void validateWorkOrderSaveData(Long id, MesProWorkOrderSaveReqVO reqVO) {
        // 1. 校验编码唯一
        validateWorkOrderCodeUnique(id, reqVO.getCode());
        // 2. 校验产品存在
        itemService.validateItemExists(reqVO.getProductId());
        // 3. 校验批次配置（如果产品有 clientFlag=true，则 clientId 必填）
        MesMdItemBatchConfigDO batchConfig = itemBatchConfigService.getItemBatchConfigByItemId(reqVO.getProductId());
        if (batchConfig != null && Boolean.TRUE.equals(batchConfig.getClientFlag()) && reqVO.getClientId() == null) {
            throw exception(MD_CLIENT_NOT_EXISTS);
        }
    }

    private void validateWorkOrderCodeUnique(Long id, String code) {
        if (code == null) {
            return;
        }
        MesProWorkOrderDO workOrder = workOrderMapper.selectByCode(code);
        if (workOrder == null) {
            return;
        }
        if (ObjUtil.notEqual(workOrder.getId(), id)) {
            throw exception(PRO_WORK_ORDER_CODE_DUPLICATE);
        }
    }

    @Override
    public void updateProducedQuantity(Long id, BigDecimal incrQuantityProduced) {
        // 校验工单存在
        validateWorkOrderExists(id);
        // 更新数量（Mapper 层 CAS 拦截超产：影响行数为 0 表示累计产量超过计划产量）
        int updatedRows = workOrderMapper.updateProducedQuantity(id, incrQuantityProduced);
        if (updatedRows == 0) {
            throw exception(PRO_WORK_ORDER_OVER_PRODUCED);
        }
    }

    @Override
    public Long getWorkOrderCountByVendorId(Long vendorId) {
        return workOrderMapper.selectCountByVendorId(vendorId);
    }

}
