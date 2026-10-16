package cn.zhicloud.module.qms.service.training;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.qms.controller.admin.training.vo.TrainingRecordPageReqVO;
import cn.zhicloud.module.qms.controller.admin.training.vo.TrainingRecordSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.training.TrainingRecordDO;
import cn.zhicloud.module.qms.dal.mysql.training.TrainingRecordMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.TRAINING_RECORD_NOT_EXISTS;

/**
 * QMS 培训记录 Service 实现类
 *
 * @author zhicloud
 */
@Service
@Validated
public class TrainingRecordServiceImpl implements TrainingRecordService {

    @Resource
    private TrainingRecordMapper trainingRecordMapper;

    /** 培训记录状态：未开始（见 TrainingRecordDO#status 注释；该域尚无枚举） */
    private static final int TRAINING_RECORD_STATUS_NOT_STARTED = 10;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTrainingRecord(TrainingRecordSaveReqVO createReqVO) {
        TrainingRecordDO trainingRecord = BeanUtils.toBean(createReqVO, TrainingRecordDO.class);
        // 状态无条件置为初始值，忽略调用方传入的任何 status（§12.235.5）。
        // 该域尚无状态枚举（DO 注释：10=未开始 / 20=已完成），按「抽象延迟到第二个真实
        // 用例」的原则不新造枚举，保留原字面量，仅补语义说明与常量名。
        trainingRecord.setStatus(TRAINING_RECORD_STATUS_NOT_STARTED);
        trainingRecordMapper.insert(trainingRecord);
        return trainingRecord.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTrainingRecord(TrainingRecordSaveReqVO updateReqVO) {
        validateTrainingRecordExists(updateReqVO.getId());
        TrainingRecordDO updateObj = BeanUtils.toBean(updateReqVO, TrainingRecordDO.class);
        // 状态不接受外部写入（§12.236.7）。本类**没有任何流转方法**，
        // 若允许通用修改写状态，则「新增 + 修改」两步就能到达任意终态，
        // 新增侧刚加的护栏形同虚设。
        // MyBatis-Plus 的 updateById 跳过 null 字段，故置 null 的效果是
        // 该列不进 UPDATE 语句 —— 调用方的 status 无法生效。
        updateObj.setStatus(null);
        trainingRecordMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTrainingRecord(Long id) {
        validateTrainingRecordExists(id);
        trainingRecordMapper.deleteById(id);
    }

    private void validateTrainingRecordExists(Long id) {
        if (trainingRecordMapper.selectById(id) == null) {
            throw exception(TRAINING_RECORD_NOT_EXISTS);
        }
    }

    @Override
    public TrainingRecordDO getTrainingRecord(Long id) {
        return trainingRecordMapper.selectById(id);
    }

    @Override
    public PageResult<TrainingRecordDO> getTrainingRecordPage(TrainingRecordPageReqVO pageReqVO) {
        return trainingRecordMapper.selectPage(pageReqVO);
    }

    @Override
    public List<TrainingRecordDO> getTrainingRecordListByPlanId(Long planId) {
        return trainingRecordMapper.selectListByPlanId(planId);
    }

}