package cn.zhicloud.module.qms.service.sqm;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.qms.controller.admin.sqm.vo.SupplierAuditSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.sqm.SupplierAuditDO;
import cn.zhicloud.module.qms.dal.mysql.sqm.SupplierAuditMapper;
import cn.zhicloud.module.qms.enums.qms.SupplierAuditStatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomLongId;
import static org.junit.jupiter.api.Assertions.*;

/**
 * §12.235.5（critical）：供应商审核建单状态护栏测试。
 *
 * <p>攻击形态：建单直接传 COMPLETED(30)，造出一份「无实际审核日期、无审核报告」
 * 却已完成的供应商审核——供应商准入/年度评价的证据凭空产生。
 *
 * @author 智云
 */
@Import(SupplierAuditServiceImpl.class)
public class SupplierAuditStatusGuardTest extends BaseDbUnitTest {

    @Resource
    private SupplierAuditServiceImpl supplierAuditService;

    @Resource
    private SupplierAuditMapper supplierAuditMapper;

    @Test
    public void test_createSupplierAudit_ignoresSuppliedCompletedStatus() {
        // 攻击形态：建单即完成（跳过现场审核）（§12.235.5）
        SupplierAuditSaveReqVO req = new SupplierAuditSaveReqVO();
        req.setAuditNo("SA-BYPASS-COMPLETED-001");
        req.setAuditName("XX 供应商 2024 年度审核");
        req.setSupplierId(randomLongId());
        req.setStatus(SupplierAuditStatusEnum.COMPLETED.getStatus());

        Long id = supplierAuditService.createSupplierAudit(req);

        SupplierAuditDO saved = supplierAuditMapper.selectById(id);
        assertEquals(SupplierAuditStatusEnum.PLANNED.getStatus(), saved.getStatus());
    }

    @Test
    public void test_createSupplierAudit_ignoresSuppliedInProgressStatus() {
        // 对称检查：任何非 PLANNED 状态都不生效（§12.235.5）
        SupplierAuditSaveReqVO req = new SupplierAuditSaveReqVO();
        req.setAuditNo("SA-BYPASS-INPROGRESS-001");
        req.setAuditName("XX 供应商专项审核");
        req.setSupplierId(randomLongId());
        req.setStatus(SupplierAuditStatusEnum.IN_PROGRESS.getStatus());

        Long id = supplierAuditService.createSupplierAudit(req);

        SupplierAuditDO saved = supplierAuditMapper.selectById(id);
        assertEquals(SupplierAuditStatusEnum.PLANNED.getStatus(), saved.getStatus());
    }
}
