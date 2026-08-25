package cn.zhicloud.module.hr.service.recruitment;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.hr.controller.admin.recruitment.vo.HrJobPostingPageReqVO;
import cn.zhicloud.module.hr.controller.admin.recruitment.vo.HrJobPostingSaveReqVO;
import cn.zhicloud.module.hr.dal.dataobject.recruitment.HrJobPostingDO;
import jakarta.validation.Valid;

public interface HrJobPostingService {

    Long createJobPosting(@Valid HrJobPostingSaveReqVO createReqVO);

    void updateJobPosting(@Valid HrJobPostingSaveReqVO updateReqVO);

    void deleteJobPosting(Long id);

    HrJobPostingDO getJobPosting(Long id);

    PageResult<HrJobPostingDO> getJobPostingPage(HrJobPostingPageReqVO pageReqVO);

    void publishJobPosting(Long id);

    void closeJobPosting(Long id);

}