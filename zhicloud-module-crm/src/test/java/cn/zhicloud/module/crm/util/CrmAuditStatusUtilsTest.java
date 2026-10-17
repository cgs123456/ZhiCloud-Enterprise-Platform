package cn.zhicloud.module.crm.util;

import cn.zhicloud.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.zhicloud.module.crm.enums.common.CrmAuditStatusEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link CrmAuditStatusUtils} 的单元测试：BPM 审批结果到 CRM 审批状态的映射
 *
 * <p>覆盖 §12.261 D6：撤回（BPM CANCEL=4）必须映射为 CRM 已取消(40)，不允许回落到非枚举值 4。
 */
public class CrmAuditStatusUtilsTest {

    @Test
    public void test_convertBpmResultToAuditStatus_approve() {
        assertEquals(CrmAuditStatusEnum.APPROVE.getStatus(),
                CrmAuditStatusUtils.convertBpmResultToAuditStatus(BpmTaskStatusEnum.APPROVE.getStatus()));
    }

    @Test
    public void test_convertBpmResultToAuditStatus_reject() {
        assertEquals(CrmAuditStatusEnum.REJECT.getStatus(),
                CrmAuditStatusUtils.convertBpmResultToAuditStatus(BpmTaskStatusEnum.REJECT.getStatus()));
    }

    @Test
    public void test_convertBpmResultToAuditStatus_cancelIsForty() {
        // 钉死修复：撤回映射为 40（曾错误映射为非枚举值 4）
        assertEquals(CrmAuditStatusEnum.CANCEL.getStatus(),
                CrmAuditStatusUtils.convertBpmResultToAuditStatus(BpmTaskStatusEnum.CANCEL.getStatus()));
    }

    @Test
    public void test_convertBpmResultToAuditStatus_unknownThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> CrmAuditStatusUtils.convertBpmResultToAuditStatus(99));
    }

}
