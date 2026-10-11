package cn.zhicloud.module.hr.service.hrm.insurance;

import cn.hutool.core.util.StrUtil;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo.HrmInsuranceEmployeeInfoVO;
import cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo.HrmInsuranceEmployeeUpdateSchemeReqVO;
import cn.zhicloud.module.hr.dal.dataobject.employee.HrEmployeeDO;
import cn.zhicloud.module.hr.dal.dataobject.insurance.HrmInsuranceEmployeeInfoDO;
import cn.zhicloud.module.hr.dal.dataobject.insurance.HrmInsuranceSchemeDO;
import cn.zhicloud.module.hr.dal.mysql.employee.HrEmployeeMapper;
import cn.zhicloud.module.hr.dal.mysql.insurance.HrmInsuranceEmployeeInfoMapper;
import cn.zhicloud.module.hr.dal.mysql.insurance.HrmInsuranceSchemeMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.hr.enums.ErrorCodeConstants.HRM_EMPLOYEE_NOT_EXISTS;
import static cn.zhicloud.module.hr.enums.ErrorCodeConstants.HRM_INSURANCE_SCHEME_NOT_EXISTS;

/**
 * HRM 员工参保信息 Service 实现类
 *
 * 单一真相（融合纪律）：社保账号/公积金账号的物理列在 hr_employee（V161 social_security_number/
 * accumulation_fund_number），本域只写这两列 + 参保特有字段（首次标志/起缴月份/方案）落 hr_insurance_employee_info；
 * 不建重复账号列，避免「多套账」。
 *
 * @author zhicloud
 */
@Service
@Validated
public class HrmInsuranceEmployeeInfoServiceImpl implements HrmInsuranceEmployeeInfoService {

    @Resource
    private HrmInsuranceEmployeeInfoMapper employeeInfoMapper;
    @Resource
    private HrEmployeeMapper employeeMapper;
    @Resource
    private HrmInsuranceSchemeMapper schemeMapper;

    @Override
    public HrmInsuranceEmployeeInfoVO getEmployeeInfo(Long employeeId) {
        HrmInsuranceEmployeeInfoDO info = employeeInfoMapper.selectByEmployeeId(employeeId);
        if (info == null) {
            return null;
        }
        return buildVO(info);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveEmployeeInfo(HrmInsuranceEmployeeInfoVO reqVO) {
        validateEmployeeExists(reqVO.getEmployeeId());
        if (reqVO.getSchemeId() != null) {
            validateSchemeExists(reqVO.getSchemeId());
        }
        HrmInsuranceEmployeeInfoDO info = employeeInfoMapper.selectByEmployeeId(reqVO.getEmployeeId());
        boolean isNew = info == null;
        if (isNew) {
            info = new HrmInsuranceEmployeeInfoDO();
            info.setEmployeeId(reqVO.getEmployeeId());
        }
        info.setFirstSocialSecurity(reqVO.getFirstSocialSecurity());
        info.setFirstAccumulationFund(reqVO.getFirstAccumulationFund());
        info.setSocialSecurityStartMonth(reqVO.getSocialSecurityStartMonth());
        info.setSchemeId(reqVO.getSchemeId());
        if (isNew) {
            employeeInfoMapper.insert(info);
        } else {
            employeeInfoMapper.updateById(info);
        }
        // 账号双写镜像 hr_employee 两列（非 null 才更新，空串按清空处理）
        syncEmployeeAccounts(reqVO.getEmployeeId(), reqVO.getSocialSecurityNumber(), reqVO.getAccumulationFundNumber());
        return info.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEmployeeScheme(HrmInsuranceEmployeeUpdateSchemeReqVO reqVO) {
        validateEmployeeExists(reqVO.getEmployeeId());
        validateSchemeExists(reqVO.getSchemeId());
        HrmInsuranceEmployeeInfoDO info = employeeInfoMapper.selectByEmployeeId(reqVO.getEmployeeId());
        if (info == null) {
            info = HrmInsuranceEmployeeInfoDO.builder()
                    .employeeId(reqVO.getEmployeeId())
                    .schemeId(reqVO.getSchemeId())
                    .build();
            employeeInfoMapper.insert(info);
        } else {
            employeeInfoMapper.update(null, new LambdaUpdateWrapper<HrmInsuranceEmployeeInfoDO>()
                    .eq(HrmInsuranceEmployeeInfoDO::getId, info.getId())
                    .set(HrmInsuranceEmployeeInfoDO::getSchemeId, reqVO.getSchemeId()));
        }
    }

    private HrmInsuranceEmployeeInfoVO buildVO(HrmInsuranceEmployeeInfoDO info) {
        HrmInsuranceEmployeeInfoVO vo = BeanUtils.toBean(info, HrmInsuranceEmployeeInfoVO.class);
        HrEmployeeDO employee = employeeMapper.selectById(info.getEmployeeId());
        if (employee != null) {
            vo.setSocialSecurityNumber(employee.getSocialSecurityNumber());
            vo.setAccumulationFundNumber(employee.getAccumulationFundNumber());
        }
        if (info.getSchemeId() != null) {
            HrmInsuranceSchemeDO scheme = schemeMapper.selectById(info.getSchemeId());
            if (scheme != null) {
                vo.setSchemeName(scheme.getName());
            }
        }
        return vo;
    }

    private void syncEmployeeAccounts(Long employeeId, String social, String fund) {
        if (social == null && fund == null) {
            return;
        }
        LambdaUpdateWrapper<HrEmployeeDO> update = new LambdaUpdateWrapper<HrEmployeeDO>()
                .eq(HrEmployeeDO::getId, employeeId);
        if (social != null) {
            update.set(HrEmployeeDO::getSocialSecurityNumber, StrUtil.emptyToNull(social));
        }
        if (fund != null) {
            update.set(HrEmployeeDO::getAccumulationFundNumber, StrUtil.emptyToNull(fund));
        }
        employeeMapper.update(null, update);
    }

    private void validateEmployeeExists(Long employeeId) {
        if (employeeMapper.selectById(employeeId) == null) {
            throw exception(HRM_EMPLOYEE_NOT_EXISTS);
        }
    }

    private void validateSchemeExists(Long schemeId) {
        if (schemeMapper.selectById(schemeId) == null) {
            throw exception(HRM_INSURANCE_SCHEME_NOT_EXISTS);
        }
    }

}
