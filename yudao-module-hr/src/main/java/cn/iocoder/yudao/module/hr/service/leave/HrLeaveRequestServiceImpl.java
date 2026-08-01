package cn.iocoder.yudao.module.hr.service.leave;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.hr.controller.admin.leave.vo.HrLeaveRequestApproveReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.leave.vo.HrLeaveRequestPageReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.leave.vo.HrLeaveRequestSaveReqVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.leave.HrLeaveBalanceDO;
import cn.iocoder.yudao.module.hr.dal.dataobject.leave.HrLeaveRequestDO;
import cn.iocoder.yudao.module.hr.dal.dataobject.leave.HrLeaveTypeDO;
import cn.iocoder.yudao.module.hr.dal.mysql.leave.HrLeaveBalanceMapper;
import cn.iocoder.yudao.module.hr.dal.mysql.leave.HrLeaveRequestMapper;
import cn.iocoder.yudao.module.hr.dal.mysql.leave.HrLeaveTypeMapper;
import cn.iocoder.yudao.module.hr.enums.leave.HrLeaveStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hr.enums.ErrorCodeConstants.*;

@Service
@Validated
public class HrLeaveRequestServiceImpl implements HrLeaveRequestService {

    @Resource
    private HrLeaveRequestMapper leaveRequestMapper;
    @Resource
    private HrLeaveBalanceMapper leaveBalanceMapper;
    @Resource
    private HrLeaveTypeMapper leaveTypeMapper;

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