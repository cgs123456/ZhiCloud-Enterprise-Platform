package cn.zhicloud.module.hr.service.hrm.insurance;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.ip.core.utils.AreaUtils;
import cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo.*;
import cn.zhicloud.module.hr.dal.dataobject.insurance.HrmInsuranceEmployeeInfoDO;
import cn.zhicloud.module.hr.dal.dataobject.insurance.HrmInsuranceSchemeDO;
import cn.zhicloud.module.hr.dal.dataobject.insurance.HrmInsuranceSchemeProjectDO;
import cn.zhicloud.module.hr.dal.mysql.insurance.HrmInsuranceEmployeeInfoMapper;
import cn.zhicloud.module.hr.dal.mysql.insurance.HrmInsuranceSchemeMapper;
import cn.zhicloud.module.hr.dal.mysql.insurance.HrmInsuranceSchemeProjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.hr.enums.ErrorCodeConstants.*;

/**
 * HRM 社保方案 Service 实现类
 *
 * 口径（对齐前端 InsuranceSchemeForm）：
 * * 项目分组：type 10=公积金，其余（含 11 公积金自定义）归公积金组？——前端 PROVIDENT_FUND_PROJECT_TYPES 仅 [10]，
 *   SOCIAL 为 1-8，自定义 9 归社保组、11 归公积金组（isCustomProject 与 customType 决定归属，见前端 addProject）。
 *   服务端拆分：社保组 = type 1-9，公积金组 = type 10-11。
 * * 金额计算：方案类型 1（比例）时 corporateAmount = baseAmount×corporateRate、personalAmount = baseAmount×personalRate；
 *   方案类型 2（固定金额）时取前端行内金额原值。头汇总四金额 = 组内行求和。
 * * 修改方案整组替换项目行；不触碰员工参保数据（前端确认文案同口径）。
 * * 删除方案：被 hr_insurance_employee_info 引用（deleted=0）时拒绝。
 *
 * @author zhicloud
 */
@Service
@Validated
public class HrmInsuranceSchemeServiceImpl implements HrmInsuranceSchemeService {

    /** 公积金项目类型（含自定义）归组：10-11 */
    private static final int PROVIDENT_FUND_TYPE_MIN = 10;

    @Resource
    private HrmInsuranceSchemeMapper schemeMapper;
    @Resource
    private HrmInsuranceSchemeProjectMapper schemeProjectMapper;
    @Resource
    private HrmInsuranceEmployeeInfoMapper employeeInfoMapper;

    @Override
    public List<InsuranceSchemeVO> getSchemeList() {
        List<HrmInsuranceSchemeDO> schemes = schemeMapper.selectListAll();
        if (CollUtil.isEmpty(schemes)) {
            return Collections.emptyList();
        }
        return buildSchemeVOList(schemes, true);
    }

    @Override
    public List<InsuranceSchemeVO> getSchemeSimpleList() {
        List<HrmInsuranceSchemeDO> schemes = schemeMapper.selectListAll();
        if (CollUtil.isEmpty(schemes)) {
            return Collections.emptyList();
        }
        // 下拉消费：带项目行（选择方案后展示金额），不带 useCount/monthRecordCount 统计
        return buildSchemeVOList(schemes, false);
    }

    @Override
    public InsuranceSchemeVO getScheme(Long id) {
        HrmInsuranceSchemeDO scheme = validateSchemeExists(id);
        return buildSchemeVOList(Collections.singletonList(scheme), true).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createScheme(InsuranceSchemeVO reqVO) {
        validateSchemeNameUnique(null, reqVO.getName());
        HrmInsuranceSchemeDO scheme = BeanUtils.toBean(reqVO, HrmInsuranceSchemeDO.class);
        if (scheme.getType() == null) {
            scheme.setType(1);
        }
        // 头汇总金额由行重算（不信任前端头字段）
        List<InsuranceSchemeProjectVO> projects = reqVO.getProjectList();
        applySchemeAmounts(scheme, projects);
        schemeMapper.insert(scheme);
        saveSchemeProjects(scheme.getId(), projects);
        return scheme.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScheme(InsuranceSchemeVO reqVO) {
        validateSchemeExists(reqVO.getId());
        validateSchemeNameUnique(reqVO.getId(), reqVO.getName());
        HrmInsuranceSchemeDO scheme = BeanUtils.toBean(reqVO, HrmInsuranceSchemeDO.class);
        List<InsuranceSchemeProjectVO> projects = reqVO.getProjectList();
        applySchemeAmounts(scheme, projects);
        schemeMapper.updateById(scheme);
        // 项目整组替换
        schemeProjectMapper.deleteBySchemeId(reqVO.getId());
        saveSchemeProjects(reqVO.getId(), projects);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScheme(Long id) {
        validateSchemeExists(id);
        if (employeeInfoMapper.selectCountBySchemeId(id) > 0) {
            throw exception(HRM_INSURANCE_SCHEME_USED);
        }
        schemeProjectMapper.deleteBySchemeId(id);
        schemeMapper.deleteById(id);
    }

    // ========== 私有 ==========

    private List<InsuranceSchemeVO> buildSchemeVOList(List<HrmInsuranceSchemeDO> schemes, boolean withUseCount) {
        List<Long> schemeIds = schemes.stream().map(HrmInsuranceSchemeDO::getId).collect(Collectors.toList());
        Map<Long, List<HrmInsuranceSchemeProjectDO>> projectMap = schemeProjectMapper
                .selectListBySchemeIds(schemeIds).stream()
                .collect(Collectors.groupingBy(HrmInsuranceSchemeProjectDO::getSchemeId));
        // 使用人数：一次按方案分组统计（避免 N+1）
        Map<Long, Long> useCountMap = Collections.emptyMap();
        if (withUseCount) {
            useCountMap = employeeInfoMapper.selectList(
                            new cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX<HrmInsuranceEmployeeInfoDO>()
                                    .in(HrmInsuranceEmployeeInfoDO::getSchemeId, schemeIds))
                    .stream().collect(Collectors.groupingBy(HrmInsuranceEmployeeInfoDO::getSchemeId, Collectors.counting()));
        }
        List<InsuranceSchemeVO> result = new ArrayList<>(schemes.size());
        for (HrmInsuranceSchemeDO scheme : schemes) {
            InsuranceSchemeVO vo = BeanUtils.toBean(scheme, InsuranceSchemeVO.class);
            vo.setAreaName(scheme.getAreaId() != null ? AreaUtils.format(scheme.getAreaId().intValue()) : null);
            List<HrmInsuranceSchemeProjectDO> projects = projectMap.getOrDefault(scheme.getId(), Collections.emptyList());
            List<InsuranceSchemeProjectVO> projectVOs = BeanUtils.toBean(projects, InsuranceSchemeProjectVO.class);
            vo.setProjectList(projectVOs);
            vo.setSocialSecurityProjectList(projectVOs.stream()
                    .filter(p -> p.getType() != null && p.getType() < PROVIDENT_FUND_TYPE_MIN)
                    .collect(Collectors.toList()));
            vo.setProvidentFundProjectList(projectVOs.stream()
                    .filter(p -> p.getType() != null && p.getType() >= PROVIDENT_FUND_TYPE_MIN)
                    .collect(Collectors.toList()));
            if (withUseCount) {
                vo.setUseCount(useCountMap.getOrDefault(scheme.getId(), 0L));
                vo.setMonthRecordCount(0L); // 月度社保表域未接线，诚实置 0（不伪造）
            }
            result.add(vo);
        }
        return result;
    }

    /**
     * 行金额与头汇总重算（比例模式按 基数×比例；金额模式取行值）
     */
    private void applySchemeAmounts(HrmInsuranceSchemeDO scheme, List<InsuranceSchemeProjectVO> projects) {
        BigDecimal personalInsurance = BigDecimal.ZERO, corporateInsurance = BigDecimal.ZERO;
        BigDecimal personalProvident = BigDecimal.ZERO, corporateProvident = BigDecimal.ZERO;
        if (projects != null) {
            for (InsuranceSchemeProjectVO project : projects) {
                if (Integer.valueOf(1).equals(scheme.getType())) {
                    if (project.getBaseAmount() != null && project.getCorporateRate() != null) {
                        project.setCorporateAmount(project.getBaseAmount().multiply(project.getCorporateRate()));
                    }
                    if (project.getBaseAmount() != null && project.getPersonalRate() != null) {
                        project.setPersonalAmount(project.getBaseAmount().multiply(project.getPersonalRate()));
                    }
                }
                boolean provident = project.getType() != null && project.getType() >= PROVIDENT_FUND_TYPE_MIN;
                if (project.getPersonalAmount() != null) {
                    if (provident) {
                        personalProvident = personalProvident.add(project.getPersonalAmount());
                    } else {
                        personalInsurance = personalInsurance.add(project.getPersonalAmount());
                    }
                }
                if (project.getCorporateAmount() != null) {
                    if (provident) {
                        corporateProvident = corporateProvident.add(project.getCorporateAmount());
                    } else {
                        corporateInsurance = corporateInsurance.add(project.getCorporateAmount());
                    }
                }
            }
        }
        scheme.setPersonalInsuranceAmount(personalInsurance);
        scheme.setCorporateInsuranceAmount(corporateInsurance);
        scheme.setPersonalProvidentFundAmount(personalProvident);
        scheme.setCorporateProvidentFundAmount(corporateProvident);
    }

    private void saveSchemeProjects(Long schemeId, List<InsuranceSchemeProjectVO> projects) {
        if (CollUtil.isEmpty(projects)) {
            return;
        }
        for (InsuranceSchemeProjectVO project : projects) {
            HrmInsuranceSchemeProjectDO row = BeanUtils.toBean(project, HrmInsuranceSchemeProjectDO.class);
            row.setId(null);
            row.setSchemeId(schemeId);
            schemeProjectMapper.insert(row);
        }
    }

    private HrmInsuranceSchemeDO validateSchemeExists(Long id) {
        HrmInsuranceSchemeDO scheme = schemeMapper.selectById(id);
        if (scheme == null) {
            throw exception(HRM_INSURANCE_SCHEME_NOT_EXISTS);
        }
        return scheme;
    }

    private void validateSchemeNameUnique(Long id, String name) {
        HrmInsuranceSchemeDO exist = schemeMapper.selectByName(name);
        if (exist != null && (id == null || !exist.getId().equals(id))) {
            throw exception(HRM_INSURANCE_SCHEME_NAME_DUPLICATE);
        }
    }

}
