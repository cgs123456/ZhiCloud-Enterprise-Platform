package cn.zhicloud.module.hr.dal.mysql.position;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.controller.admin.position.vo.HrPositionPageReqVO;
import cn.zhicloud.module.hr.dal.dataobject.position.HrPositionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HR 职位 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrPositionMapper extends BaseMapperX<HrPositionDO> {

    default PageResult<HrPositionDO> selectPage(HrPositionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrPositionDO>()
                .likeIfPresent(HrPositionDO::getCode, reqVO.getCode())
                .likeIfPresent(HrPositionDO::getName, reqVO.getName())
                .eqIfPresent(HrPositionDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(HrPositionDO::getLevel, reqVO.getLevel())
                .orderByDesc(HrPositionDO::getId));
    }

    default HrPositionDO selectByCode(String code) {
        return selectOne(HrPositionDO::getCode, code);
    }

    default List<HrPositionDO> selectListByDeptId(Long deptId) {
        return selectList(HrPositionDO::getDeptId, deptId);
    }

}