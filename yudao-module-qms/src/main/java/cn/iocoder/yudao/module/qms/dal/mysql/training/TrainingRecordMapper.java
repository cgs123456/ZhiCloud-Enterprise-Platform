package cn.iocoder.yudao.module.qms.dal.mysql.training;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.qms.controller.admin.training.vo.TrainingRecordPageReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.training.TrainingRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * QMS 培训记录 Mapper
 *
 * @author yudao
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