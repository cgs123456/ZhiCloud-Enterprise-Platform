package cn.iocoder.yudao.module.qms.service.training;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.training.vo.TrainingPlanPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.training.vo.TrainingPlanSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.training.TrainingPlanDO;
import cn.iocoder.yudao.module.qms.dal.mysql.training.TrainingPlanMapper;
import cn.iocoder.yudao.module.qms.enums.qms.TrainingPlanStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.TRAINING_PLAN_NOT_EXISTS;

/**
 * QMS 培训计划 Service 实现类
 *
 * @author yudao
 */
@Service
@Validated
public class TrainingPlanServiceImpl implements TrainingPlanService {

    @Resource
    private TrainingPlanMapper trainingPlanMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTrainingPlan(TrainingPlanSaveReqVO createReqVO) {
        TrainingPlanDO trainingPlan = BeanUtils.toBean(createReqVO, TrainingPlanDO.class);
        if (trainingPlan.getStatus() == null) {
            trainingPlan.setStatus(TrainingPlanStatusEnum.DRAFT.getStatus());
        }
        trainingPlanMapper.insert(trainingPlan);
        return trainingPlan.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTrainingPlan(TrainingPlanSaveReqVO updateReqVO) {
        validateTrainingPlanExists(updateReqVO.getId());
        TrainingPlanDO updateObj = BeanUtils.toBean(updateReqVO, TrainingPlanDO.class);
        trainingPlanMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTrainingPlan(Long id) {
        validateTrainingPlanExists(id);
        trainingPlanMapper.deleteById(id);
    }

    private void validateTrainingPlanExists(Long id) {
        if (trainingPlanMapper.selectById(id) == null) {
            throw exception(TRAINING_PLAN_NOT_EXISTS);
        }
    }

    @Override
    public TrainingPlanDO getTrainingPlan(Long id) {
        return trainingPlanMapper.selectById(id);
    }

    @Override
    public PageResult<TrainingPlanDO> getTrainingPlanPage(TrainingPlanPageReqVO pageReqVO) {
        return trainingPlanMapper.selectPage(pageReqVO);
    }

}