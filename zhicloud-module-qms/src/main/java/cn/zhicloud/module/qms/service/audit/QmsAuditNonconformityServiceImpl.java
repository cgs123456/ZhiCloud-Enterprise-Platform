package cn.zhicloud.module.qms.service.audit;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.qms.controller.admin.audit.vo.QmsAuditNonconformityPageReqVO;
import cn.zhicloud.module.qms.controller.admin.audit.vo.QmsAuditNonconformityRectifyReqVO;
import cn.zhicloud.module.qms.controller.admin.audit.vo.QmsAuditNonconformitySaveReqVO;
import cn.zhicloud.module.qms.controller.admin.audit.vo.QmsAuditNonconformityVerifyReqVO;
import cn.zhicloud.module.qms.dal.dataobject.audit.QmsAuditNonconformityDO;
import cn.zhicloud.module.qms.dal.dataobject.audit.QmsAuditReportDO;
import cn.zhicloud.module.qms.dal.mysql.audit.QmsAuditNonconformityMapper;
import cn.zhicloud.module.qms.dal.mysql.audit.QmsAuditReportMapper;
import cn.zhicloud.module.qms.enums.audit.QmsNcStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.AUDIT_NC_STATUS_INVALID;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.AUDIT_NONCONFORMITY_NOT_EXISTS;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.AUDIT_REPORT_NOT_EXISTS;

/**
 * QMS 审核不符合项 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class QmsAuditNonconformityServiceImpl implements QmsAuditNonconformityService {

    @Resource
    private QmsAuditNonconformityMapper nonconformityMapper;

    @Resource
    private QmsAuditReportMapper auditReportMapper;

    @Resource
    @Lazy
    private QmsAuditReportServiceImpl auditReportServiceImpl;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createNonconformity(QmsAuditNonconformitySaveReqVO createReqVO) {
        // 校验审核报告存在
        QmsAuditReportDO report = auditReportMapper.selectById(createReqVO.getReportId());
        if (report == null) {
            throw exception(AUDIT_REPORT_NOT_EXISTS);
        }
        // 插入不符合项
        QmsAuditNonconformityDO nonconformity = BeanUtils.toBean(createReqVO, QmsAuditNonconformityDO.class);
        if (nonconformity.getStatus() == null) {
            nonconformity.setStatus(QmsNcStatusEnum.PENDING.getStatus());
        }
        nonconformityMapper.insert(nonconformity);
        // 刷新审核报告的不符合项数
        auditReportServiceImpl.refreshIssueCount(createReqVO.getReportId());
        return nonconformity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNonconformity(QmsAuditNonconformitySaveReqVO updateReqVO) {
        // 校验存在
        QmsAuditNonconformityDO existing = nonconformityMapper.selectById(updateReqVO.getId());
        if (existing == null) {
            throw exception(AUDIT_NONCONFORMITY_NOT_EXISTS);
        }
        // 更新（P0 修复：屏蔽 status 字段，状态变更必须走 rectify/verify/close 专门方法）
        QmsAuditNonconformityDO updateObj = BeanUtils.toBean(updateReqVO, QmsAuditNonconformityDO.class);
        updateObj.setStatus(null);
        nonconformityMapper.updateById(updateObj);
        // 若关联的报告 ID 发生变化，刷新两份报告的 issueCount
        if (existing.getReportId() != null && !existing.getReportId().equals(updateReqVO.getReportId())) {
            auditReportServiceImpl.refreshIssueCount(existing.getReportId());
            if (updateReqVO.getReportId() != null) {
                auditReportServiceImpl.refreshIssueCount(updateReqVO.getReportId());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNonconformity(Long id) {
        // 校验存在
        QmsAuditNonconformityDO existing = nonconformityMapper.selectById(id);
        if (existing == null) {
            throw exception(AUDIT_NONCONFORMITY_NOT_EXISTS);
        }
        // 删除
        nonconformityMapper.deleteById(id);
        // 刷新审核报告的不符合项数
        if (existing.getReportId() != null) {
            auditReportServiceImpl.refreshIssueCount(existing.getReportId());
        }
    }

    @Override
    public QmsAuditNonconformityDO getNonconformity(Long id) {
        return nonconformityMapper.selectById(id);
    }

    @Override
    public PageResult<QmsAuditNonconformityDO> getNonconformityPage(QmsAuditNonconformityPageReqVO pageReqVO) {
        return nonconformityMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rectifyNonconformity(QmsAuditNonconformityRectifyReqVO reqVO) {
        // 1. 校验存在
        QmsAuditNonconformityDO nonconformity = nonconformityMapper.selectById(reqVO.getId());
        if (nonconformity == null) {
            throw exception(AUDIT_NONCONFORMITY_NOT_EXISTS);
        }
        // 2. 校验状态：必须是待整改
        if (!QmsNcStatusEnum.PENDING.getStatus().equals(nonconformity.getStatus())) {
            throw exception(AUDIT_NC_STATUS_INVALID);
        }
        // 3. 流转状态为整改中，并记录整改措施到备注
        QmsAuditNonconformityDO updateObj = new QmsAuditNonconformityDO();
        updateObj.setId(reqVO.getId());
        updateObj.setStatus(QmsNcStatusEnum.RECTIFYING.getStatus());
        updateObj.setRemark(reqVO.getAction());
        nonconformityMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void verifyNonconformity(QmsAuditNonconformityVerifyReqVO reqVO) {
        // 1. 校验存在
        QmsAuditNonconformityDO nonconformity = nonconformityMapper.selectById(reqVO.getId());
        if (nonconformity == null) {
            throw exception(AUDIT_NONCONFORMITY_NOT_EXISTS);
        }
        // 2. 校验状态：必须是已整改
        if (!QmsNcStatusEnum.RECTIFIED.getStatus().equals(nonconformity.getStatus())) {
            throw exception(AUDIT_NC_STATUS_INVALID);
        }
        // 3. 流转状态为已验证，并记录验证结果到备注
        QmsAuditNonconformityDO updateObj = new QmsAuditNonconformityDO();
        updateObj.setId(reqVO.getId());
        updateObj.setStatus(QmsNcStatusEnum.VERIFIED.getStatus());
        updateObj.setRemark(reqVO.getResult());
        nonconformityMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeNonconformity(Long id) {
        // 1. 校验存在
        QmsAuditNonconformityDO nonconformity = nonconformityMapper.selectById(id);
        if (nonconformity == null) {
            throw exception(AUDIT_NONCONFORMITY_NOT_EXISTS);
        }
        // 2. 校验状态：必须是已验证
        if (!QmsNcStatusEnum.VERIFIED.getStatus().equals(nonconformity.getStatus())) {
            throw exception(AUDIT_NC_STATUS_INVALID);
        }
        // 3. 流转状态为已关闭
        QmsAuditNonconformityDO updateObj = new QmsAuditNonconformityDO();
        updateObj.setId(id);
        updateObj.setStatus(QmsNcStatusEnum.CLOSED.getStatus());
        nonconformityMapper.updateById(updateObj);
    }

    @Override
    public List<QmsAuditNonconformityDO> getNonconformityListByReportId(Long reportId) {
        return nonconformityMapper.selectListByReportId(reportId);
    }

}
