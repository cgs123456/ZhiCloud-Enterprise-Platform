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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTrainingRecord(TrainingRecordSaveReqVO createReqVO) {
        TrainingRecordDO trainingRecord = BeanUtils.toBean(createReqVO, TrainingRecordDO.class);
        if (trainingRecord.getStatus() == null) {
            trainingRecord.setStatus(10);
        }
        trainingRecordMapper.insert(trainingRecord);
        return trainingRecord.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTrainingRecord(TrainingRecordSaveReqVO updateReqVO) {
        validateTrainingRecordExists(updateReqVO.getId());
        TrainingRecordDO updateObj = BeanUtils.toBean(updateReqVO, TrainingRecordDO.class);
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