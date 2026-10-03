package cn.zhicloud.module.hr.service.leave;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.hr.controller.admin.leave.vo.HrLeaveRequestApproveReqVO;
import cn.zhicloud.module.hr.controller.admin.leave.vo.HrLeaveRequestPageReqVO;
import cn.zhicloud.module.hr.controller.admin.leave.vo.HrLeaveRequestSaveReqVO;
import cn.zhicloud.module.hr.dal.dataobject.leave.HrLeaveBalanceDO;
import cn.zhicloud.module.hr.dal.dataobject.leave.HrLeaveRequestDO;
import jakarta.validation.Valid;

import java.util.List;

public interface HrLeaveRequestService {

    Long createLeaveRequest(@Valid HrLeaveRequestSaveReqVO createReqVO);

    void approveLeaveRequest(@Valid HrLeaveRequestApproveReqVO reqVO);

    /**
     * 提交请假申请到 BPM 流程引擎
     *
     * @param id 请假申请编号
     */
    void submitLeaveRequest(Long id);

    /**
     * 回写 BPM 审批结果（由流程结果监听器调用）
     *
     * @param id                请假申请编号
     * @param bpmStatus         BPM 流程实例状态（2 通过 / 3 不通过 / 4 取消）
     * @param processInstanceId BPM 流程实例编号
     */
    void updateLeaveRequestBpmResult(Long id, Integer bpmStatus, String processInstanceId);

    void cancelLeaveRequest(Long id);

    HrLeaveRequestDO getLeaveRequest(Long id);

    PageResult<HrLeaveRequestDO> getLeaveRequestPage(HrLeaveRequestPageReqVO pageReqVO);

    List<HrLeaveBalanceDO> getLeaveBalanceByYear(Long employeeId, Integer year);

}