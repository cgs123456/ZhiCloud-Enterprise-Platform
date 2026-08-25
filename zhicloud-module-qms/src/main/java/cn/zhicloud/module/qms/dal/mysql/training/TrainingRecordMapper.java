package cn.zhicloud.module.qms.dal.mysql.training;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.qms.controller.admin.training.vo.TrainingRecordPageReqVO;
import cn.zhicloud.module.qms.dal.dataobject.training.TrainingRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * QMS 培训记录 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface TrainingRecordMapper extends BaseMapperX<TrainingRecordDO> {

    default PageResult<TrainingRecordDO> selectPage(TrainingRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TrainingRecordDO>()
                .likeIfPresent(TrainingRecordDO::getRecordNo, reqVO.getRecordNo())
                .eqIfPresent(TrainingRecordDO::getPlanId, reqVO.getPlanId())
                .eqIfPresent(TrainingRecordDO::getTraineeId, reqVO.getTraineeId())
                .eqIfPresent(TrainingRecordDO::getStatus, reqVO.getStatus())
                .orderByDesc(TrainingRecordDO::getId));
    }

    default List<TrainingRecordDO> selectListByPlanId(Long planId) {
        return selectList(TrainingRecordDO::getPlanId, planId);
    }

}