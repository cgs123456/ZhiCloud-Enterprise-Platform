package cn.iocoder.yudao.module.hr.service.recruitment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.hr.controller.admin.recruitment.vo.HrInterviewPageReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.recruitment.vo.HrInterviewResultReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.recruitment.vo.HrInterviewSaveReqVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.recruitment.HrInterviewDO;
import jakarta.validation.Valid;

public interface HrInterviewService {

    Long createInterview(@Valid HrInterviewSaveReqVO createReqVO);

    void updateInterview(@Valid HrInterviewSaveReqVO updateReqVO);

    void deleteInterview(Long id);

    HrInterviewDO getInterview(Long id);

    PageResult<HrInterviewDO> getInterviewPage(HrInterviewPageReqVO pageReqVO);

    Long scheduleInterview(@Valid HrInterviewSaveReqVO reqVO);

    void recordResult(@Valid HrInterviewResultReqVO reqVO);

}