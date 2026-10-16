package cn.zhicloud.module.qms.service.complaint;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.qms.controller.admin.complaint.vo.CustomerComplaintSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.complaint.CustomerComplaintDO;
import cn.zhicloud.module.qms.dal.mysql.complaint.CustomerComplaintMapper;
import cn.zhicloud.module.qms.enums.qms.ComplaintStatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §12.235.5（critical）：客户投诉建单状态护栏测试。
 *
 * <p>攻击形态：建单直接传 CLOSED(40)，造出一份「从未登记、从未调查、从未处理」
 * 却已关闭的投诉记录——8D 关联与整改措施都无从谈起。
 *
 * @author 智云
 */
@Import(CustomerComplaintServiceImpl.class)
public class CustomerComplaintStatusGuardTest extends BaseDbUnitTest {

    @Resource
    private CustomerComplaintServiceImpl complaintService;

    @Resource
    private CustomerComplaintMapper complaintMapper;

    @Test
    public void test_createCustomerComplaint_ignoresSuppliedClosedStatus() {
        // 攻击形态：建单即关闭（跳过登记/调查/处理全链路）（§12.235.5）
        CustomerComplaintSaveReqVO req = new CustomerComplaintSaveReqVO();
        req.setComplaintNo("CP-BYPASS-CLOSED-001");
        req.setComplaintContent("外观不良");
        req.setStatus(ComplaintStatusEnum.CLOSED.getStatus());

        Long id = complaintService.createCustomerComplaint(req);

        CustomerComplaintDO saved = complaintMapper.selectById(id);
        assertEquals(ComplaintStatusEnum.REGISTERED.getStatus(), saved.getStatus());
    }

    @Test
    public void test_createCustomerComplaint_ignoresSuppliedHandlingStatus() {
        // 对称检查：任何非 REGISTERED 状态都不生效（§12.235.5）
        CustomerComplaintSaveReqVO req = new CustomerComplaintSaveReqVO();
        req.setComplaintNo("CP-BYPASS-HANDLING-001");
        req.setComplaintContent("尺寸超差");
        req.setStatus(ComplaintStatusEnum.HANDLING.getStatus());

        Long id = complaintService.createCustomerComplaint(req);

        CustomerComplaintDO saved = complaintMapper.selectById(id);
        assertEquals(ComplaintStatusEnum.REGISTERED.getStatus(), saved.getStatus());
    }
}
