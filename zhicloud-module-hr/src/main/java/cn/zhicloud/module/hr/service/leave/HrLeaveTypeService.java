package cn.zhicloud.module.hr.service.leave;

import cn.zhicloud.module.hr.controller.admin.leave.vo.HrLeaveTypeSaveReqVO;
import cn.zhicloud.module.hr.dal.dataobject.leave.HrLeaveTypeDO;
import jakarta.validation.Valid;

import java.util.List;

public interface HrLeaveTypeService {

    Long createLeaveType(@Valid HrLeaveTypeSaveReqVO createReqVO);

    void updateLeaveType(@Valid HrLeaveTypeSaveReqVO updateReqVO);

    void deleteLeaveType(Long id);

    HrLeaveTypeDO getLeaveType(Long id);

    List<HrLeaveTypeDO> getLeaveTypeList();

}