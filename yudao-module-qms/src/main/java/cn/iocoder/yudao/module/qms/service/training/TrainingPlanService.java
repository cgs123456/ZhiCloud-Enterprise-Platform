package cn.iocoder.yudao.module.qms.service.training;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.qms.controller.admin.training.vo.TrainingPlanPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.training.vo.TrainingPlanSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.training.TrainingPlanDO;
import jakarta.validation.Valid;

/**
 * QMS 培训计划 Service 接口
 *
 * @author yudao
 */
public interface TrainingPlanService {

    Long createTrainingPlan(@Valid TrainingPlanSaveReqVO createReqVO);

    void updateTrainingPlan(@Valid TrainingPlanSaveReqVO updateReqVO);

    void deleteTrainingPlan(Long id);

    TrainingPlanDO getTrainingPlan(Long id);

    PageResult<TrainingPlanDO> getTrainingPlanPage(TrainingPlanPageReqVO pageReqVO);

}