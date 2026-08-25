package cn.zhicloud.module.hr.service.department;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.hr.controller.admin.department.vo.HrDepartmentListReqVO;
import cn.zhicloud.module.hr.controller.admin.department.vo.HrDepartmentSaveReqVO;
import cn.zhicloud.module.hr.dal.dataobject.department.HrDepartmentDO;
import cn.zhicloud.module.hr.dal.dataobject.employee.HrEmployeeDO;
import cn.zhicloud.module.hr.dal.mysql.department.HrDepartmentMapper;
import cn.zhicloud.module.hr.dal.mysql.employee.HrEmployeeMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.*;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.zhicloud.module.hr.enums.ErrorCodeConstants.*;

/**
 * HR 部门 Service 实现类
 *
 * @author zhicloud
 */
@Service
@Validated
public class HrDepartmentServiceImpl implements HrDepartmentService {

    @Resource
    private HrDepartmentMapper departmentMapper;

    @Resource
    private HrEmployeeMapper employeeMapper;

    @Override
    public Long createDepartment(HrDepartmentSaveReqVO createReqVO) {
        if (createReqVO.getParentId() == null) {
            createReqVO.setParentId(HrDepartmentDO.PARENT_ID_ROOT);
        }
        // 校验父部门有效性
        validateParentDepartment(null, createReqVO.getParentId());
        // 校验编码唯一
        validateCodeUnique(null, createReqVO.getCode());
        // 插入
        HrDepartmentDO department = BeanUtils.toBean(createReqVO, HrDepartmentDO.class);
        departmentMapper.insert(department);
        return department.getId();
    }

    @Override
    public void updateDepartment(HrDepartmentSaveReqVO updateReqVO) {
        if (updateReqVO.getParentId() == null) {
            updateReqVO.setParentId(HrDepartmentDO.PARENT_ID_ROOT);
        }
        // 校验存在
        validateDepartmentExists(updateReqVO.getId());
        // 校验父部门有效性
        validateParentDepartment(updateReqVO.getId(), updateReqVO.getParentId());
        // 校验编码唯一
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getCode());
        // 更新
        HrDepartmentDO updateObj = BeanUtils.toBean(updateReqVO, HrDepartmentDO.class);
        departmentMapper.updateById(updateObj);
    }

    @Override
    public void deleteDepartment(Long id) {
        // 校验存在
        validateDepartmentExists(id);
        // 校验是否有子部门
        if (departmentMapper.selectCountByParentId(id) > 0) {
            throw exception(HR_DEPARTMENT_HAS_CHILDREN);
        }
        // 校验是否有员工
        if (employeeMapper.selectCountByDeptId(id) > 0) {
            throw exception(HR_DEPARTMENT_HAS_EMPLOYEES);
        }
        // 删除
        departmentMapper.deleteById(id);
    }

    private void validateDepartmentExists(Long id) {
        if (id == null) {
            return;
        }
        if (departmentMapper.selectById(id) == null) {
            throw exception(HR_DEPARTMENT_NOT_EXISTS);
        }
    }

    private void validateParentDepartment(Long id, Long parentId) {
        if (parentId == null || HrDepartmentDO.PARENT_ID_ROOT.equals(parentId)) {
            return;
        }
        // 1. 不能设置自己为父部门
        if (Objects.equals(id, parentId)) {
            throw exception(HR_DEPARTMENT_NOT_EXISTS);
        }
        // 2. 父部门不存在
        HrDepartmentDO parentDepartment = departmentMapper.selectById(parentId);
        if (parentDepartment == null) {
            throw exception(HR_DEPARTMENT_NOT_EXISTS);
        }
        // 3. 递归校验父部门，避免形成环路
        if (id == null) {
            return;
        }
        for (int i = 0; i < Short.MAX_VALUE; i++) {
            parentId = parentDepartment.getParentId();
            if (Objects.equals(id, parentId)) {
                throw exception(HR_DEPARTMENT_NOT_EXISTS);
            }
            if (parentId == null || HrDepartmentDO.PARENT_ID_ROOT.equals(parentId)) {
                break;
            }
            parentDepartment = departmentMapper.selectById(parentId);
            if (parentDepartment == null) {
                break;
            }
        }
    }

    private void validateCodeUnique(Long id, String code) {
        HrDepartmentDO department = departmentMapper.selectByCode(code);
        if (department == null) {
            return;
        }
        if (id == null) {
            throw exception(HR_DEPARTMENT_CODE_DUPLICATE);
        }
        if (!department.getId().equals(id)) {
            throw exception(HR_DEPARTMENT_CODE_DUPLICATE);
        }
    }

    @Override
    public HrDepartmentDO getDepartment(Long id) {
        return departmentMapper.selectById(id);
    }

    @Override
    public List<HrDepartmentDO> getDepartmentList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return departmentMapper.selectByIds(ids);
    }

    @Override
    public List<HrDepartmentDO> getDepartmentList(HrDepartmentListReqVO reqVO) {
        return departmentMapper.selectList(reqVO);
    }

    @Override
    public List<HrDepartmentDO> getChildDepartmentList(Long id) {
        List<HrDepartmentDO> children = new LinkedList<>();
        Collection<Long> parentIds = Collections.singleton(id);
        for (int i = 0; i < Short.MAX_VALUE; i++) {
            List<HrDepartmentDO> departments = departmentMapper.selectListByParentId(parentIds);
            if (CollUtil.isEmpty(departments)) {
                break;
            }
            children.addAll(departments);
            parentIds = convertSet(departments, HrDepartmentDO::getId);
        }
        return children;
    }

}