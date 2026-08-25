package cn.zhicloud.module.hr.service.leave;

import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.hr.controller.admin.leave.vo.HrLeaveTypeSaveReqVO;
import cn.zhicloud.module.hr.dal.dataobject.leave.HrLeaveTypeDO;
import cn.zhicloud.module.hr.dal.mysql.leave.HrLeaveTypeMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.hr.enums.ErrorCodeConstants.*;

@Service
@Validated
public class HrLeaveTypeServiceImpl implements HrLeaveTypeService {

    @Resource
    private HrLeaveTypeMapper leaveTypeMapper;

    @Override
    public Long createLeaveType(HrLeaveTypeSaveReqVO createReqVO) {
        validateCodeUnique(null, createReqVO.getCode());
        HrLeaveTypeDO leaveType = BeanUtils.toBean(createReqVO, HrLeaveTypeDO.class);
        leaveTypeMapper.insert(leaveType);
        return leaveType.getId();
    }

    @Override
    public void updateLeaveType(HrLeaveTypeSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getId());
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getCode());
        HrLeaveTypeDO updateObj = BeanUtils.toBean(updateReqVO, HrLeaveTypeDO.class);
        leaveTypeMapper.updateById(updateObj);
    }

    @Override
    public void deleteLeaveType(Long id) {
        validateExists(id);
        leaveTypeMapper.deleteById(id);
    }

    private void validateExists(Long id) {
        if (leaveTypeMapper.selectById(id) == null) {
            throw exception(HR_LEAVE_TYPE_NOT_EXISTS);
        }
    }

    private void validateCodeUnique(Long id, String code) {
        HrLeaveTypeDO leaveType = leaveTypeMapper.selectByCode(code);
        if (leaveType == null) {
            return;
        }
        if (id == null || !leaveType.getId().equals(id)) {
            throw exception(HR_LEAVE_TYPE_CODE_DUPLICATE);
        }
    }

    @Override
    public HrLeaveTypeDO getLeaveType(Long id) {
        return leaveTypeMapper.selectById(id);
    }

    @Override
    public List<HrLeaveTypeDO> getLeaveTypeList() {
        return leaveTypeMapper.selectListAll();
    }

}