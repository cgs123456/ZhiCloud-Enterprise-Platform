package cn.iocoder.yudao.module.hr.service.employee;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.hr.controller.admin.employee.vo.HrEmployeePageReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.employee.vo.HrEmployeeResignReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.employee.vo.HrEmployeeSaveReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.employee.vo.HrEmployeeTransferReqVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.employee.HrEmployeeDO;
import cn.iocoder.yudao.module.hr.dal.mysql.employee.HrEmployeeMapper;
import cn.iocoder.yudao.module.hr.enums.employee.HrEmployeeStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hr.enums.ErrorCodeConstants.*;

/**
 * HR 员工档案 Service 实现类
 *
 * @author yudao
 */
@Service
@Validated
public class HrEmployeeServiceImpl implements HrEmployeeService {

    @Resource
    private HrEmployeeMapper employeeMapper;

    @Override
    public Long createEmployee(HrEmployeeSaveReqVO createReqVO) {
        // 校验工号唯一
        validateEmpNoUnique(null, createReqVO.getEmpNo());
        // 插入
        HrEmployeeDO employee = BeanUtils.toBean(createReqVO, HrEmployeeDO.class);
        employeeMapper.insert(employee);
        return employee.getId();
    }

    @Override
    public void updateEmployee(HrEmployeeSaveReqVO updateReqVO) {
        // 校验存在
        validateEmployeeExists(updateReqVO.getId());
        // 校验工号唯一
        validateEmpNoUnique(updateReqVO.getId(), updateReqVO.getEmpNo());
        // 更新
        HrEmployeeDO updateObj = BeanUtils.toBean(updateReqVO, HrEmployeeDO.class);
        employeeMapper.updateById(updateObj);
    }

    @Override
    public void deleteEmployee(Long id) {
        // 校验存在
        validateEmployeeExists(id);
        // 删除
        employeeMapper.deleteById(id);
    }

    private void validateEmployeeExists(Long id) {
        if (employeeMapper.selectById(id) == null) {
            throw exception(HR_EMPLOYEE_NOT_EXISTS);
        }
    }

    private void validateEmpNoUnique(Long id, String empNo) {
        HrEmployeeDO employee = employeeMapper.selectByEmpNo(empNo);
        if (employee == null) {
            return;
        }
        if (id == null) {
            throw exception(HR_EMPLOYEE_NO_DUPLICATE);
        }
        if (!employee.getId().equals(id)) {
            throw exception(HR_EMPLOYEE_NO_DUPLICATE);
        }
    }

    @Override
    public HrEmployeeDO getEmployee(Long id) {
        return employeeMapper.selectById(id);
    }

    @Override
    public List<HrEmployeeDO> getEmployeeList(Collection<Long> ids) {
        return employeeMapper.selectByIds(ids);
    }

    @Override
    public PageResult<HrEmployeeDO> getEmployeePage(HrEmployeePageReqVO pageReqVO) {
        return employeeMapper.selectPage(pageReqVO);
    }

    @Override
    public void resignEmployee(HrEmployeeResignReqVO reqVO) {
        // 校验存在
        HrEmployeeDO employee = validateEmployeeExists4Update(reqVO.getId());
        // 校验是否已离职
        if (HrEmployeeStatusEnum.RESIGNED.getStatus().equals(employee.getStatus())) {
            throw exception(HR_EMPLOYEE_ALREADY_RESIGNED);
        }
        // 更新：自动设置离职日期 + 状态为离职
        HrEmployeeDO updateObj = new HrEmployeeDO();
        updateObj.setId(reqVO.getId());
        updateObj.setLeaveDate(reqVO.getLeaveDate());
        updateObj.setStatus(HrEmployeeStatusEnum.RESIGNED.getStatus());
        employeeMapper.updateById(updateObj);
    }

    @Override
    public void transferEmployee(HrEmployeeTransferReqVO reqVO) {
        // 校验存在
        HrEmployeeDO employee = validateEmployeeExists4Update(reqVO.getId());
        // 校验是否已离职
        if (HrEmployeeStatusEnum.RESIGNED.getStatus().equals(employee.getStatus())) {
            throw exception(HR_EMPLOYEE_ALREADY_RESIGNED);
        }
        // 更新：变更部门 + 职位
        HrEmployeeDO updateObj = new HrEmployeeDO();
        updateObj.setId(reqVO.getId());
        updateObj.setDeptId(reqVO.getDeptId());
        updateObj.setPositionId(reqVO.getPositionId());
        employeeMapper.updateById(updateObj);
    }

    private HrEmployeeDO validateEmployeeExists4Update(Long id) {
        HrEmployeeDO employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw exception(HR_EMPLOYEE_NOT_EXISTS);
        }
        return employee;
    }

    @Override
    public List<HrEmployeeDO> getEmployeeListByDeptIds(Collection<Long> deptIds) {
        return employeeMapper.selectListByDeptIds(deptIds);
    }

}