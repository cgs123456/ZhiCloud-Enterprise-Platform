package cn.zhicloud.module.qms.service.training;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.qms.controller.admin.training.vo.TrainingRecordPageReqVO;
import cn.zhicloud.module.qms.controller.admin.training.vo.TrainingRecordSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.training.TrainingRecordDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * QMS 培训记录 Service 接口
 *
 * @author zhicloud
 */
public interface TrainingRecordService {

    Long createTrainingRecord(@Valid TrainingRecordSaveReqVO createReqVO);

    void updateTrainingRecord(@Valid TrainingRecordSaveReqVO updateReqVO);

    void deleteTrainingRecord(Long id);

    TrainingRecordDO getTrainingRecord(Long id);

    PageResult<TrainingRecordDO> getTrainingRecordPage(TrainingRecordPageReqVO pageReqVO);

    /**
     * 获得培训计划关联的培训记录列表
     */
    List<TrainingRecordDO> getTrainingRecordListByPlanId(Long planId);

}