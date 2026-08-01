package cn.iocoder.yudao.module.hr.service.recruitment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.hr.controller.admin.recruitment.vo.HrResumePageReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.recruitment.vo.HrResumeSaveReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.recruitment.vo.HrResumeScreenReqVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.recruitment.HrResumeDO;
import jakarta.validation.Valid;

public interface HrResumeService {

    Long createResume(@Valid HrResumeSaveReqVO createReqVO);

    void updateResume(@Valid HrResumeSaveReqVO updateReqVO);

    void deleteResume(Long id);

    HrResumeDO getResume(Long id);

    PageResult<HrResumeDO> getResumePage(HrResumePageReqVO pageReqVO);

    void screenResume(@Valid HrResumeScreenReqVO reqVO);

    Long offerResume(Long id);

}