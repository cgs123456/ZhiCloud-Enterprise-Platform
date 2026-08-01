package cn.iocoder.yudao.module.hr.dal.mysql.department;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hr.controller.admin.department.vo.HrDepartmentListReqVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.department.HrDepartmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * HR 部门 Mapper
 *
 * @author yudao
 */
@Mapper
public interface HrDepartmentMapper extends BaseMapperX<HrDepartmentDO> {

    default List<HrDepartmentDO> selectList(HrDepartmentListReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<HrDepartmentDO>()
                .likeIfPresent(HrDepartmentDO::getCode, reqVO.getCode())
                .likeIfPresent(HrDepartmentDO::getName, reqVO.getName())
                .eqIfPresent(HrDepartmentDO::getStatus, reqVO.getStatus())
                .orderByAsc(HrDepartmentDO::getSort));
    }

    default HrDepartmentDO selectByCode(String code) {
        return selectOne(HrDepartmentDO::getCode, code);
    }

    default Long selectCountByParentId(Long parentId) {
        return selectCount(HrDepartmentDO::getParentId, parentId);
    }

    default List<HrDepartmentDO> selectListByParentId(Collection<Long> parentIds) {
        return selectList(HrDepartmentDO::getParentId, parentIds);
    }

}