package cn.zhicloud.module.hr.service.leave;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.zhicloud.module.bpm.api.task.BpmProcessInstanceApi;
import cn.zhicloud.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.zhicloud.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.zhicloud.module.hr.dal.dataobject.leave.HrLeaveBalanceDO;
import cn.zhicloud.module.hr.dal.dataobject.leave.HrLeaveRequestDO;
import cn.zhicloud.module.hr.dal.dataobject.leave.HrLeaveTypeDO;
import cn.zhicloud.module.hr.dal.mysql.leave.HrLeaveBalanceMapper;
import cn.zhicloud.module.hr.dal.mysql.leave.HrLeaveRequestMapper;
import cn.zhicloud.module.hr.dal.mysql.leave.HrLeaveTypeMapper;
import cn.zhicloud.module.hr.enums.leave.HrLeaveStatusEnum;
import cn.zhicloud.module.hr.service.leave.listener.HrLeaveRequestStatusListener;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.hr.enums.ErrorCodeConstants.HR_LEAVE_REQUEST_ALREADY_SUBMITTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 请假「提交 BPM → 审批结果回写 → 余额扣减」闭环的单元测试。
 *
 * <p>用真实 H2 + 真实 Mapper，只 mock BPM 的发起接口：
 * 要证明的是假单状态、流程编号与带薪余额这三件事在一条链上同时对得上。
 *
 * @author 智云
 */
@Import({HrLeaveRequestServiceImpl.class, HrLeaveRequestStatusListener.class})
public class HrLeaveRequestBpmTest extends BaseDbUnitTest {

    @Resource
    private HrLeaveRequestServiceImpl leaveRequestService;
    @Resource
    private HrLeaveRequestStatusListener listener;
    @Resource
    private HrLeaveRequestMapper leaveRequestMapper;
    @Resource
    private HrLeaveTypeMapper leaveTypeMapper;
    @Resource
    private HrLeaveBalanceMapper leaveBalanceMapper;

    @MockitoBean
    private BpmProcessInstanceApi processInstanceApi;

    private static final Long EMPLOYEE_ID = 9001L;
    private static final Long PAID_TYPE_ID = 8001L;

    /**
     * 造一张待审批的带薪假单（3 天），并配好假期类型与余额。
     */
    private HrLeaveRequestDO prepareLeave(Long requestId) {
        HrLeaveTypeDO type = new HrLeaveTypeDO();
        type.setId(PAID_TYPE_ID);
        type.setName("年假");
        type.setIsPaid(1);
        leaveTypeMapper.insert(type);

        HrLeaveBalanceDO balance = new HrLeaveBalanceDO();
        balance.setId(7001L);
        balance.setEmployeeId(EMPLOYEE_ID);
        balance.setLeaveTypeId(PAID_TYPE_ID);
        balance.setYear(LocalDate.now().getYear());
        balance.setTotalDays(new BigDecimal("10"));
        balance.setUsedDays(BigDecimal.ZERO);
        balance.setRemainingDays(new BigDecimal("10"));
        leaveBalanceMapper.insert(balance);

        HrLeaveRequestDO request = new HrLeaveRequestDO();
        request.setId(requestId);
        request.setEmployeeId(EMPLOYEE_ID);
        request.setLeaveTypeId(PAID_TYPE_ID);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));
        request.setDays(new BigDecimal("3"));
        request.setReason("家事");
        request.setStatus(HrLeaveStatusEnum.PENDING.getStatus());
        leaveRequestMapper.insert(request);
        return request;
    }

    private BpmProcessInstanceStatusEvent event(Long requestId, Integer bpmStatus) {
        BpmProcessInstanceStatusEvent event = new BpmProcessInstanceStatusEvent(this);
        event.setId("pi-" + requestId);
        event.setProcessDefinitionKey(HrLeaveRequestServiceImpl.PROCESS_KEY);
        event.setBusinessKey(String.valueOf(requestId));
        event.setStatus(bpmStatus);
        return event;
    }

    @Test
    public void testSubmit_backfillsProcessInstanceId() {
        prepareLeave(5001L);
        when(processInstanceApi.createProcessInstance(nullable(Long.class), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("pi-5001");

        leaveRequestService.submitLeaveRequest(5001L);

        HrLeaveRequestDO db = leaveRequestMapper.selectById(5001L);
        assertEquals("pi-5001", db.getProcessInstanceId());
        // 状态仍是待审批：BPM 的 RUNNING 不应该把单据推成别的状态
        assertEquals(HrLeaveStatusEnum.PENDING.getStatus(), db.getStatus());
    }

    @Test
    public void testSubmit_twiceRejected() {
        prepareLeave(5002L);
        when(processInstanceApi.createProcessInstance(nullable(Long.class), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("pi-5002");
        leaveRequestService.submitLeaveRequest(5002L);

        // 重复提交会在引擎里建出第二条在途流程，同一张假单可能被批两遍
        assertServiceException(() -> leaveRequestService.submitLeaveRequest(5002L),
                HR_LEAVE_REQUEST_ALREADY_SUBMITTED);
    }

    @Test
    public void testApproveEvent_approvesAndDeductsBalance() {
        prepareLeave(5003L);

        listener.onApplicationEvent(event(5003L, BpmProcessInstanceStatusEnum.APPROVE.getStatus()));

        HrLeaveRequestDO db = leaveRequestMapper.selectById(5003L);
        assertEquals(HrLeaveStatusEnum.APPROVED.getStatus(), db.getStatus());
        assertEquals("pi-5003", db.getProcessInstanceId());
        HrLeaveBalanceDO balance = leaveBalanceMapper.selectByEmployeeAndTypeAndYear(
                EMPLOYEE_ID, PAID_TYPE_ID, LocalDate.now().getYear());
        assertEquals(0, new BigDecimal("3").compareTo(balance.getUsedDays()));
        assertEquals(0, new BigDecimal("7").compareTo(balance.getRemainingDays()));
    }

    @Test
    public void testApproveEvent_deliveredTwice_deductsOnce() {
        prepareLeave(5004L);

        listener.onApplicationEvent(event(5004L, BpmProcessInstanceStatusEnum.APPROVE.getStatus()));
        listener.onApplicationEvent(event(5004L, BpmProcessInstanceStatusEnum.APPROVE.getStatus()));

        // BPM 结果事件是「至少一次」投递，重复投递不能再扣一次余额
        HrLeaveBalanceDO balance = leaveBalanceMapper.selectByEmployeeAndTypeAndYear(
                EMPLOYEE_ID, PAID_TYPE_ID, LocalDate.now().getYear());
        assertEquals(0, new BigDecimal("3").compareTo(balance.getUsedDays()));
    }

    @Test
    public void testRejectEvent_keepsBalance() {
        prepareLeave(5005L);

        listener.onApplicationEvent(event(5005L, BpmProcessInstanceStatusEnum.REJECT.getStatus()));

        assertEquals(HrLeaveStatusEnum.REJECTED.getStatus(), leaveRequestMapper.selectById(5005L).getStatus());
        HrLeaveBalanceDO balance = leaveBalanceMapper.selectByEmployeeAndTypeAndYear(
                EMPLOYEE_ID, PAID_TYPE_ID, LocalDate.now().getYear());
        assertEquals(0, BigDecimal.ZERO.compareTo(balance.getUsedDays()));
    }

    @Test
    public void testRunningEvent_changesNothing() {
        prepareLeave(5006L);

        listener.onApplicationEvent(event(5006L, BpmProcessInstanceStatusEnum.RUNNING.getStatus()));

        HrLeaveRequestDO db = leaveRequestMapper.selectById(5006L);
        assertEquals(HrLeaveStatusEnum.PENDING.getStatus(), db.getStatus());
        assertEquals(0, new BigDecimal("10").compareTo(
                leaveBalanceMapper.selectByEmployeeAndTypeAndYear(EMPLOYEE_ID, PAID_TYPE_ID,
                        LocalDate.now().getYear()).getRemainingDays()));
    }

    @Test
    public void testEvent_fromOtherProcessKey_isIgnored() {
        prepareLeave(5007L);
        BpmProcessInstanceStatusEvent other = new BpmProcessInstanceStatusEvent(this);
        other.setId("pi-other");
        other.setProcessDefinitionKey("oa_reimburse");
        other.setBusinessKey("5007");
        other.setStatus(BpmProcessInstanceStatusEnum.APPROVE.getStatus());

        listener.onApplicationEvent(other);

        assertEquals(HrLeaveStatusEnum.PENDING.getStatus(), leaveRequestMapper.selectById(5007L).getStatus());
    }

}
