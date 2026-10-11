package cn.zhicloud.module.hr.dal.mysql.performance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.controller.admin.hrm.performance.vo.HrmPerformancePlanPageReqVO;
import cn.zhicloud.module.hr.dal.dataobject.performance.HrmPerformancePlanDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * HRM 绩效计划 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmPerformancePlanMapper extends BaseMapperX<HrmPerformancePlanDO> {

    default PageResult<HrmPerformancePlanDO> selectPage(HrmPerformancePlanPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrmPerformancePlanDO>()
                .likeIfPresent(HrmPerformancePlanDO::getName, reqVO.getName())
                .eqIfPresent(HrmPerformancePlanDO::getStatus, reqVO.getStatus())
                .eqIfPresent(HrmPerformancePlanDO::getCycleType, reqVO.getCycleType())
                .inIfPresent(HrmPerformancePlanDO::getStatus, reqVO.getStatuses())
                .orderByDesc(HrmPerformancePlanDO::getId));
    }

    default List<HrmPerformancePlanDO> selectListByIds(Collection<Long> ids) {
        return selectList(HrmPerformancePlanDO::getId, ids);
    }

    default List<HrmPerformancePlanDO> selectListByArchived() {
        return selectList(new LambdaQueryWrapperX<HrmPerformancePlanDO>()
                .eq(HrmPerformancePlanDO::getStatus, 4)
                .orderByDesc(HrmPerformancePlanDO::getId));
    }

}
