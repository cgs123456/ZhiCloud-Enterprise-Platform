package cn.iocoder.yudao.module.qms.service.audit;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.audit.vo.QmsAuditReportPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.audit.vo.QmsAuditReportSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.audit.QmsAuditNonconformityDO;
import cn.iocoder.yudao.module.qms.dal.dataobject.audit.QmsAuditReportDO;
import cn.iocoder.yudao.module.qms.dal.mysql.audit.QmsAuditNonconformityMapper;
import cn.iocoder.yudao.module.qms.dal.mysql.audit.QmsAuditReportMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.AUDIT_PLAN_NOT_EXISTS;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.AUDIT_REPORT_NOT_EXISTS;

/**
 * QMS 审核报告 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class QmsAuditReportServiceImpl implements QmsAuditReportService {

    @Resource
    private QmsAuditReportMapper auditReportMapper;

    @Resource
    private QmsAuditNonconformityMapper nonconformityMapper;

    @Resource
    private QmsAuditPlanService auditPlanService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAuditReport(QmsAuditReportSaveReqVO createReqVO) {
        // 校验审核计划存在
        if (auditPlanService.getAuditPlan(createReqVO.getPlanId()) == null) {
            throw exception(AUDIT_PLAN_NOT_EXISTS);
        }
        // 汇总不符合项数（新建时默认 0，待不符合项创建后由 NonconformityService 自动刷新）
        // 插入
        QmsAuditReportDO auditReport = BeanUtils.toBean(createReqVO, QmsAuditReportDO.class);
        if (auditReport.getIssueCount() == null) {
            auditReport.setIssueCount(0);
        }
        auditReportMapper.insert(auditReport);
        return auditReport.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAuditReport(QmsAuditReportSaveReqVO updateReqVO) {
        // 校验存在
        validateAuditReportExists(updateReqVO.getId());
        // 更新
        QmsAuditReportDO updateObj = BeanUtils.toBean(updateReqVO, QmsAuditReportDO.class);
        auditReportMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAuditReport(Long id) {
        // 校验存在
        validateAuditReportExists(id);
        // 删除报告
        auditReportMapper.deleteById(id);
    }

    private void validateAuditReportExists(Long id) {
        if (auditReportMapper.selectById(id) == null) {
            throw exception(AUDIT_REPORT_NOT_EXISTS);
        }
    }

    @Override
    public QmsAuditReportDO getAuditReport(Long id) {
        return auditReportMapper.selectById(id);
    }

    @Override
    public PageResult<QmsAuditReportDO> getAuditReportPage(QmsAuditReportPageReqVO pageReqVO) {
        return auditReportMapper.selectPage(pageReqVO);
    }

    /**
     * 刷新审核报告的不符合项数（供 NonconformityService 调用）
     *
     * @param reportId 报告 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void refreshIssueCount(Long reportId) {
        QmsAuditReportDO report = auditReportMapper.selectById(reportId);
        if (report == null) {
            return;
        }
        List<QmsAuditNonconformityDO> list = nonconformityMapper.selectListByReportId(reportId);
        QmsAuditReportDO updateObj = new QmsAuditReportDO();
        updateObj.setId(reportId);
        updateObj.setIssueCount(list.size());
        auditReportMapper.updateById(updateObj);
    }

}
