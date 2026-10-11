package cn.zhicloud.module.hr.service.hrm.insurance;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.framework.common.pojo.PageParam;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.ip.core.utils.AreaUtils;
import cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo.*;
import cn.zhicloud.module.hr.dal.dataobject.employee.HrEmployeeDO;
import cn.zhicloud.module.hr.dal.dataobject.insurance.*;
import cn.zhicloud.module.hr.dal.mysql.employee.HrEmployeeMapper;
import cn.zhicloud.module.hr.dal.mysql.insurance.*;
import cn.zhicloud.module.system.api.dept.DeptApi;
import cn.zhicloud.module.system.api.dept.dto.DeptRespDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
 * HRM 保险域月度社保 Service 实现类
 *
 * 口径（对齐前端 views/hrm/insurance/month-record）：
 * * 月度社保表：首月按指定年月创建；次月在最近一月基础上顺延（重复月份拒绝）。
 * * 添加参保人员：按员工绑定的社保方案生成月度记录（社保/公积金两组项目快照 + 四项金额汇总），幂等跳过已存在。
 * * 表头汇总：参保人数/停止参保人数 + 四项金额，添加/停止/修改后重算。
 * * 员工 enrich：姓名/工号/性别/年龄/手机/证件/部门名/职位/入职状态/员工状态/入职时间 + 方案名/参保地区/账号（镜像 hr_employee）。
 *
 * @author zhicloud
 */
@Service
@Validated
public class HrmInsuranceMonthServiceImpl implements HrmInsuranceMonthService {

    private static final int PROVIDENT_FUND_TYPE_MIN = 10;
    private static final int EMP_STATUS_STOPPED = 0;

    @Resource
    private HrmInsuranceMonthRecordMapper hrmInsuranceMonthRecordMapper;
    @Resource
    private HrmInsuranceMonthEmployeeRecordMapper hrmInsuranceMonthEmployeeRecordMapper;
    @Resource
    private HrmInsuranceEmployeeInfoMapper hrmInsuranceEmployeeInfoMapper;
    @Resource
    private HrmInsuranceSchemeMapper hrmInsuranceSchemeMapper;
    @Resource
    private HrmInsuranceSchemeProjectMapper hrmInsuranceSchemeProjectMapper;
    @Resource
    private HrEmployeeMapper hrmEmployeeMapper;
    @Resource
    private DeptApi deptApi;

    // ========== 月度社保表 ==========

    @Override
    public List<InsuranceMonthRecordVO> getMonthRecordList(Integer year) {
        return BeanUtils.toBean(hrmInsuranceMonthRecordMapper.selectListByYear(year), InsuranceMonthRecordVO.class);
    }

    @Override
    public InsuranceMonthRecordVO getMonthRecord(Long id) {
        return BeanUtils.toBean(validateMonthRecordExists(id), InsuranceMonthRecordVO.class);
    }

    @Override
    public InsuranceMonthRecordVO getLastMonthRecord() {
        return BeanUtils.toBean(hrmInsuranceMonthRecordMapper.selectLast(), InsuranceMonthRecordVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFirstMonthRecord(InsuranceMonthRecordCreateReqVO reqVO) {
        if (hrmInsuranceMonthRecordMapper.selectByYearMonth(reqVO.getYear(), reqVO.getMonth()) != null) {
            throw exception(HRM_INSURANCE_MONTH_RECORD_EXISTS);
        }
        HrmInsuranceMonthRecordDO record = HrmInsuranceMonthRecordDO.builder()
                .title(String.format("%d年%d月社保表", reqVO.getYear(), reqVO.getMonth()))
                .year(reqVO.getYear())
                .month(reqVO.getMonth())
                .insuredEmployeeCount(0)
                .stoppedEmployeeCount(0)
                .status(0)
                .build();
        hrmInsuranceMonthRecordMapper.insert(record);
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createNextMonthRecord() {
        HrmInsuranceMonthRecordDO last = hrmInsuranceMonthRecordMapper.selectLast();
        int year;
        int month;
        if (last == null) {
            year = java.time.LocalDate.now().getYear();
            month = java.time.LocalDate.now().getMonthValue();
        } else if (last.getMonth() == 12) {
            year = last.getYear() + 1;
            month = 1;
        } else {
            year = last.getYear();
            month = last.getMonth() + 1;
        }
        if (hrmInsuranceMonthRecordMapper.selectByYearMonth(year, month) != null) {
            throw exception(HRM_INSURANCE_MONTH_RECORD_EXISTS);
        }
        HrmInsuranceMonthRecordDO record = HrmInsuranceMonthRecordDO.builder()
                .title(String.format("%d年%d月社保表", year, month))
                .year(year)
                .month(month)
                .insuredEmployeeCount(0)
                .stoppedEmployeeCount(0)
                .status(0)
                .build();
        hrmInsuranceMonthRecordMapper.insert(record);
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMonthRecord(Long id) {
        validateMonthRecordExists(id);
        hrmInsuranceMonthEmployeeRecordMapper.delete(new LambdaQueryWrapper<HrmInsuranceMonthEmployeeRecordDO>()
                .eq(HrmInsuranceMonthEmployeeRecordDO::getMonthRecordId, id));
        hrmInsuranceMonthRecordMapper.deleteById(id);
    }

    // ========== 员工月度社保记录 ==========

    @Override
    public PageResult<InsuranceMonthEmployeeRecordVO> getMonthEmployeeRecordPage(Integer pageNo, Integer pageSize, Long monthRecordId) {
        if (monthRecordId == null) {
            return PageResult.empty();
        }
        PageParam pageParam = new PageParam();
        pageParam.setPageNo(pageNo != null ? pageNo : 1);
        pageParam.setPageSize(pageSize != null ? pageSize : 10);
        PageResult<HrmInsuranceMonthEmployeeRecordDO> pageResult = hrmInsuranceMonthEmployeeRecordMapper.selectPage(
                pageParam, new LambdaQueryWrapper<HrmInsuranceMonthEmployeeRecordDO>()
                        .eq(HrmInsuranceMonthEmployeeRecordDO::getMonthRecordId, monthRecordId)
                        .orderByDesc(HrmInsuranceMonthEmployeeRecordDO::getId));
        return new PageResult<>(enrichRecords(pageResult.getList()), pageResult.getTotal());
    }

    @Override
    public InsuranceMonthEmployeeRecordVO getMonthEmployeeRecord(Long id) {
        HrmInsuranceMonthEmployeeRecordDO record = hrmInsuranceMonthEmployeeRecordMapper.selectById(id);
        if (record == null) {
            throw exception(HRM_INSURANCE_MONTH_EMPLOYEE_NOT_EXISTS);
        }
        return enrichRecords(Collections.singletonList(record)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMonthEmployeeRecordList(InsuranceMonthEmployeeCreateListReqVO reqVO) {
        HrmInsuranceMonthRecordDO monthRecord = validateMonthRecordExists(reqVO.getMonthRecordId());
        int created = 0;
        for (Long employeeId : reqVO.getEmployeeIds()) {
            if (hrmInsuranceMonthEmployeeRecordMapper.selectByMonthRecordAndEmployee(reqVO.getMonthRecordId(), employeeId) != null) {
                continue; // 幂等
            }
            HrEmployeeDO employee = hrmEmployeeMapper.selectById(employeeId);
            if (employee == null) {
                continue;
            }
            HrmInsuranceMonthEmployeeRecordDO record = buildMonthEmployeeRecord(monthRecord, employee);
            hrmInsuranceMonthEmployeeRecordMapper.insert(record);
            created++;
        }
        recalcMonthRecord(monthRecord);
        return (long) created;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMonthEmployeeRecord(InsuranceMonthEmployeeUpdateReqVO reqVO) {
        HrmInsuranceMonthEmployeeRecordDO record = hrmInsuranceMonthEmployeeRecordMapper.selectById(reqVO.getId());
        if (record == null) {
            throw exception(HRM_INSURANCE_MONTH_EMPLOYEE_NOT_EXISTS);
        }
        // 项目整组替换 + 四项金额重算
        List<Map<String, Object>> projects = reqVO.getProjects() == null ? Collections.emptyList()
                : reqVO.getProjects().stream().map(this::toProjectMap).collect(Collectors.toList());
        List<Map<String, Object>> social = projects.stream()
                .filter(p -> toInt(p.get("type")) != null && toInt(p.get("type")) < PROVIDENT_FUND_TYPE_MIN)
                .collect(Collectors.toList());
        List<Map<String, Object>> fund = projects.stream()
                .filter(p -> toInt(p.get("type")) != null && toInt(p.get("type")) >= PROVIDENT_FUND_TYPE_MIN)
                .collect(Collectors.toList());
        BigDecimal personalInsurance = sumAmounts(social, "personalAmount");
        BigDecimal corporateInsurance = sumAmounts(social, "corporateAmount");
        BigDecimal personalFund = sumAmounts(fund, "personalAmount");
        BigDecimal corporateFund = sumAmounts(fund, "corporateAmount");
        // json 列必须走 DO + updateById（JacksonTypeHandler 生效）；LambdaUpdateWrapper.set() 会绕开 typeHandler
        HrmInsuranceMonthEmployeeRecordDO updateObj = new HrmInsuranceMonthEmployeeRecordDO();
        updateObj.setId(reqVO.getId());
        updateObj.setSchemeId(reqVO.getSchemeId());
        updateObj.setSocialSecurityProjects(social);
        updateObj.setProvidentFundProjects(fund);
        updateObj.setPersonalInsuranceAmount(personalInsurance);
        updateObj.setCorporateInsuranceAmount(corporateInsurance);
        updateObj.setPersonalProvidentFundAmount(personalFund);
        updateObj.setCorporateProvidentFundAmount(corporateFund);
        hrmInsuranceMonthEmployeeRecordMapper.updateById(updateObj);
        recalcMonthRecord(hrmInsuranceMonthRecordMapper.selectById(record.getMonthRecordId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void stopMonthEmployeeRecordList(InsuranceMonthEmployeeStopListReqVO reqVO) {
        if (CollUtil.isEmpty(reqVO.getIds())) {
            return;
        }
        Long monthRecordId = null;
        for (Long id : reqVO.getIds()) {
            HrmInsuranceMonthEmployeeRecordDO record = hrmInsuranceMonthEmployeeRecordMapper.selectById(id);
            if (record == null) {
                continue;
            }
            monthRecordId = record.getMonthRecordId();
            hrmInsuranceMonthEmployeeRecordMapper.update(null, new LambdaUpdateWrapper<HrmInsuranceMonthEmployeeRecordDO>()
                    .eq(HrmInsuranceMonthEmployeeRecordDO::getId, id)
                    .set(HrmInsuranceMonthEmployeeRecordDO::getStatus, EMP_STATUS_STOPPED));
        }
        if (monthRecordId != null) {
            recalcMonthRecord(hrmInsuranceMonthRecordMapper.selectById(monthRecordId));
        }
    }

    @Override
    public List<Long> getUninsuredEmployeeIdList(Long monthRecordId) {
        validateMonthRecordExists(monthRecordId);
        Set<Long> insured = hrmInsuranceMonthEmployeeRecordMapper.selectListByMonthRecordId(monthRecordId).stream()
                .map(HrmInsuranceMonthEmployeeRecordDO::getEmployeeId).collect(Collectors.toSet());
        List<HrEmployeeDO> all = hrmEmployeeMapper.selectList(new LambdaQueryWrapper<HrEmployeeDO>()
                .in(HrEmployeeDO::getEntryStatus, Arrays.asList(1, 3)));
        return all.stream().map(HrEmployeeDO::getId).filter(id -> !insured.contains(id)).collect(Collectors.toList());
    }

    // ========== 私有：构建与重算 ==========

    private HrmInsuranceMonthEmployeeRecordDO buildMonthEmployeeRecord(HrmInsuranceMonthRecordDO monthRecord, HrEmployeeDO employee) {
        HrmInsuranceMonthEmployeeRecordDO record = HrmInsuranceMonthEmployeeRecordDO.builder()
                .monthRecordId(monthRecord.getId())
                .employeeId(employee.getId())
                .year(monthRecord.getYear())
                .month(monthRecord.getMonth())
                .status(1)
                .personalInsuranceAmount(BigDecimal.ZERO)
                .personalProvidentFundAmount(BigDecimal.ZERO)
                .corporateInsuranceAmount(BigDecimal.ZERO)
                .corporateProvidentFundAmount(BigDecimal.ZERO)
                .build();
        // 取员工参保方案 → 生成项目快照
        HrmInsuranceEmployeeInfoDO info = hrmInsuranceEmployeeInfoMapper.selectByEmployeeId(employee.getId());
        if (info == null || info.getSchemeId() == null) {
            record.setSocialSecurityProjects(Collections.emptyList());
            record.setProvidentFundProjects(Collections.emptyList());
            return record;
        }
        record.setSchemeId(info.getSchemeId());
        List<HrmInsuranceSchemeProjectDO> projects = hrmInsuranceSchemeProjectMapper.selectListBySchemeId(info.getSchemeId());
        List<Map<String, Object>> social = new ArrayList<>();
        List<Map<String, Object>> fund = new ArrayList<>();
        for (HrmInsuranceSchemeProjectDO project : projects) {
            Map<String, Object> map = projectToMap(project);
            if (project.getType() != null && project.getType() >= PROVIDENT_FUND_TYPE_MIN) {
                fund.add(map);
            } else {
                social.add(map);
            }
        }
        record.setSocialSecurityProjects(social);
        record.setProvidentFundProjects(fund);
        record.setPersonalInsuranceAmount(sumAmounts(social, "personalAmount"));
        record.setCorporateInsuranceAmount(sumAmounts(social, "corporateAmount"));
        record.setPersonalProvidentFundAmount(sumAmounts(fund, "personalAmount"));
        record.setCorporateProvidentFundAmount(sumAmounts(fund, "corporateAmount"));
        return record;
    }

    /**
     * 表头重算：参保人数（status=1）/停止人数（status=0）+ 四项金额汇总
     */
    private void recalcMonthRecord(HrmInsuranceMonthRecordDO monthRecord) {
        if (monthRecord == null) {
            return;
        }
        List<HrmInsuranceMonthEmployeeRecordDO> records =
                hrmInsuranceMonthEmployeeRecordMapper.selectListByMonthRecordId(monthRecord.getId());
        int insured = (int) records.stream().filter(r -> !Integer.valueOf(EMP_STATUS_STOPPED).equals(r.getStatus())).count();
        int stopped = (int) records.stream().filter(r -> Integer.valueOf(EMP_STATUS_STOPPED).equals(r.getStatus())).count();
        BigDecimal personalInsurance = records.stream().map(r -> nvl(r.getPersonalInsuranceAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal personalFund = records.stream().map(r -> nvl(r.getPersonalProvidentFundAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal corporateInsurance = records.stream().map(r -> nvl(r.getCorporateInsuranceAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal corporateFund = records.stream().map(r -> nvl(r.getCorporateProvidentFundAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        hrmInsuranceMonthRecordMapper.update(null, new LambdaUpdateWrapper<HrmInsuranceMonthRecordDO>()
                .eq(HrmInsuranceMonthRecordDO::getId, monthRecord.getId())
                .set(HrmInsuranceMonthRecordDO::getInsuredEmployeeCount, insured)
                .set(HrmInsuranceMonthRecordDO::getStoppedEmployeeCount, stopped)
                .set(HrmInsuranceMonthRecordDO::getPersonalInsuranceAmount, personalInsurance)
                .set(HrmInsuranceMonthRecordDO::getPersonalProvidentFundAmount, personalFund)
                .set(HrmInsuranceMonthRecordDO::getCorporateInsuranceAmount, corporateInsurance)
                .set(HrmInsuranceMonthRecordDO::getCorporateProvidentFundAmount, corporateFund));
    }

    // ========== 私有：enrich ==========

    private List<InsuranceMonthEmployeeRecordVO> enrichRecords(List<HrmInsuranceMonthEmployeeRecordDO> records) {
        if (CollUtil.isEmpty(records)) {
            return Collections.emptyList();
        }
        List<InsuranceMonthEmployeeRecordVO> voList = BeanUtils.toBean(records, InsuranceMonthEmployeeRecordVO.class);
        Set<Long> employeeIds = records.stream().map(HrmInsuranceMonthEmployeeRecordDO::getEmployeeId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, HrEmployeeDO> employeeMap = employeeIds.isEmpty() ? Collections.emptyMap()
                : hrmEmployeeMapper.selectByIds(employeeIds).stream()
                        .collect(Collectors.toMap(HrEmployeeDO::getId, e -> e, (a, b) -> a));
        Set<Long> deptIds = employeeMap.values().stream().map(HrEmployeeDO::getDeptId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, DeptRespDTO> deptMap = deptIds.isEmpty() ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        Set<Long> schemeIds = records.stream().map(HrmInsuranceMonthEmployeeRecordDO::getSchemeId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, HrmInsuranceSchemeDO> schemeMap = schemeIds.isEmpty() ? Collections.emptyMap()
                : hrmInsuranceSchemeMapper.selectByIds(schemeIds).stream()
                        .collect(Collectors.toMap(HrmInsuranceSchemeDO::getId, s -> s, (a, b) -> a));
        java.time.LocalDate today = java.time.LocalDate.now();
        for (int i = 0; i < records.size(); i++) {
            HrmInsuranceMonthEmployeeRecordDO record = records.get(i);
            InsuranceMonthEmployeeRecordVO vo = voList.get(i);
            HrEmployeeDO employee = employeeMap.get(record.getEmployeeId());
            if (employee != null) {
                vo.setEmployeeName(employee.getName());
                vo.setJobNumber(employee.getEmpNo());
                vo.setSex(employee.getSex());
                vo.setMobile(employee.getPhone());
                vo.setIdNumber(employee.getIdCard());
                vo.setDeptId(employee.getDeptId());
                vo.setPostName(employee.getPostName());
                vo.setEntryStatus(employee.getEntryStatus());
                vo.setEmployeeStatus(employee.getEmployeeStatus());
                vo.setEntryTime(employee.getEntryTime());
                vo.setSocialSecurityNumber(employee.getSocialSecurityNumber());
                vo.setAccumulationFundNumber(employee.getAccumulationFundNumber());
                if (employee.getBirthday() != null) {
                    vo.setAge(java.time.Period.between(employee.getBirthday().toLocalDate(), today).getYears());
                }
                DeptRespDTO dept = deptMap.get(employee.getDeptId());
                if (dept != null) {
                    vo.setDeptName(dept.getName());
                }
            }
            HrmInsuranceSchemeDO scheme = schemeMap.get(record.getSchemeId());
            if (scheme != null) {
                vo.setSchemeName(scheme.getName());
                vo.setAreaId(scheme.getAreaId());
                vo.setHouseType(scheme.getHouseholdType());
                vo.setSchemeType(scheme.getType());
                if (scheme.getAreaId() != null) {
                    vo.setAreaName(AreaUtils.format(scheme.getAreaId().intValue()));
                }
            }
            vo.setSocialSecurityProjectList(BeanUtils.toBean(record.getSocialSecurityProjects(), InsuranceMonthEmployeeProjectVO.class));
            vo.setProvidentFundProjectList(BeanUtils.toBean(record.getProvidentFundProjects(), InsuranceMonthEmployeeProjectVO.class));
        }
        return voList;
    }

    // ========== 私有：小工具 ==========

    private HrmInsuranceMonthRecordDO validateMonthRecordExists(Long id) {
        HrmInsuranceMonthRecordDO record = hrmInsuranceMonthRecordMapper.selectById(id);
        if (record == null) {
            throw exception(HRM_INSURANCE_MONTH_RECORD_NOT_EXISTS);
        }
        return record;
    }

    private Map<String, Object> projectToMap(HrmInsuranceSchemeProjectDO project) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("schemeProjectId", project.getId());
        map.put("type", project.getType());
        map.put("name", project.getName());
        map.put("baseAmount", project.getBaseAmount());
        map.put("corporateRate", project.getCorporateRate());
        map.put("personalRate", project.getPersonalRate());
        map.put("corporateAmount", project.getCorporateAmount());
        map.put("personalAmount", project.getPersonalAmount());
        return map;
    }

    private Map<String, Object> toProjectMap(InsuranceMonthEmployeeProjectVO project) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("schemeProjectId", project.getSchemeProjectId());
        map.put("type", project.getType());
        map.put("name", project.getName());
        map.put("baseAmount", project.getBaseAmount());
        map.put("corporateRate", project.getCorporateRate());
        map.put("personalRate", project.getPersonalRate());
        map.put("corporateAmount", project.getCorporateAmount());
        map.put("personalAmount", project.getPersonalAmount());
        return map;
    }

    private static BigDecimal sumAmounts(List<Map<String, Object>> projects, String key) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Map<String, Object> project : projects) {
            Object value = project.get(key);
            if (value != null) {
                sum = sum.add(new BigDecimal(value.toString()));
            }
        }
        return sum;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static Integer toInt(Object value) {
        if (value == null) {
            return null;
        }
        return (int) Double.parseDouble(value.toString());
    }

}
