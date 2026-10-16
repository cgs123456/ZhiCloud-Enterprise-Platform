package cn.zhicloud.module.qms.service.training;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.qms.controller.admin.training.vo.TrainingPlanPageReqVO;
import cn.zhicloud.module.qms.controller.admin.training.vo.TrainingPlanSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.training.TrainingPlanDO;
import cn.zhicloud.module.qms.dal.mysql.training.TrainingPlanMapper;
import cn.zhicloud.module.qms.enums.qms.TrainingPlanStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.TRAINING_PLAN_NOT_EXISTS;

/**
 * QMS 培训计划 Service 实现类
 *
 * @author zhicloud
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
        // 状态无条件置为初始值，忽略调用方传入的任何 status。
        // §12.235.5：此前「仅在 null 时补默认」，调用方即可在**建单**这一步直接写入终态，
        // 跳过整个流转（该类可能连流转方法都没有），产生「未审即批/未做即完成」的记录。
        // 状态只能由本模块的流转方法产生。
        trainingPlan.setStatus(TrainingPlanStatusEnum.DRAFT.getStatus());
        trainingPlanMapper.insert(trainingPlan);
        return trainingPlan.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTrainingPlan(TrainingPlanSaveReqVO updateReqVO) {
        validateTrainingPlanExists(updateReqVO.getId());
        TrainingPlanDO updateObj = BeanUtils.toBean(updateReqVO, TrainingPlanDO.class);
        // 状态不接受外部写入（§12.236.7）。本类**没有任何流转方法**，
        // 若允许通用修改写状态，则「新增 + 修改」两步就能到达任意终态，
        // 新增侧刚加的护栏形同虚设。
        // MyBatis-Plus 的 updateById 跳过 null 字段，故置 null 的效果是
        // 该列不进 UPDATE 语句 —— 调用方的 status 无法生效。
        updateObj.setStatus(null);
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