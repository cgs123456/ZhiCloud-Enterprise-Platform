package cn.zhicloud.module.qms.dal.mysql.training;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.qms.controller.admin.training.vo.TrainingPlanPageReqVO;
import cn.zhicloud.module.qms.dal.dataobject.training.TrainingPlanDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * QMS 培训计划 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface TrainingPlanMapper extends BaseMapperX<TrainingPlanDO> {

    default PageResult<TrainingPlanDO> selectPage(TrainingPlanPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TrainingPlanDO>()
                .likeIfPresent(TrainingPlanDO::getPlanNo, reqVO.getPlanNo())
                .likeIfPresent(TrainingPlanDO::getPlanName, reqVO.getPlanName())
                .eqIfPresent(TrainingPlanDO::getYear, reqVO.getYear())
                .eqIfPresent(TrainingPlanDO::getStatus, reqVO.getStatus())
                .orderByDesc(TrainingPlanDO::getId));
    }

    default TrainingPlanDO selectByPlanNo(String planNo) {
        return selectOne(TrainingPlanDO::getPlanNo, planNo);
    }

}