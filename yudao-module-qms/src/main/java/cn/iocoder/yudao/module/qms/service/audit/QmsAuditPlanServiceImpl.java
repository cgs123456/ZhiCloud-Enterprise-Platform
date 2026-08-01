package cn.iocoder.yudao.module.qms.service.audit;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.audit.vo.QmsAuditPlanAuditorSaveReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.audit.vo.QmsAuditPlanPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.audit.vo.QmsAuditPlanSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.audit.QmsAuditPlanAuditorDO;
import cn.iocoder.yudao.module.qms.dal.dataobject.audit.QmsAuditPlanDO;
import cn.iocoder.yudao.module.qms.dal.mysql.audit.QmsAuditPlanAuditorMapper;
import cn.iocoder.yudao.module.qms.dal.mysql.audit.QmsAuditPlanMapper;
import cn.iocoder.yudao.module.qms.enums.audit.QmsAuditPlanStatusEnum;
import cn.iocoder.yudao.module.qms.enums.audit.QmsAuditorRoleEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.AUDIT_PLAN_NO_DUPLICATE;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.AUDIT_PLAN_NOT_EXISTS;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.AUDIT_PLAN_STATUS_INVALID;

/**
 * QMS 审核计划 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class QmsAuditPlanServiceImpl implements QmsAuditPlanService {

    @Resource
    private QmsAuditPlanMapper auditPlanMapper;

    @Resource
    private QmsAuditPlanAuditorMapper auditorMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAuditPlan(QmsAuditPlanSaveReqVO createReqVO) {
        // 校验计划编号唯一
        validatePlanNoUnique(null, createReqVO.getPlanNo());
        // 插入
        QmsAuditPlanDO auditPlan = BeanUtils.toBean(createReqVO, QmsAuditPlanDO.class);
        // 默认状态为已计划
        if (auditPlan.getStatus() == null) {
            auditPlan.setStatus(QmsAuditPlanStatusEnum.PLANNED.getStatus());
        }
        auditPlanMapper.insert(auditPlan);
        return auditPlan.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAuditPlan(QmsAuditPlanSaveReqVO updateReqVO) {
        // 校验存在
        validateAuditPlanExists(updateReqVO.getId());
        // 校验计划编号唯一
        validatePlanNoUnique(updateReqVO.getId(), updateReqVO.getPlanNo());
        // 更新
        QmsAuditPlanDO updateObj = BeanUtils.toBean(updateReqVO, QmsAuditPlanDO.class);
        // 禁止通过通用更新修改状态，状态变更必须走 executeAuditPlan/completeAuditPlan/cancelAuditPlan 等状态流转方法
        updateObj.setStatus(null);
        auditPlanMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAuditPlan(Long id) {
        // 校验存在
        validateAuditPlanExists(id);
        // 删除计划
        auditPlanMapper.deleteById(id);
        // 删除关联的审核组成员
        List<QmsAuditPlanAuditorDO> auditors = auditorMapper.selectListByPlanId(id);
        if (!auditors.isEmpty()) {
            auditorMapper.deleteBatchIds(auditors.stream().map(QmsAuditPlanAuditorDO::getId).toList());
        }
    }

    private void validateAuditPlanExists(Long id) {
        if (auditPlanMapper.selectById(id) == null) {
            throw exception(AUDIT_PLAN_NOT_EXISTS);
        }
    }

    private void validatePlanNoUnique(Long id, String planNo) {
        QmsAuditPlanDO existing = auditPlanMapper.selectByPlanNo(planNo);
        if (existing == null) {
            return;
        }
        if (id == null || !id.equals(existing.getId())) {
            throw exception(AUDIT_PLAN_NO_DUPLICATE);
        }
    }

    @Override
    public QmsAuditPlanDO getAuditPlan(Long id) {
        return auditPlanMapper.selectById(id);
    }

    @Override
    public PageResult<QmsAuditPlanDO> getAuditPlanPage(QmsAuditPlanPageReqVO pageReqVO) {
        return auditPlanMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeAuditPlan(Long id) {
        // 1. 校验存在
        QmsAuditPlanDO auditPlan = auditPlanMapper.selectById(id);
        if (auditPlan == null) {
            throw exception(AUDIT_PLAN_NOT_EXISTS);
        }
        // 2. 校验状态：必须是已计划
        if (!QmsAuditPlanStatusEnum.PLANNED.getStatus().equals(auditPlan.getStatus())) {
            throw exception(AUDIT_PLAN_STATUS_INVALID);
        }
        // 3. 流转状态为已执行
        QmsAuditPlanDO updateObj = new QmsAuditPlanDO();
        updateObj.setId(id);
        updateObj.setStatus(QmsAuditPlanStatusEnum.EXECUTING.getStatus());
        auditPlanMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeAuditPlan(Long id) {
        // 1. 校验存在
        QmsAuditPlanDO auditPlan = auditPlanMapper.selectById(id);
        if (auditPlan == null) {
            throw exception(AUDIT_PLAN_NOT_EXISTS);
        }
        // 2. 校验状态：必须是已执行
        if (!QmsAuditPlanStatusEnum.EXECUTING.getStatus().equals(auditPlan.getStatus())) {
            throw exception(AUDIT_PLAN_STATUS_INVALID);
        }
        // 3. 流转状态为已完成
        QmsAuditPlanDO updateObj = new QmsAuditPlanDO();
        updateObj.setId(id);
        updateObj.setStatus(QmsAuditPlanStatusEnum.COMPLETED.getStatus());
        auditPlanMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelAuditPlan(Long id) {
        // 1. 校验存在
        QmsAuditPlanDO auditPlan = auditPlanMapper.selectById(id);
        if (auditPlan == null) {
            throw exception(AUDIT_PLAN_NOT_EXISTS);
        }
        // 2. 校验状态：必须是已计划或已执行
        Integer status = auditPlan.getStatus();
        if (!QmsAuditPlanStatusEnum.PLANNED.getStatus().equals(status)
                && !QmsAuditPlanStatusEnum.EXECUTING.getStatus().equals(status)) {
            throw exception(AUDIT_PLAN_STATUS_INVALID);
        }
        // 3. 流转状态为已取消
        QmsAuditPlanDO updateObj = new QmsAuditPlanDO();
        updateObj.setId(id);
        updateObj.setStatus(QmsAuditPlanStatusEnum.CANCELLED.getStatus());
        auditPlanMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addAuditor(QmsAuditPlanAuditorSaveReqVO reqVO) {
        // 校验计划存在
        validateAuditPlanExists(reqVO.getPlanId());
        // 插入成员
        QmsAuditPlanAuditorDO auditor = BeanUtils.toBean(reqVO, QmsAuditPlanAuditorDO.class);
        if (auditor.getRole() == null) {
            auditor.setRole(QmsAuditorRoleEnum.AUDITOR.getRole());
        }
        auditorMapper.insert(auditor);
        // 若角色为主审，同步到审核计划的 leadAuditorId
        if (QmsAuditorRoleEnum.LEAD_AUDITOR.getRole().equals(auditor.getRole())) {
            QmsAuditPlanDO updatePlan = new QmsAuditPlanDO();
            updatePlan.setId(reqVO.getPlanId());
            updatePlan.setLeadAuditorId(auditor.getAuditorId());
            auditPlanMapper.updateById(updatePlan);
        }
        return auditor.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAuditor(QmsAuditPlanAuditorSaveReqVO reqVO) {
        // 校验存在
        if (auditorMapper.selectById(reqVO.getId()) == null) {
            throw exception(AUDIT_PLAN_NOT_EXISTS);
        }
        // 更新
        QmsAuditPlanAuditorDO updateObj = BeanUtils.toBean(reqVO, QmsAuditPlanAuditorDO.class);
        auditorMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAuditor(Long id) {
        // 校验存在
        if (auditorMapper.selectById(id) == null) {
            throw exception(AUDIT_PLAN_NOT_EXISTS);
        }
        // 删除
        auditorMapper.deleteById(id);
    }

    @Override
    public List<QmsAuditPlanAuditorDO> getAuditorListByPlanId(Long planId) {
        return auditorMapper.selectListByPlanId(planId);
    }

}
