package cn.zhicloud.module.hr.service.leave;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.hr.controller.admin.leave.vo.HrLeaveRequestApproveReqVO;
import cn.zhicloud.module.hr.controller.admin.leave.vo.HrLeaveRequestPageReqVO;
import cn.zhicloud.module.hr.controller.admin.leave.vo.HrLeaveRequestSaveReqVO;
import cn.zhicloud.module.hr.dal.dataobject.leave.HrLeaveBalanceDO;
import cn.zhicloud.module.hr.dal.dataobject.leave.HrLeaveRequestDO;
import cn.zhicloud.module.hr.dal.dataobject.leave.HrLeaveTypeDO;
import cn.zhicloud.module.hr.dal.mysql.leave.HrLeaveBalanceMapper;
import cn.zhicloud.module.hr.dal.mysql.leave.HrLeaveRequestMapper;
import cn.zhicloud.module.hr.dal.mysql.leave.HrLeaveTypeMapper;
import cn.zhicloud.module.hr.enums.leave.HrLeaveStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.hr.enums.ErrorCodeConstants.*;
import cn.zhicloud.module.bpm.api.task.BpmProcessInstanceApi;
import cn.zhicloud.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.zhicloud.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.zhicloud.framework.security.core.util.SecurityFrameworkUtils;
import java.util.HashMap;
import java.util.Map;

@Service
@Validated
public class HrLeaveRequestServiceImpl implements HrLeaveRequestService {

    @Resource
    private HrLeaveRequestMapper leaveRequestMapper;
    @Resource
    private HrLeaveBalanceMapper leaveBalanceMapper;
    @Resource
    private HrLeaveTypeMapper leaveTypeMapper;
    /**
     * BPM 流程实例 API：请假审批从 HR 内部手写状态改为走流程引擎。
     * 仅依赖 bpm 的 api 包（P1-3 模块边界治理）。
     */
    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    /**
     * 请假审批对应的流程定义 KEY
     */
    public static final String PROCESS_KEY = "hr_leave_request";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createLeaveRequest(HrLeaveRequestSaveReqVO createReqVO) {
        HrLeaveTypeDO leaveType = leaveTypeMapper.selectById(createReqVO.getLeaveTypeId());
        if (leaveType == null) {
            throw exception(HR_LEAVE_TYPE_NOT_EXISTS);
        }
        // 带薪假期需校验余额
        if (leaveType.getIsPaid() != null && leaveType.getIsPaid() == 1) {
            validateBalanceEnough(createReqVO.getEmployeeId(), createReqVO.getLeaveTypeId(),
                    LocalDate.now().getYear(), createReqVO.getDays());
        }
        HrLeaveRequestDO leaveRequest = BeanUtils.toBean(createReqVO, HrLeaveRequestDO.class);
        leaveRequest.setStatus(HrLeaveStatusEnum.PENDING.getStatus());
        leaveRequestMapper.insert(leaveRequest);
        return leaveRequest.getId();
    }

    private HrLeaveBalanceDO validateBalanceEnough(Long employeeId, Long leaveTypeId, Integer year, BigDecimal days) {
        HrLeaveBalanceDO balance = leaveBalanceMapper.selectByEmployeeAndTypeAndYear(employeeId, leaveTypeId, year);
        if (balance == null) {
            throw exception(HR_LEAVE_BALANCE_NOT_EXISTS);
        }
        BigDecimal remaining = balance.getRemainingDays() != null
                ? balance.getRemainingDays() : balance.getTotalDays().subtract(balance.getUsedDays());
        if (remaining.compareTo(days) < 0) {
            throw exception(HR_LEAVE_BALANCE_NOT_ENOUGH);
        }
        return balance;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveLeaveRequest(HrLeaveRequestApproveReqVO reqVO) {
        HrLeaveRequestDO leaveRequest = validateExists(reqVO.getId());
        if (!HrLeaveStatusEnum.PENDING.getStatus().equals(leaveRequest.getStatus())) {
            throw exception(HR_LEAVE_REQUEST_STATUS_INVALID);
        }
        HrLeaveRequestDO updateObj = new HrLeaveRequestDO();
        updateObj.setId(reqVO.getId());
        updateObj.setApproveTime(LocalDateTime.now());
        updateObj.setApproveRemark(reqVO.getApproveRemark());
        if (Boolean.TRUE.equals(reqVO.getApproved())) {
            updateObj.setStatus(HrLeaveStatusEnum.APPROVED.getStatus());
            // 批准时扣减带薪假期余额
            HrLeaveTypeDO leaveType = leaveTypeMapper.selectById(leaveRequest.getLeaveTypeId());
            if (leaveType != null && leaveType.getIsPaid() != null && leaveType.getIsPaid() == 1) {
                deductBalance(leaveRequest.getEmployeeId(), leaveRequest.getLeaveTypeId(),
                        leaveRequest.getStartDate().getYear(), leaveRequest.getDays());
            }
        } else {
            updateObj.setStatus(HrLeaveStatusEnum.REJECTED.getStatus());
        }
        leaveRequestMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitLeaveRequest(Long id) {
        HrLeaveRequestDO leaveRequest = validateExists(id);
        if (!HrLeaveStatusEnum.PENDING.getStatus().equals(leaveRequest.getStatus())) {
            throw exception(HR_LEAVE_REQUEST_STATUS_INVALID);
        }
        if (leaveRequest.getProcessInstanceId() != null) {
            // 已经有流程实例还再提交一次，会在引擎里建出第二条在途流程：同一张假单被批两遍
            throw exception(HR_LEAVE_REQUEST_ALREADY_SUBMITTED);
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("leaveTypeId", leaveRequest.getLeaveTypeId());
        variables.put("days", leaveRequest.getDays());
        String processInstanceId = processInstanceApi.createProcessInstance(
                SecurityFrameworkUtils.getLoginUserId(),
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(variables).setBusinessKey(String.valueOf(id)));
        HrLeaveRequestDO updateObj = new HrLeaveRequestDO();
        updateObj.setId(id);
        updateObj.setProcessInstanceId(processInstanceId);
        leaveRequestMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLeaveRequestBpmResult(Long id, Integer bpmStatus, String processInstanceId) {
        HrLeaveRequestDO leaveRequest = validateExists(id);
        // 幂等：审批结果只能从「待审批」落一次；重复投递（BPM 事件至少一次）不再改状态也不重复扣余额
        if (!HrLeaveStatusEnum.PENDING.getStatus().equals(leaveRequest.getStatus())) {
            return;
        }
        HrLeaveRequestDO updateObj = new HrLeaveRequestDO();
        updateObj.setId(id);
        updateObj.setProcessInstanceId(processInstanceId);
        updateObj.setApproveTime(LocalDateTime.now());
        if (BpmProcessInstanceStatusEnum.APPROVE.getStatus().equals(bpmStatus)) {
            updateObj.setStatus(HrLeaveStatusEnum.APPROVED.getStatus());
            leaveRequestMapper.updateById(updateObj);
            HrLeaveTypeDO leaveType = leaveTypeMapper.selectById(leaveRequest.getLeaveTypeId());
            if (leaveType != null && leaveType.getIsPaid() != null && leaveType.getIsPaid() == 1) {
                deductBalance(leaveRequest.getEmployeeId(), leaveRequest.getLeaveTypeId(),
                        leaveRequest.getStartDate().getYear(), leaveRequest.getDays());
            }
            return;
        }
        if (BpmProcessInstanceStatusEnum.REJECT.getStatus().equals(bpmStatus)) {
            updateObj.setStatus(HrLeaveStatusEnum.REJECTED.getStatus());
        } else if (BpmProcessInstanceStatusEnum.CANCEL.getStatus().equals(bpmStatus)) {
            updateObj.setStatus(HrLeaveStatusEnum.CANCELLED.getStatus());
        } else {
            // RUNNING 等中间态不落状态：提交时单据仍是待审批，倒写会把已通过的单据改回去
            return;
        }
        leaveRequestMapper.updateById(updateObj);
    }

    private void deductBalance(Long employeeId, Long leaveTypeId, Integer year, BigDecimal days) {
        HrLeaveBalanceDO balance = leaveBalanceMapper.selectByEmployeeAndTypeAndYear(employeeId, leaveTypeId, year);
        if (balance == null) {
            throw exception(HR_LEAVE_BALANCE_NOT_EXISTS);
        }
        BigDecimal newUsed = balance.getUsedDays().add(days);
        BigDecimal newRemaining = balance.getTotalDays().subtract(newUsed);
        HrLeaveBalanceDO updateObj = new HrLeaveBalanceDO();
        updateObj.setId(balance.getId());
        updateObj.setUsedDays(newUsed);
        updateObj.setRemainingDays(newRemaining);
        leaveBalanceMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelLeaveRequest(Long id) {
        HrLeaveRequestDO leaveRequest = validateExists(id);
        if (HrLeaveStatusEnum.CANCELLED.getStatus().equals(leaveRequest.getStatus())
                || HrLeaveStatusEnum.REJECTED.getStatus().equals(leaveRequest.getStatus())) {
            throw exception(HR_LEAVE_REQUEST_STATUS_INVALID);
        }
        // 已批准的请假撤销时返还余额
        if (HrLeaveStatusEnum.APPROVED.getStatus().equals(leaveRequest.getStatus())) {
            HrLeaveTypeDO leaveType = leaveTypeMapper.selectById(leaveRequest.getLeaveTypeId());
            if (leaveType != null && leaveType.getIsPaid() != null && leaveType.getIsPaid() == 1) {
                returnBalance(leaveRequest.getEmployeeId(), leaveRequest.getLeaveTypeId(),
                        leaveRequest.getStartDate().getYear(), leaveRequest.getDays());
            }
        }
        HrLeaveRequestDO updateObj = new HrLeaveRequestDO();
        updateObj.setId(id);
        updateObj.setStatus(HrLeaveStatusEnum.CANCELLED.getStatus());
        leaveRequestMapper.updateById(updateObj);
    }

    private void returnBalance(Long employeeId, Long leaveTypeId, Integer year, BigDecimal days) {
        HrLeaveBalanceDO balance = leaveBalanceMapper.selectByEmployeeAndTypeAndYear(employeeId, leaveTypeId, year);
        if (balance == null) {
            return;
        }
        BigDecimal newUsed = balance.getUsedDays().subtract(days);
        if (newUsed.compareTo(BigDecimal.ZERO) < 0) {
            newUsed = BigDecimal.ZERO;
        }
        BigDecimal newRemaining = balance.getTotalDays().subtract(newUsed);
        HrLeaveBalanceDO updateObj = new HrLeaveBalanceDO();
        updateObj.setId(balance.getId());
        updateObj.setUsedDays(newUsed);
        updateObj.setRemainingDays(newRemaining);
        leaveBalanceMapper.updateById(updateObj);
    }

    private HrLeaveRequestDO validateExists(Long id) {
        HrLeaveRequestDO leaveRequest = leaveRequestMapper.selectById(id);
        if (leaveRequest == null) {
            throw exception(HR_LEAVE_REQUEST_NOT_EXISTS);
        }
        return leaveRequest;
    }

    @Override
    public HrLeaveRequestDO getLeaveRequest(Long id) {
        return leaveRequestMapper.selectById(id);
    }

    @Override
    public PageResult<HrLeaveRequestDO> getLeaveRequestPage(HrLeaveRequestPageReqVO pageReqVO) {
        return leaveRequestMapper.selectPage(pageReqVO);
    }

    @Override
    public List<HrLeaveBalanceDO> getLeaveBalanceByYear(Long employeeId, Integer year) {
        return leaveBalanceMapper.selectListByEmployeeAndYear(employeeId, year);
    }

}