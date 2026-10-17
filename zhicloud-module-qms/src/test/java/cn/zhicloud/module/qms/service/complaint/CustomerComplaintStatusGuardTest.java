package cn.zhicloud.module.qms.service.complaint;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.qms.controller.admin.complaint.vo.CustomerComplaintSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.complaint.CustomerComplaintDO;
import cn.zhicloud.module.qms.dal.mysql.complaint.CustomerComplaintMapper;
import cn.zhicloud.module.qms.enums.qms.ComplaintStatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.CUSTOMER_COMPLAINT_NOT_CLOSEABLE;
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

    // ------------------------------------------------------------------
    // §12.253：关闭类幂等——只有处理中（HANDLING(30)）可关闭。
    //
    // 旧 closeComplaint 无状态校验：登记即关（调查/处理全跳过）、已关闭重复关
    // （closeTime 二次重写）都是放行形态。以下钉住收紧后的契约。
    // ------------------------------------------------------------------

    @Test
    public void test_closeCustomerComplaint_registeredRejected() {
        // 登记即关：调查结论与处理措施全无，拒绝（§12.253）
        CustomerComplaintSaveReqVO req = new CustomerComplaintSaveReqVO();
        req.setComplaintNo("CP-CLOSE-REG-001");
        req.setComplaintContent("外观不良");
        Long id = complaintService.createCustomerComplaint(req);

        assertServiceException(() -> complaintService.closeComplaint(id),
                CUSTOMER_COMPLAINT_NOT_CLOSEABLE);

        CustomerComplaintDO after = complaintMapper.selectById(id);
        assertEquals(ComplaintStatusEnum.REGISTERED.getStatus(), after.getStatus());
        assertNull(after.getCloseTime());
    }

    @Test
    public void test_closeCustomerComplaint_investigatingRejected() {
        // 调查未出结论不可关闭（§12.253）
        CustomerComplaintSaveReqVO req = new CustomerComplaintSaveReqVO();
        req.setComplaintNo("CP-CLOSE-INV-001");
        req.setComplaintContent("尺寸超差");
        Long id = complaintService.createCustomerComplaint(req);
        complaintService.advanceStatus(id);

        assertServiceException(() -> complaintService.closeComplaint(id),
                CUSTOMER_COMPLAINT_NOT_CLOSEABLE);

        CustomerComplaintDO after = complaintMapper.selectById(id);
        assertEquals(ComplaintStatusEnum.INVESTIGATING.getStatus(), after.getStatus());
        assertNull(after.getCloseTime());
    }

    @Test
    public void test_closeCustomerComplaint_successFromHandling_thenRepeatRejected() {
        // 只有处理中可关闭；重复关拒绝且 closeTime 保持首次关闭时刻（幂等防重写）（§12.253）
        CustomerComplaintSaveReqVO req = new CustomerComplaintSaveReqVO();
        req.setComplaintNo("CP-CLOSE-OK-001");
        req.setComplaintContent("包装破损");
        Long id = complaintService.createCustomerComplaint(req);
        complaintService.advanceStatus(id);
        complaintService.advanceStatus(id);

        complaintService.closeComplaint(id);

        CustomerComplaintDO first = complaintMapper.selectById(id);
        assertEquals(ComplaintStatusEnum.CLOSED.getStatus(), first.getStatus());
        assertNotNull(first.getCloseTime());

        assertServiceException(() -> complaintService.closeComplaint(id),
                CUSTOMER_COMPLAINT_NOT_CLOSEABLE);
        CustomerComplaintDO second = complaintMapper.selectById(id);
        assertEquals(ComplaintStatusEnum.CLOSED.getStatus(), second.getStatus());
        assertEquals(first.getCloseTime(), second.getCloseTime());
    }
}
