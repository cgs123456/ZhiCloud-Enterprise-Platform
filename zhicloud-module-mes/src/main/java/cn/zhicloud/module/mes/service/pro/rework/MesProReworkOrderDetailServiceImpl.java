package cn.zhicloud.module.mes.service.pro.rework;

import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.mes.controller.admin.pro.rework.vo.MesProReworkOrderDetailSaveReqVO;
import cn.zhicloud.module.mes.dal.dataobject.pro.rework.MesProReworkOrderDetailDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.rework.MesProReworkOrderDO;
import cn.zhicloud.module.mes.dal.mysql.pro.rework.MesProReworkOrderDetailMapper;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_REWORK_DETAIL_NOT_EXISTS;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_REWORK_ORDER_STATUS_INVALID;
import static cn.zhicloud.module.mes.enums.pro.MesProReworkStatusEnum.PENDING;

/**
 * MES 返工工单明细 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class MesProReworkOrderDetailServiceImpl implements MesProReworkOrderDetailService {

    @Resource
    private MesProReworkOrderDetailMapper reworkOrderDetailMapper;

    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private MesProReworkOrderService reworkOrderService;

    @Override
    public Long addDetail(MesProReworkOrderDetailSaveReqVO createReqVO) {
        // 1. 校验返工工单存在 + 待返工状态
        validateReworkOrderPending(createReqVO.getReworkOrderId());

        // 2. 插入
        MesProReworkOrderDetailDO detail = BeanUtils.toBean(createReqVO, MesProReworkOrderDetailDO.class);
        reworkOrderDetailMapper.insert(detail);
        return detail.getId();
    }

    @Override
    public void updateDetail(MesProReworkOrderDetailSaveReqVO updateReqVO) {
        // 1.1 校验明细存在
        MesProReworkOrderDetailDO existing = validateDetailExists(updateReqVO.getId());
        // 1.2 校验返工工单存在 + 待返工状态（使用明细所属的返工工单）
        validateReworkOrderPending(existing.getReworkOrderId());

        // 2. 更新
        MesProReworkOrderDetailDO updateObj = BeanUtils.toBean(updateReqVO, MesProReworkOrderDetailDO.class);
        reworkOrderDetailMapper.updateById(updateObj);
    }

    @Override
    public void deleteDetail(Long id) {
        // 1.1 校验明细存在
        MesProReworkOrderDetailDO existing = validateDetailExists(id);
        // 1.2 校验返工工单存在 + 待返工状态
        validateReworkOrderPending(existing.getReworkOrderId());

        // 2. 删除
        reworkOrderDetailMapper.deleteById(id);
    }

    @Override
    public MesProReworkOrderDetailDO getDetail(Long id) {
        return reworkOrderDetailMapper.selectById(id);
    }

    @Override
    public List<MesProReworkOrderDetailDO> listByReworkOrderId(Long reworkOrderId) {
        return reworkOrderDetailMapper.selectListByReworkOrderId(reworkOrderId);
    }

    @Override
    public boolean validateAllDetailsProcessed(Long reworkOrderId) {
        List<MesProReworkOrderDetailDO> details = reworkOrderDetailMapper.selectListByReworkOrderId(reworkOrderId);
        if (details.isEmpty()) {
            // 无明细视为未处理，不允许完工
            return false;
        }
        for (MesProReworkOrderDetailDO detail : details) {
            BigDecimal repaired = detail.getRepairedQuantity() == null ? BigDecimal.ZERO : detail.getRepairedQuantity();
            BigDecimal defect = detail.getDefectQuantity() == null ? BigDecimal.ZERO : detail.getDefectQuantity();
            if (repaired.compareTo(defect) < 0) {
                return false;
            }
        }
        return true;
    }

    // ==================== 校验方法 ====================

    private MesProReworkOrderDetailDO validateDetailExists(Long id) {
        MesProReworkOrderDetailDO detail = reworkOrderDetailMapper.selectById(id);
        if (detail == null) {
            throw exception(PRO_REWORK_DETAIL_NOT_EXISTS);
        }
        return detail;
    }

    private void validateReworkOrderPending(Long reworkOrderId) {
        MesProReworkOrderDO reworkOrder = reworkOrderService.validateReworkOrderExists(reworkOrderId);
        if (!reworkOrder.getStatus().equals(PENDING.getStatus())) {
            throw exception(PRO_REWORK_ORDER_STATUS_INVALID);
        }
    }

}
