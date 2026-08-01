package cn.iocoder.yudao.module.mes.service.pro.rework;

import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.rework.vo.MesProReworkOrderCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.rework.vo.MesProReworkOrderPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.rework.vo.MesProReworkOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.rework.MesProReworkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.rework.MesProReworkOrderDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.rework.MesProReworkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProReworkStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;

/**
 * MES 返工工单 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class MesProReworkOrderServiceImpl implements MesProReworkOrderService {

    @Resource
    private MesProReworkOrderMapper reworkOrderMapper;
    @Resource
    private MesProReworkOrderDetailMapper reworkOrderDetailMapper;

    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private MesProWorkOrderService workOrderService;
    @Resource
    private MesMdItemService itemService;
    @Resource
    @Lazy
    private MesProReworkOrderDetailService reworkOrderDetailService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReworkOrder(MesProReworkOrderCreateReqVO createReqVO) {
        // 1.1 校验编码唯一
        validateReworkOrderCodeUnique(null, createReqVO.getCode());
        // 1.2 校验原工单存在 + 状态为 FINISHED/CLOSED
        MesProWorkOrderDO originalWorkOrder = validateOriginalWorkOrderFinished(createReqVO.getOriginalWorkOrderId());
        // 1.3 校验返工数量：返工数量 <= 原工单已生产数量 - 已返工数量
        validateReworkQuantity(createReqVO.getOriginalWorkOrderId(),
                originalWorkOrder.getQuantityProduced(), createReqVO.getReworkQuantity());

        // 2. 获取产品信息
        MesMdItemDO item = itemService.validateItemExists(originalWorkOrder.getProductId());

        // 3. 插入返工工单
        MesProReworkOrderDO reworkOrder = BeanUtils.toBean(createReqVO, MesProReworkOrderDO.class);
        reworkOrder.setOriginalWorkOrderCode(originalWorkOrder.getCode())
                .setProductId(originalWorkOrder.getProductId())
                .setProductCode(item.getCode())
                .setProductName(item.getName())
                .setStatus(MesProReworkStatusEnum.PENDING.getStatus());
        reworkOrderMapper.insert(reworkOrder);
        return reworkOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReworkOrder(MesProReworkOrderSaveReqVO updateReqVO) {
        // 1.1 校验存在
        MesProReworkOrderDO existing = validateReworkOrderExists(updateReqVO.getId());
        // 1.2 只有待返工状态才能编辑
        if (ObjUtil.notEqual(existing.getStatus(), MesProReworkStatusEnum.PENDING.getStatus())) {
            throw exception(PRO_REWORK_ORDER_STATUS_INVALID);
        }
        // 1.3 校验编码唯一
        validateReworkOrderCodeUnique(updateReqVO.getId(), updateReqVO.getCode());

        // 2. 更新
        MesProReworkOrderDO updateObj = BeanUtils.toBean(updateReqVO, MesProReworkOrderDO.class);
        reworkOrderMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReworkOrder(Long id) {
        // 1.1 校验存在
        MesProReworkOrderDO existing = validateReworkOrderExists(id);
        // 1.2 只有待返工状态才能删除
        if (ObjUtil.notEqual(existing.getStatus(), MesProReworkStatusEnum.PENDING.getStatus())) {
            throw exception(PRO_REWORK_ORDER_STATUS_INVALID);
        }

        // 2. 删除返工工单 + 明细
        reworkOrderMapper.deleteById(id);
        reworkOrderDetailMapper.deleteByReworkOrderId(id);
    }

    @Override
    public MesProReworkOrderDO validateReworkOrderExists(Long id) {
        MesProReworkOrderDO reworkOrder = reworkOrderMapper.selectById(id);
        if (reworkOrder == null) {
            throw exception(PRO_REWORK_ORDER_NOT_EXISTS);
        }
        return reworkOrder;
    }

    @Override
    public MesProReworkOrderDO getReworkOrder(Long id) {
        return reworkOrderMapper.selectById(id);
    }

    @Override
    public PageResult<MesProReworkOrderDO> getReworkOrderPage(MesProReworkOrderPageReqVO pageReqVO) {
        return reworkOrderMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startRework(Long id) {
        // 1. 校验存在 + 只有待返工状态才能开工
        MesProReworkOrderDO reworkOrder = validateReworkOrderExists(id);
        if (ObjUtil.notEqual(reworkOrder.getStatus(), MesProReworkStatusEnum.PENDING.getStatus())) {
            throw exception(PRO_REWORK_ORDER_NOT_STARTABLE);
        }

        // 2. 更新状态为返工中 + 实际开始时间
        reworkOrderMapper.updateById(new MesProReworkOrderDO().setId(id)
                .setStatus(MesProReworkStatusEnum.REWORKING.getStatus())
                .setActualStartTime(LocalDateTime.now()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeRework(Long id) {
        // 1. 校验存在 + 只有返工中状态才能完工
        MesProReworkOrderDO reworkOrder = validateReworkOrderExists(id);
        if (ObjUtil.notEqual(reworkOrder.getStatus(), MesProReworkStatusEnum.REWORKING.getStatus())) {
            throw exception(PRO_REWORK_ORDER_NOT_COMPLETABLE);
        }
        // 2. 校验所有明细已处理
        if (!reworkOrderDetailService.validateAllDetailsProcessed(id)) {
            throw exception(PRO_REWORK_ORDER_NOT_COMPLETABLE);
        }

        // 3. 更新状态为已完成 + 实际结束时间
        reworkOrderMapper.updateById(new MesProReworkOrderDO().setId(id)
                .setStatus(MesProReworkStatusEnum.COMPLETED.getStatus())
                .setActualEndTime(LocalDateTime.now()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelRework(Long id) {
        // 1. 校验存在
        MesProReworkOrderDO reworkOrder = validateReworkOrderExists(id);
        // 2. 已完成或已取消的工单不允许取消
        if (ObjUtil.equal(reworkOrder.getStatus(), MesProReworkStatusEnum.COMPLETED.getStatus())
                || ObjUtil.equal(reworkOrder.getStatus(), MesProReworkStatusEnum.CANCELED.getStatus())) {
            throw exception(PRO_REWORK_ORDER_STATUS_INVALID);
        }

        // 3. 更新状态为已取消
        reworkOrderMapper.updateById(new MesProReworkOrderDO().setId(id)
                .setStatus(MesProReworkStatusEnum.CANCELED.getStatus()));
    }

    // ==================== 校验方法 ====================

    private void validateReworkOrderCodeUnique(Long id, String code) {
        if (code == null) {
            return;
        }
        MesProReworkOrderDO reworkOrder = reworkOrderMapper.selectByCode(code);
        if (reworkOrder == null) {
            return;
        }
        if (ObjUtil.notEqual(reworkOrder.getId(), id)) {
            throw exception(PRO_REWORK_ORDER_CODE_DUPLICATE);
        }
    }

    /**
     * 校验原工单存在 + 状态为 FINISHED/CLOSED
     */
    private MesProWorkOrderDO validateOriginalWorkOrderFinished(Long originalWorkOrderId) {
        MesProWorkOrderDO workOrder = workOrderService.validateWorkOrderExists(originalWorkOrderId);
        if (ObjUtil.notEqual(workOrder.getStatus(), MesProWorkOrderStatusEnum.FINISHED.getStatus())
                && ObjUtil.notEqual(workOrder.getStatus(), MesProWorkOrderStatusEnum.CLOSED.getStatus())) {
            throw exception(PRO_REWORK_ORDER_STATUS_INVALID);
        }
        return workOrder;
    }

    /**
     * 校验返工数量：返工数量 <= 原工单已生产数量 - 已返工数量
     */
    private void validateReworkQuantity(Long originalWorkOrderId, BigDecimal quantityProduced, BigDecimal reworkQuantity) {
        if (quantityProduced == null) {
            quantityProduced = BigDecimal.ZERO;
        }
        BigDecimal reworkedSum = reworkOrderMapper.selectReworkedQuantitySumByOriginalWorkOrderId(originalWorkOrderId);
        BigDecimal available = quantityProduced.subtract(reworkedSum);
        if (reworkQuantity.compareTo(available) > 0) {
            throw exception(PRO_REWORK_ORDER_QUANTITY_INVALID);
        }
    }

}
