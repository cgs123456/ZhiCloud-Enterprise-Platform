package cn.zhicloud.module.hr.dal.mysql.employee;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.controller.admin.employee.vo.HrEmployeePageReqVO;
import cn.zhicloud.module.hr.dal.dataobject.employee.HrEmployeeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * HR 员工档案 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrEmployeeMapper extends BaseMapperX<HrEmployeeDO> {

    default PageResult<HrEmployeeDO> selectPage(HrEmployeePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrEmployeeDO>()
                .likeIfPresent(HrEmployeeDO::getEmpNo, reqVO.getEmpNo())
                .likeIfPresent(HrEmployeeDO::getName, reqVO.getName())
                .eqIfPresent(HrEmployeeDO::getGender, reqVO.getGender())
                .eqIfPresent(HrEmployeeDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(HrEmployeeDO::getPositionId, reqVO.getPositionId())
                .eqIfPresent(HrEmployeeDO::getStatus, reqVO.getStatus())
                .eqIfPresent(HrEmployeeDO::getEmploymentType, reqVO.getEmploymentType())
                .orderByDesc(HrEmployeeDO::getId));
    }

    default HrEmployeeDO selectByEmpNo(String empNo) {
        return selectOne(HrEmployeeDO::getEmpNo, empNo);
    }

    default Long selectCountByDeptId(Long deptId) {
        return selectCount(HrEmployeeDO::getDeptId, deptId);
    }

    default Long selectCountByPositionId(Long positionId) {
        return selectCount(HrEmployeeDO::getPositionId, positionId);
    }

    default List<HrEmployeeDO> selectListByDeptIds(Collection<Long> deptIds) {
        return selectList(HrEmployeeDO::getDeptId, deptIds);
    }

}