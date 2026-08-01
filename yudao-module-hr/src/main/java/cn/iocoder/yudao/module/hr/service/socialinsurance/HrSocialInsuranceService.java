package cn.iocoder.yudao.module.hr.service.socialinsurance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.hr.controller.admin.socialinsurance.vo.HrSocialInsurancePageReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.socialinsurance.vo.HrSocialInsuranceSaveReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.socialinsurance.vo.SocialInsuranceDetailRespVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.socialinsurance.HrSocialInsuranceBaseDO;
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