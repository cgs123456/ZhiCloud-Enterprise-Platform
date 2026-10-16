package cn.zhicloud.module.qms.service.audit;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.qms.controller.admin.audit.vo.QmsAuditPlanSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.audit.QmsAuditPlanDO;
import cn.zhicloud.module.qms.dal.mysql.audit.QmsAuditPlanMapper;
import cn.zhicloud.module.qms.enums.audit.QmsAuditPlanStatusEnum;
import cn.zhicloud.module.qms.enums.audit.QmsAuditTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §12.235.5（critical）：审核计划建单状态护栏测试。
 *
 * <p>攻击形态：建单直接传 COMPLETED(30)，造出一份「未执行、无审核报告、
 * 无不符合项」却已完成的审核计划——审核证据链凭空产生。
 *
 * @author 智云
 */
@Import(QmsAuditPlanServiceImpl.class)
public class QmsAuditPlanStatusGuardTest extends BaseDbUnitTest {

    @Resource
    private QmsAuditPlanServiceImpl auditPlanService;

    @Resource
    private QmsAuditPlanMapper auditPlanMapper;

    @Test
    public void test_createAuditPlan_ignoresSuppliedCompletedStatus() {
        // 攻击形态：建单即完成（跳过执行与报告）（§12.235.5）
        QmsAuditPlanSaveReqVO req = new QmsAuditPlanSaveReqVO();
        req.setPlanNo("AUDIT-BYPASS-COMPLETED-001");
        req.setAuditType(QmsAuditTypeEnum.INTERNAL.getAuditType());
        req.setTitle("2024 年度内审");
        req.setStatus(QmsAuditPlanStatusEnum.COMPLETED.getStatus());

        Long id = auditPlanService.createAuditPlan(req);

        QmsAuditPlanDO saved = auditPlanMapper.selectById(id);
        assertEquals(QmsAuditPlanStatusEnum.PLANNED.getStatus(), saved.getStatus());
    }

    @Test
    public void test_createAuditPlan_ignoresSuppliedExecutingStatus() {
        // 对称检查：任何非 PLANNED 状态都不生效（§12.235.5）
        QmsAuditPlanSaveReqVO req = new QmsAuditPlanSaveReqVO();
        req.setPlanNo("AUDIT-BYPASS-EXECUTING-001");
        req.setAuditType(QmsAuditTypeEnum.INTERNAL.getAuditType());
        req.setTitle("2024 年度内审（第二组）");
        req.setStatus(QmsAuditPlanStatusEnum.EXECUTING.getStatus());

        Long id = auditPlanService.createAuditPlan(req);

        QmsAuditPlanDO saved = auditPlanMapper.selectById(id);
        assertEquals(QmsAuditPlanStatusEnum.PLANNED.getStatus(), saved.getStatus());
    }
}
