package cn.iocoder.yudao.module.tms.service.freight;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.tms.controller.admin.freight.vo.TmsFreightReconciliationPageReqVO;
import cn.iocoder.yudao.module.tms.controller.admin.freight.vo.TmsFreightReconciliationSaveReqVO;
import cn.iocoder.yudao.module.tms.dal.dataobject.freight.TmsFreightReconciliationDO;
import cn.iocoder.yudao.module.tms.dal.mysql.freight.TmsFreightReconciliationMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.tms.enums.ErrorCodeConstants.*;

/**
 * TMS 运费对账 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class TmsFreightReconciliationServiceImpl implements TmsFreightReconciliationService {

    @Resource
    private TmsFreightReconciliationMapper reconciliationMapper;

    @Override
    public Long createReconciliation(TmsFreightReconciliationSaveReqVO createReqVO) {
        validateNoDuplicate(null, createReqVO.getNo());
        TmsFreightReconciliationDO reconciliation = BeanUtils.toBean(createReqVO, TmsFreightReconciliationDO.class);
        reconciliation.setStatus(0); // 待对账
        reconciliationMapper.insert(reconciliation);
        return reconciliation.getId();
    }

    @Override
    public void updateReconciliation(TmsFreightReconciliationSaveReqVO updateReqVO) {
        validateReconciliationExists(updateReqVO.getId());
        validateNoDuplicate(updateReqVO.getId(), updateReqVO.getNo());
        TmsFreightReconciliationDO updateObj = BeanUtils.toBean(updateReqVO, TmsFreightReconciliationDO.class);
        reconciliationMapper.updateById(updateObj);
    }

    @Override
    public void deleteReconciliation(Long id) {
        validateReconciliationExists(id);
        reconciliationMapper.deleteById(id);
    }

    @Override
    public TmsFreightReconciliationDO getReconciliation(Long id) {
        return reconciliationMapper.selectById(id);
    }

    @Override
    public PageResult<TmsFreightReconciliationDO> getReconciliationPage(TmsFreightReconciliationPageReqVO pageReqVO) {
        return reconciliationMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void doReconcile(Long id) {
        TmsFreightReconciliationDO reconciliation = validateReconciliationExists(id);
        // 计算差异金额 = 承运商账单金额 - 系统运费总额
        BigDecimal systemAmount = reconciliation.getSystemAmount() != null ? reconciliation.getSystemAmount() : BigDecimal.ZERO;
        BigDecimal carrierAmount = reconciliation.getCarrierAmount() != null ? reconciliation.getCarrierAmount() : BigDecimal.ZERO;
        BigDecimal diffAmount = carrierAmount.subtract(systemAmount);

        // 更新状态
        TmsFreightReconciliationDO updateObj = new TmsFreightReconciliationDO();
        updateObj.setId(id);
        updateObj.setDiffAmount(diffAmount);
        // 差异为 0 → 已对账；有差异 → 有差异
        updateObj.setStatus(diffAmount.compareTo(BigDecimal.ZERO) == 0 ? 10 : 20);
        updateObj.setReconcileTime(LocalDateTime.now());
        reconciliationMapper.updateById(updateObj);
    }

    @Override
    public void confirmReconciliation(Long id) {
        TmsFreightReconciliationDO reconciliation = validateReconciliationExists(id);
        if (reconciliation.getStatus() == null || reconciliation.getStatus() < 10) {
            throw exception(TMS_FREIGHT_RECONCILIATION_NOT_RECONCILED);
        }
        TmsFreightReconciliationDO updateObj = new TmsFreightReconciliationDO();
        updateObj.setId(id);
        updateObj.setStatus(30); // 已确认
        updateObj.setConfirmTime(LocalDateTime.now());
        reconciliationMapper.updateById(updateObj);
    }

    @Override
    public void rejectReconciliation(Long id, String reason) {
        validateReconciliationExists(id);
        TmsFreightReconciliationDO updateObj = new TmsFreightReconciliationDO();
        updateObj.setId(id);
        updateObj.setStatus(40); // 已驳回
        updateObj.setRemark(reason);
        reconciliationMapper.updateById(updateObj);
    }

    private void validateNoDuplicate(Long id, String no) {
        TmsFreightReconciliationDO existing = reconciliationMapper.selectByNo(no);
        if (existing != null && !existing.getId().equals(id)) {
            throw exception(TMS_FREIGHT_RECONCILIATION_NO_DUPLICATE);
        }
    }

    private TmsFreightReconciliationDO validateReconciliationExists(Long id) {
        TmsFreightReconciliationDO reconciliation = reconciliationMapper.selectById(id);
        if (reconciliation == null) {
            throw exception(TMS_FREIGHT_RECONCILIATION_NOT_EXISTS);
        }
        return reconciliation;
    }

}
