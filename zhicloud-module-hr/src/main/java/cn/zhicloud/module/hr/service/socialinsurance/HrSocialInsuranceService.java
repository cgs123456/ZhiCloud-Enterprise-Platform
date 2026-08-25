package cn.zhicloud.module.hr.service.socialinsurance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.hr.controller.admin.socialinsurance.vo.HrSocialInsurancePageReqVO;
import cn.zhicloud.module.hr.controller.admin.socialinsurance.vo.HrSocialInsuranceSaveReqVO;
import cn.zhicloud.module.hr.controller.admin.socialinsurance.vo.SocialInsuranceDetailRespVO;
import cn.zhicloud.module.hr.dal.dataobject.socialinsurance.HrSocialInsuranceBaseDO;
import jakarta.validation.Valid;

public interface HrSocialInsuranceService {

    Long createSocialInsurance(@Valid HrSocialInsuranceSaveReqVO createReqVO);

    void updateSocialInsurance(@Valid HrSocialInsuranceSaveReqVO updateReqVO);

    void deleteSocialInsurance(Long id);

    HrSocialInsuranceBaseDO getSocialInsurance(Long id);

    PageResult<HrSocialInsuranceBaseDO> getSocialInsurancePage(HrSocialInsurancePageReqVO pageReqVO);

    Long adjustBase(@Valid HrSocialInsuranceSaveReqVO reqVO);

    SocialInsuranceDetailRespVO calculateMonthly(Long employeeId);

}