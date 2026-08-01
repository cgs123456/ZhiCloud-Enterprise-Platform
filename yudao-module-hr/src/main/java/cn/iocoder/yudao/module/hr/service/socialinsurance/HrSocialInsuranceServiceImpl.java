package cn.iocoder.yudao.module.hr.service.socialinsurance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.hr.controller.admin.socialinsurance.vo.HrSocialInsurancePageReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.socialinsurance.vo.HrSocialInsuranceSaveReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.socialinsurance.vo.SocialInsuranceDetailRespVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.socialinsurance.HrSocialInsuranceBaseDO;
import cn.iocoder.yudao.module.hr.dal.mysql.socialinsurance.HrSocialInsuranceBaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hr.enums.ErrorCodeConstants.*;

@Service
@Validated
public class HrSocialInsuranceServiceImpl implements HrSocialInsuranceService {

    private static final BigDecimal DEFAULT_PERSONAL_PENSION_RATE = new BigDecimal("0.0800");
    private static final BigDecimal DEFAULT_COMPANY_PENSION_RATE = new BigDecimal("0.1600");
    private static final BigDecimal DEFAULT_PERSONAL_MEDICAL_RATE = new BigDecimal("0.0200");
    private static final BigDecimal DEFAULT_COMPANY_MEDICAL_RATE = new BigDecimal("0.0800");
    private static final BigDecimal DEFAULT_PERSONAL_UNEMP_RATE = new BigDecimal("0.0050");
    private static final BigDecimal DEFAULT_COMPANY_UNEMP_RATE = new BigDecimal("0.0050");
    private static final BigDecimal DEFAULT_COMPANY_INJURY_RATE = new BigDecimal("0.0020");
    private static final BigDecimal DEFAULT_COMPANY_MATERNITY_RATE = new BigDecimal("0.0080");
    private static final BigDecimal DEFAULT_PERSONAL_FUND_RATE = new BigDecimal("0.0700");
    private static final BigDecimal DEFAULT_COMPANY_FUND_RATE = new BigDecimal("0.0700");

    @Resource
    private HrSocialInsuranceBaseMapper socialInsuranceBaseMapper;

    @Override
    public Long createSocialInsurance(HrSocialInsuranceSaveReqVO createReqVO) {
        validateUnique(createReqVO.getEmployeeId(), createReqVO.getYear(), null);
        HrSocialInsuranceBaseDO socialInsurance = BeanUtils.toBean(createReqVO, HrSocialInsuranceBaseDO.class);
        fillDefaultRates(socialInsurance);
        socialInsurance.setStatus(0);
        socialInsuranceBaseMapper.insert(socialInsurance);
        return socialInsurance.getId();
    }

    private void validateUnique(Long employeeId, Integer year, Long id) {
        HrSocialInsuranceBaseDO existing = socialInsuranceBaseMapper.selectByEmployeeAndYear(employeeId, year);
        if (existing == null) {
            return;
        }
        if (id == null || !existing.getId().equals(id)) {
            throw exception(HR_SOCIAL_INSURANCE_EXISTS);
        }
    }

    private void fillDefaultRates(HrSocialInsuranceBaseDO socialInsurance) {
        if (socialInsurance.getPersonalPensionRate() == null) {
            socialInsurance.setPersonalPensionRate(DEFAULT_PERSONAL_PENSION_RATE);
        }
        if (socialInsurance.getCompanyPensionRate() == null) {
            socialInsurance.setCompanyPensionRate(DEFAULT_COMPANY_PENSION_RATE);
        }
        if (socialInsurance.getPersonalMedicalRate() == null) {
            socialInsurance.setPersonalMedicalRate(DEFAULT_PERSONAL_MEDICAL_RATE);
        }
        if (socialInsurance.getCompanyMedicalRate() == null) {
            socialInsurance.setCompanyMedicalRate(DEFAULT_COMPANY_MEDICAL_RATE);
        }
        if (socialInsurance.getPersonalUnemploymentRate() == null) {
            socialInsurance.setPersonalUnemploymentRate(DEFAULT_PERSONAL_UNEMP_RATE);
        }
        if (socialInsurance.getCompanyUnemploymentRate() == null) {
            socialInsurance.setCompanyUnemploymentRate(DEFAULT_COMPANY_UNEMP_RATE);
        }
        if (socialInsurance.getCompanyWorkInjuryRate() == null) {
            socialInsurance.setCompanyWorkInjuryRate(DEFAULT_COMPANY_INJURY_RATE);
        }
        if (socialInsurance.getCompanyMaternityRate() == null) {
            socialInsurance.setCompanyMaternityRate(DEFAULT_COMPANY_MATERNITY_RATE);
        }
        if (socialInsurance.getPersonalHousingFundRate() == null) {
            socialInsurance.setPersonalHousingFundRate(DEFAULT_PERSONAL_FUND_RATE);
        }
        if (socialInsurance.getCompanyHousingFundRate() == null) {
            socialInsurance.setCompanyHousingFundRate(DEFAULT_COMPANY_FUND_RATE);
        }
    }

    @Override
    public void updateSocialInsurance(HrSocialInsuranceSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getId());
        validateUnique(updateReqVO.getEmployeeId(), updateReqVO.getYear(), updateReqVO.getId());
        HrSocialInsuranceBaseDO updateObj = BeanUtils.toBean(updateReqVO, HrSocialInsuranceBaseDO.class);
        socialInsuranceBaseMapper.updateById(updateObj);
    }

    @Override
    public void deleteSocialInsurance(Long id) {
        validateExists(id);
        socialInsuranceBaseMapper.deleteById(id);
    }

    private HrSocialInsuranceBaseDO validateExists(Long id) {
        HrSocialInsuranceBaseDO socialInsurance = socialInsuranceBaseMapper.selectById(id);
        if (socialInsurance == null) {
            throw exception(HR_SOCIAL_INSURANCE_NOT_EXISTS);
        }
        return socialInsurance;
    }

    @Override
    public HrSocialInsuranceBaseDO getSocialInsurance(Long id) {
        return socialInsuranceBaseMapper.selectById(id);
    }

    @Override
    public PageResult<HrSocialInsuranceBaseDO> getSocialInsurancePage(HrSocialInsurancePageReqVO pageReqVO) {
        return socialInsuranceBaseMapper.selectPage(pageReqVO);
    }

    @Override
    public Long adjustBase(HrSocialInsuranceSaveReqVO reqVO) {
        // 旧记录状态变更为已调整
        Integer year = reqVO.getYear() != null ? reqVO.getYear() : LocalDate.now().getYear();
        HrSocialInsuranceBaseDO oldBase = socialInsuranceBaseMapper.selectByEmployeeAndYear(reqVO.getEmployeeId(), year);
        if (oldBase != null) {
            HrSocialInsuranceBaseDO updateOld = new HrSocialInsuranceBaseDO();
            updateOld.setId(oldBase.getId());
            updateOld.setStatus(1);
            socialInsuranceBaseMapper.updateById(updateOld);
        }
        // 创建新年度记录
        return createSocialInsurance(reqVO);
    }

    @Override
    public SocialInsuranceDetailRespVO calculateMonthly(Long employeeId) {
        Integer year = LocalDate.now().getYear();
        HrSocialInsuranceBaseDO base = socialInsuranceBaseMapper.selectByEmployeeAndYear(employeeId, year);
        if (base == null) {
            return null;
        }
        SocialInsuranceDetailRespVO resp = new SocialInsuranceDetailRespVO();
        resp.setEmployeeId(employeeId);
        resp.setPensionBase(base.getPensionBase());

        BigDecimal pensionBase = base.getPensionBase() != null ? base.getPensionBase() : BigDecimal.ZERO;
        BigDecimal medicalBase = base.getMedicalBase() != null ? base.getMedicalBase() : BigDecimal.ZERO;
        BigDecimal unempBase = base.getUnemploymentBase() != null ? base.getUnemploymentBase() : BigDecimal.ZERO;
        BigDecimal injuryBase = base.getWorkInjuryBase() != null ? base.getWorkInjuryBase() : BigDecimal.ZERO;
        BigDecimal maternityBase = base.getMaternityBase() != null ? base.getMaternityBase() : BigDecimal.ZERO;
        BigDecimal fundBase = base.getHousingFundBase() != null ? base.getHousingFundBase() : BigDecimal.ZERO;

        resp.setPersonalPension(pensionBase.multiply(base.getPersonalPensionRate()).setScale(2, RoundingMode.HALF_UP));
        resp.setCompanyPension(pensionBase.multiply(base.getCompanyPensionRate()).setScale(2, RoundingMode.HALF_UP));
        resp.setPersonalMedical(medicalBase.multiply(base.getPersonalMedicalRate()).setScale(2, RoundingMode.HALF_UP));
        resp.setCompanyMedical(medicalBase.multiply(base.getCompanyMedicalRate()).setScale(2, RoundingMode.HALF_UP));
        resp.setPersonalUnemployment(unempBase.multiply(base.getPersonalUnemploymentRate()).setScale(2, RoundingMode.HALF_UP));
        resp.setCompanyUnemployment(unempBase.multiply(base.getCompanyUnemploymentRate()).setScale(2, RoundingMode.HALF_UP));
        resp.setCompanyWorkInjury(injuryBase.multiply(base.getCompanyWorkInjuryRate()).setScale(2, RoundingMode.HALF_UP));
        resp.setCompanyMaternity(maternityBase.multiply(base.getCompanyMaternityRate()).setScale(2, RoundingMode.HALF_UP));
        resp.setPersonalHousingFund(fundBase.multiply(base.getPersonalHousingFundRate()).setScale(2, RoundingMode.HALF_UP));
        resp.setCompanyHousingFund(fundBase.multiply(base.getCompanyHousingFundRate()).setScale(2, RoundingMode.HALF_UP));

        BigDecimal personalTotal = resp.getPersonalPension().add(resp.getPersonalMedical())
                .add(resp.getPersonalUnemployment()).add(resp.getPersonalHousingFund());
        BigDecimal companyTotal = resp.getCompanyPension().add(resp.getCompanyMedical())
                .add(resp.getCompanyUnemployment()).add(resp.getCompanyWorkInjury())
                .add(resp.getCompanyMaternity()).add(resp.getCompanyHousingFund());
        resp.setPersonalTotal(personalTotal);
        resp.setCompanyTotal(companyTotal);
        return resp;
    }

}