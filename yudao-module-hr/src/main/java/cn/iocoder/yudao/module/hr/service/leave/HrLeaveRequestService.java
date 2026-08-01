package cn.iocoder.yudao.module.hr.service.leave;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.hr.controller.admin.leave.vo.HrLeaveRequestApproveReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.leave.vo.HrLeaveRequestPageReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.leave.vo.HrLeaveRequestSaveReqVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.leave.HrLeaveBalanceDO;
import cn.iocoder.yudao.module.hr.dal.dataobject.leave.HrLeaveRequestDO;
import jakarta.validation.Valid;

import java.util.List;

public interface HrLeaveRequestService {

    Long createLeaveRequest(@Valid HrLeaveRequestSaveReqVO createReqVO);

    void approveLeaveRequest(@Valid HrLeaveRequestApproveReqVO reqVO);

    void cancelLeaveRequest(Long id);

    HrLeaveRequestDO getLeaveRequest(Long id);

    PageResult<HrLeaveRequestDO> getLeaveRequestPage(HrLeaveRequestPageReqVO pageReqVO);

    List<HrLeaveBalanceDO> getLeaveBalanceByYear(Long employeeId, Integer year);

}