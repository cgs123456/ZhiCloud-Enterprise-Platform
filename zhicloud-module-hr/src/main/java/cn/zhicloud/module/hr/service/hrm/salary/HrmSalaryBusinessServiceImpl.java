package cn.zhicloud.module.hr.service.hrm.salary;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.zhicloud.framework.common.pojo.PageParam;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.controller.admin.hrm.salary.vo.*;
import cn.zhicloud.module.hr.dal.dataobject.employee.HrEmployeeDO;
import cn.zhicloud.module.hr.dal.dataobject.salary2.*;
import cn.zhicloud.module.hr.dal.mysql.employee.HrEmployeeMapper;
import cn.zhicloud.module.hr.dal.mysql.salary2.*;
import cn.zhicloud.module.system.api.dept.DeptApi;
import cn.zhicloud.module.system.api.dept.dto.DeptRespDTO;
import cn.zhicloud.module.system.api.user.AdminUserApi;
import cn.zhicloud.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.hr.enums.ErrorCodeConstants.*;

/**
 * HRM 薪资业务域 Service 实现类（员工薪资档案/调薪记录/月度工资表/员工月度工资/工资条）
 *
 * 口径（对齐前端 views/hrm/salary）：
 * * 员工薪资档案：按员工唯一（employeeId）；定薪/调薪 upsert 档案并写调薪记录（recordType=1）。
 * * 员工信息（姓名/工号/手机号/部门/岗位/入职状态/员工状态）一律 enrich 自 hr_employee，不落薪资库。
 * * 月度工资表：表头聚合（应发/个税/实发）+ 员工月度工资明细；核算后 status=11 已核算，新建 status=5 未核算。
 * * 工资条：发放记录 + 每员工一条工资条；查看状态 readStatus 0未查看 1已查看。
 * * 本切片无薪资组关联，noSalaryGroupEmployeeCount 恒为 0（占位）。
 *
 * @author zhicloud
 */
@Service
@Validated
public class HrmSalaryBusinessServiceImpl implements HrmSalaryBusinessService {

    /** 个人所得税薪资项编码 */
    private static final int OPTION_CODE_PERSONAL_TAX = 230101;
    /** 调薪记录状态：1待生效 2已取消 */
    private static final int CHANGE_RECORD_STATUS_PENDING = 1;
    private static final int CHANGE_RECORD_STATUS_CANCELLED = 2;
    /** 月度工资表状态：5未核算 11已核算 */
    private static final int MONTH_STATUS_DRAFT = 5;
    private static final int MONTH_STATUS_COMPUTED = 11;
    /** 在职口径的入职状态：1在职 2待入职 3待离职 */
    private static final List<Integer> IN_SERVICE_ENTRY_STATUS = Arrays.asList(1, 2, 3);

    @Resource
    private HrmSalaryEmployeeInfoMapper hrmSalaryEmployeeInfoMapper;
    @Resource
    private HrmSalaryEmployeeChangeRecordMapper hrmSalaryEmployeeChangeRecordMapper;
    @Resource
    private HrmSalaryMonthRecordMapper hrmSalaryMonthRecordMapper;
    @Resource
    private HrmSalaryMonthEmployeeRecordMapper hrmSalaryMonthEmployeeRecordMapper;
    @Resource
    private HrmSalarySlipTemplateMapper hrmSalarySlipTemplateMapper;
    @Resource
    private HrmSalarySlipSendRecordMapper hrmSalarySlipSendRecordMapper;
    @Resource
    private HrmSalarySlipMapper hrmSalarySlipMapper;
    @Resource
    private HrEmployeeMapper hrmEmployeeMapper;
    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;

    // ========== 员工薪资档案 ==========

    @Override
    public PageResult<HrmSalaryEmployeeInfoVO> getEmployeeInfoPage(HrmSalaryEmployeeInfoPageReqVO reqVO) {
        PageResult<HrEmployeeDO> pageResult = hrmEmployeeMapper.selectPage(reqVO, buildEmployeeWrapper(reqVO));
        return new PageResult<>(buildEmployeeInfoVOs(pageResult.getList()), pageResult.getTotal());
    }

    @Override
    public HrmSalaryEmployeeInfoVO getEmployeeInfo(Long employeeId) {
        HrEmployeeDO employee = hrmEmployeeMapper.selectById(employeeId);
        if (employee == null) {
            throw exception(HRM_SALARY_EMPLOYEE_NOT_EXISTS);
        }
        List<HrmSalaryEmployeeInfoVO> list = buildEmployeeInfoVOs(Collections.singletonList(employee));
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<HrmSalaryEmployeeStatusCountRespVO> getEmployeeStatusCount(HrmSalaryEmployeeInfoPageReqVO reqVO) {
        List<HrEmployeeDO> employees = hrmEmployeeMapper.selectList(buildEmployeeWrapper(reqVO));
        Map<Integer, Long> countMap = employees.stream()
                .map(HrEmployeeDO::getEntryStatus).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(status -> status, Collectors.counting()));
        List<HrmSalaryEmployeeStatusCountRespVO> result = new ArrayList<>();
        for (int status = 1; status <= 4; status++) {
            HrmSalaryEmployeeStatusCountRespVO vo = new HrmSalaryEmployeeStatusCountRespVO();
            vo.setStatus(status);
            vo.setCount(countMap.getOrDefault(status, 0L));
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEmployeeInfo(HrmSalaryEmployeeInfoUpdateReqVO reqVO) {
        HrEmployeeDO employee = hrmEmployeeMapper.selectById(reqVO.getEmployeeId());
        if (employee == null) {
            throw exception(HRM_SALARY_EMPLOYEE_NOT_EXISTS);
        }
        HrmSalaryEmployeeInfoDO exist = hrmSalaryEmployeeInfoMapper.selectByEmployeeId(reqVO.getEmployeeId());
        BigDecimal beforeTotal = exist != null ? sumOptionMaps(exist.getSalaryOptions()) : BigDecimal.ZERO;
        BigDecimal beforeProbation = exist != null ? sumOptionMaps(exist.getProbationSalaryOptions()) : BigDecimal.ZERO;
        // 1. upsert 薪资档案
        HrmSalaryEmployeeInfoDO info = exist != null ? exist : new HrmSalaryEmployeeInfoDO();
        info.setEmployeeId(reqVO.getEmployeeId());
        info.setChangeReason(reqVO.getChangeReason());
        info.setEffectTime(reqVO.getEffectTime());
        info.setChangeType(reqVO.getChangeType());
        info.setProbationSalary(reqVO.getProbationSalary());
        info.setRegularSalary(reqVO.getRegularSalary());
        info.setRemark(reqVO.getRemark());
        info.setSalaryOptions(toOptionMaps(reqVO.getSalaryOptions()));
        info.setProbationSalaryOptions(toOptionMaps(reqVO.getProbationSalaryOptions()));
        if (exist != null) {
            hrmSalaryEmployeeInfoMapper.updateById(info);
        } else {
            hrmSalaryEmployeeInfoMapper.insert(info);
        }
        // 2. 写调薪记录（recordType=1 定薪）
        HrmSalaryEmployeeChangeRecordDO record = HrmSalaryEmployeeChangeRecordDO.builder()
                .employeeId(reqVO.getEmployeeId())
                .recordType(1)
                .changeReason(reqVO.getChangeReason())
                .effectTime(reqVO.getEffectTime())
                .beforeTotal(beforeTotal)
                .afterTotal(sumOptionValueVOs(reqVO.getSalaryOptions()))
                .probationBeforeTotal(beforeProbation)
                .probationAfterTotal(sumOptionValueVOs(reqVO.getProbationSalaryOptions()))
                .status(CHANGE_RECORD_STATUS_PENDING)
                .remark(reqVO.getRemark())
                .salaryOptions(toOptionMaps(reqVO.getSalaryOptions()))
                .probationSalaryOptions(toOptionMaps(reqVO.getProbationSalaryOptions()))
                .build();
        hrmSalaryEmployeeChangeRecordMapper.insert(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEmployeeInfoList(List<HrmSalaryEmployeeInfoUpdateReqVO> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        for (HrmSalaryEmployeeInfoUpdateReqVO reqVO : list) {
            updateEmployeeInfo(reqVO);
        }
    }

    @Override
    public LocalDateTime getAdjustmentMinEffectDate() {
        List<HrmSalaryEmployeeChangeRecordDO> list = hrmSalaryEmployeeChangeRecordMapper.selectList(
                new LambdaQueryWrapperX<HrmSalaryEmployeeChangeRecordDO>()
                        .gt(HrmSalaryEmployeeChangeRecordDO::getEffectTime, LocalDateTime.now()));
        return list.stream().map(HrmSalaryEmployeeChangeRecordDO::getEffectTime)
                .filter(Objects::nonNull).min(LocalDateTime::compareTo).orElse(null);
    }

    // ========== 调薪记录 ==========

    @Override
    public List<HrmSalaryEmployeeChangeRecordVO> getChangeRecordList(Long employeeId) {
        if (employeeId == null) {
            return Collections.emptyList();
        }
        return toChangeRecordVOs(hrmSalaryEmployeeChangeRecordMapper.selectListByEmployeeId(employeeId));
    }

    @Override
    public HrmSalaryEmployeeChangeRecordVO getChangeRecord(Long id) {
        HrmSalaryEmployeeChangeRecordDO record = validateChangeRecordExists(id);
        return toChangeRecordVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelChangeRecord(Long id) {
        HrmSalaryEmployeeChangeRecordDO record = validateChangeRecordExists(id);
        if (Integer.valueOf(CHANGE_RECORD_STATUS_CANCELLED).equals(record.getStatus())) {
            throw exception(HRM_SALARY_CHANGE_RECORD_CANCELLED);
        }
        HrmSalaryEmployeeChangeRecordDO updateObj = new HrmSalaryEmployeeChangeRecordDO();
        updateObj.setId(id);
        updateObj.setStatus(CHANGE_RECORD_STATUS_CANCELLED);
        hrmSalaryEmployeeChangeRecordMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChangeRecord(Long id) {
        validateChangeRecordExists(id);
        hrmSalaryEmployeeChangeRecordMapper.deleteById(id);
    }

    // ========== 月度工资表 ==========

    @Override
    public PageResult<HrmSalaryMonthRecordVO> getMonthRecordPage(HrmSalaryMonthRecordPageReqVO reqVO) {
        PageResult<HrmSalaryMonthRecordDO> pageResult = hrmSalaryMonthRecordMapper.selectPage(reqVO,
                new LambdaQueryWrapperX<HrmSalaryMonthRecordDO>()
                        .eqIfPresent(HrmSalaryMonthRecordDO::getYear, reqVO.getYear())
                        .eqIfPresent(HrmSalaryMonthRecordDO::getMonth, reqVO.getMonth())
                        .eqIfPresent(HrmSalaryMonthRecordDO::getStatus, reqVO.getStatus())
                        .orderByDesc(HrmSalaryMonthRecordDO::getYear)
                        .orderByDesc(HrmSalaryMonthRecordDO::getMonth));
        return new PageResult<>(enrichMonthRecords(pageResult.getList()), pageResult.getTotal());
    }

    @Override
    public HrmSalaryMonthRecordVO getMonthRecord(Long id) {
        HrmSalaryMonthRecordDO record = validateMonthRecordExists(id);
        return enrichMonthRecords(Collections.singletonList(record)).get(0);
    }

    @Override
    public HrmSalaryMonthRecordVO getLastMonthRecord() {
        HrmSalaryMonthRecordDO record = hrmSalaryMonthRecordMapper.selectLast();
        if (record == null) {
            return null;
        }
        return enrichMonthRecords(Collections.singletonList(record)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createNextMonthRecord() {
        HrmSalaryMonthRecordDO last = hrmSalaryMonthRecordMapper.selectLast();
        YearMonth base = (last != null && last.getYear() != null && last.getMonth() != null)
                ? YearMonth.of(last.getYear(), last.getMonth()) : YearMonth.now();
        YearMonth next = base.plusMonths(1);
        if (hrmSalaryMonthRecordMapper.selectByYearMonth(next.getYear(), next.getMonthValue()) != null) {
            throw exception(HRM_SALARY_MONTH_RECORD_EXISTS);
        }
        HrmSalaryMonthRecordDO record = HrmSalaryMonthRecordDO.builder()
                .title(next.getYear() + "年" + String.format("%02d", next.getMonthValue()) + "月工资表")
                .year(next.getYear())
                .month(next.getMonthValue())
                .employeeCount(0)
                .status(MONTH_STATUS_DRAFT)
                .startTime(next.atDay(1).atStartOfDay())
                .endTime(next.atEndOfMonth().atTime(23, 59, 59))
                .build();
        hrmSalaryMonthRecordMapper.insert(record);
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMonthRecord(Long id) {
        validateMonthRecordExists(id);
        hrmSalaryMonthEmployeeRecordMapper.delete(HrmSalaryMonthEmployeeRecordDO::getMonthRecordId, id);
        hrmSalaryMonthRecordMapper.deleteById(id);
    }

    @Override
    public HrmSalaryPayrollReadinessRespVO getPayrollReadiness(Long monthRecordId) {
        HrmSalaryMonthRecordDO record;
        if (monthRecordId != null) {
            record = validateMonthRecordExists(monthRecordId);
        } else {
            record = hrmSalaryMonthRecordMapper.selectLast();
        }
        HrmSalaryPayrollReadinessRespVO resp = new HrmSalaryPayrollReadinessRespVO();
        resp.setNoSalaryGroupEmployeeCount(0L);
        resp.setChangeTypeCountMap(Collections.emptyMap());
        resp.setNoSalaryEmployees(Collections.emptyList());
        if (record == null) {
            resp.setPayrollEmployeeCount(0L);
            resp.setSalaryEmployeeCount(0L);
            resp.setNoSalaryEmployeeCount(0L);
            resp.setChangeEmployeeCount(0L);
            return resp;
        }
        resp.setMonthRecordId(record.getId());
        resp.setTitle(record.getTitle());
        resp.setYear(record.getYear());
        resp.setMonth(record.getMonth());
        resp.setSocialSecurityYearMonth(record.getYear() != null && record.getMonth() != null
                ? record.getYear() + "-" + String.format("%02d", record.getMonth()) : null);
        if (record.getYear() == null || record.getMonth() == null) {
            resp.setPayrollEmployeeCount(0L);
            resp.setSalaryEmployeeCount(0L);
            resp.setNoSalaryEmployeeCount(0L);
            resp.setChangeEmployeeCount(0L);
            return resp;
        }
        // 计薪范围 = 在职员工（1在职 2待入职 3待离职）
        List<HrEmployeeDO> inService = hrmEmployeeMapper.selectHrmListByEntryStatusIn(IN_SERVICE_ENTRY_STATUS);
        Set<Long> employeeIds = inService.stream().map(HrEmployeeDO::getId).collect(Collectors.toSet());
        Map<Long, HrmSalaryEmployeeInfoDO> infoMap = employeeIds.isEmpty() ? Collections.emptyMap()
                : hrmSalaryEmployeeInfoMapper.selectListByEmployeeIds(employeeIds).stream()
                        .collect(Collectors.toMap(HrmSalaryEmployeeInfoDO::getEmployeeId, x -> x, (a, b) -> a));
        long payrollCount = inService.size();
        long salaryCount = infoMap.size();
        resp.setPayrollEmployeeCount(payrollCount);
        resp.setSalaryEmployeeCount(salaryCount);
        resp.setNoSalaryEmployeeCount(payrollCount - salaryCount);
        // 异动：本月窗口内的调薪记录
        LocalDateTime start = record.getStartTime() != null ? record.getStartTime()
                : YearMonth.of(record.getYear(), record.getMonth()).atDay(1).atStartOfDay();
        LocalDateTime end = record.getEndTime() != null ? record.getEndTime()
                : YearMonth.of(record.getYear(), record.getMonth()).atEndOfMonth().atTime(23, 59, 59);
        List<HrmSalaryEmployeeChangeRecordDO> changes = hrmSalaryEmployeeChangeRecordMapper.selectList(
                new LambdaQueryWrapperX<HrmSalaryEmployeeChangeRecordDO>()
                        .ge(HrmSalaryEmployeeChangeRecordDO::getEffectTime, start)
                        .le(HrmSalaryEmployeeChangeRecordDO::getEffectTime, end));
        resp.setChangeEmployeeCount(changes.stream().map(HrmSalaryEmployeeChangeRecordDO::getEmployeeId)
                .filter(Objects::nonNull).distinct().count());
        resp.setChangeTypeCountMap(changes.stream()
                .filter(c -> c.getChangeReason() != null)
                .collect(Collectors.groupingBy(HrmSalaryEmployeeChangeRecordDO::getChangeReason, Collectors.counting())));
        // 未定薪员工
        Map<Long, DeptRespDTO> deptMap = loadDeptMap(inService);
        List<HrmSalaryMonthEmployeeRecordVO> noSalaryList = inService.stream()
                .filter(emp -> !infoMap.containsKey(emp.getId()))
                .map(emp -> {
                    HrmSalaryMonthEmployeeRecordVO vo = new HrmSalaryMonthEmployeeRecordVO();
                    vo.setEmployeeId(emp.getId());
                    fillMonthEmployeeFields(vo, emp, deptMap);
                    return vo;
                }).collect(Collectors.toList());
        resp.setNoSalaryEmployees(noSalaryList);
        return resp;
    }

    @Override
    public List<SalaryOptionValueVO> getMonthOptionSummary(Long monthRecordId) {
        if (monthRecordId == null) {
            return Collections.emptyList();
        }
        List<HrmSalaryMonthEmployeeRecordDO> records = hrmSalaryMonthEmployeeRecordMapper
                .selectListByMonthRecordId(monthRecordId);
        Map<Integer, SalaryOptionValueVO> agg = new LinkedHashMap<>();
        for (HrmSalaryMonthEmployeeRecordDO record : records) {
            if (record.getOptionValues() == null) {
                continue;
            }
            for (Map<String, Object> option : record.getOptionValues()) {
                Integer code = toInteger(option.get("code"));
                if (code == null) {
                    continue;
                }
                BigDecimal value = toBigDecimal(option.get("value"));
                SalaryOptionValueVO vo = agg.get(code);
                if (vo == null) {
                    vo = new SalaryOptionValueVO();
                    vo.setCode(code);
                    vo.setName(toStr(option.get("name")));
                    vo.setValue(BigDecimal.ZERO);
                    agg.put(code, vo);
                }
                if (value != null) {
                    vo.setValue(vo.getValue().add(value));
                }
            }
        }
        return new ArrayList<>(agg.values());
    }

    // ========== 员工月度工资 ==========

    @Override
    public PageResult<HrmSalaryMonthEmployeeRecordVO> getMonthEmployeeRecordPage(HrmSalaryMonthEmployeeRecordPageReqVO reqVO) {
        return pageMonthEmployeeRecords(reqVO);
    }

    @Override
    public PageResult<HrmSalaryMonthEmployeeRecordVO> getMonthEmployeeRecordEmployeePage(HrmSalaryMonthEmployeeRecordPageReqVO reqVO) {
        return pageMonthEmployeeRecords(reqVO);
    }

    @Override
    public List<HrmSalaryMonthEmployeeRecordVO> getMonthEmployeeRecordList(Long monthRecordId) {
        if (monthRecordId == null) {
            return Collections.emptyList();
        }
        return buildMonthEmployeeVOs(monthRecordId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMonthEmployeeRecordList(HrmSalaryMonthEmployeeUpdateListReqVO reqVO) {
        HrmSalaryMonthRecordDO record = validateMonthRecordExists(reqVO.getMonthRecordId());
        List<Map<String, Object>> maps = toOptionMaps(reqVO.getOptionValues());
        BigDecimal expected = sumOptionValueVOs(reqVO.getOptionValues());
        BigDecimal personalTax = extractOptionValue(reqVO.getOptionValues(), OPTION_CODE_PERSONAL_TAX);
        BigDecimal realPay = personalTax != null ? expected.subtract(personalTax) : expected;
        for (Long employeeId : reqVO.getEmployeeIds()) {
            HrmSalaryMonthEmployeeRecordDO exist = hrmSalaryMonthEmployeeRecordMapper
                    .selectByMonthRecordAndEmployee(reqVO.getMonthRecordId(), employeeId);
            HrmSalaryMonthEmployeeRecordDO rec = exist != null ? exist : new HrmSalaryMonthEmployeeRecordDO();
            rec.setMonthRecordId(reqVO.getMonthRecordId());
            rec.setEmployeeId(employeeId);
            rec.setYear(record.getYear());
            rec.setMonth(record.getMonth());
            rec.setOptionValues(maps);
            rec.setExpectedPaySalary(expected);
            rec.setPersonalTax(personalTax);
            rec.setRealPaySalary(realPay);
            if (exist != null) {
                hrmSalaryMonthEmployeeRecordMapper.updateById(rec);
            } else {
                hrmSalaryMonthEmployeeRecordMapper.insert(rec);
            }
        }
    }

    @Override
    public List<HrmSalaryChangeCountRespVO> getMonthEmployeeChangeCount(Long monthRecordId) {
        HrmSalaryMonthRecordDO record = hrmSalaryMonthRecordMapper.selectById(monthRecordId);
        if (record == null || record.getYear() == null || record.getMonth() == null) {
            return Collections.emptyList();
        }
        YearMonth ym = YearMonth.of(record.getYear(), record.getMonth());
        List<HrmSalaryEmployeeChangeRecordDO> changes = hrmSalaryEmployeeChangeRecordMapper.selectList(
                new LambdaQueryWrapperX<HrmSalaryEmployeeChangeRecordDO>()
                        .ge(HrmSalaryEmployeeChangeRecordDO::getEffectTime, ym.atDay(1).atStartOfDay())
                        .le(HrmSalaryEmployeeChangeRecordDO::getEffectTime, ym.atEndOfMonth().atTime(23, 59, 59)));
        Map<Integer, Long> countMap = changes.stream()
                .filter(c -> c.getChangeReason() != null)
                .collect(Collectors.groupingBy(HrmSalaryEmployeeChangeRecordDO::getChangeReason, Collectors.counting()));
        return countMap.entrySet().stream().map(e -> {
            HrmSalaryChangeCountRespVO vo = new HrmSalaryChangeCountRespVO();
            vo.setChangeType(e.getKey());
            vo.setCount(e.getValue());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<HrmSalaryPerformanceCoefficientVO> getPerformanceCoefficients(Long monthRecordId) {
        if (monthRecordId == null) {
            return Collections.emptyList();
        }
        List<HrmSalaryMonthEmployeeRecordVO> records = buildMonthEmployeeVOs(monthRecordId);
        return records.stream().map(record -> {
            HrmSalaryPerformanceCoefficientVO vo = new HrmSalaryPerformanceCoefficientVO();
            vo.setEmployeeId(record.getEmployeeId());
            vo.setEmployeeName(record.getEmployeeName());
            vo.setPerformanceCoefficient(record.getPerformanceCoefficient());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long computeMonthRecord(Long monthRecordId) {
        HrmSalaryMonthRecordDO record = validateMonthRecordExists(monthRecordId);
        List<HrmSalaryMonthEmployeeRecordDO> records = hrmSalaryMonthEmployeeRecordMapper
                .selectListByMonthRecordId(monthRecordId);
        record.setEmployeeCount(records.size());
        record.setExpectedPaySalary(sum(records.stream().map(HrmSalaryMonthEmployeeRecordDO::getExpectedPaySalary)));
        record.setPersonalTax(sum(records.stream().map(HrmSalaryMonthEmployeeRecordDO::getPersonalTax)));
        record.setRealPaySalary(sum(records.stream().map(HrmSalaryMonthEmployeeRecordDO::getRealPaySalary)));
        record.setStatus(MONTH_STATUS_COMPUTED);
        hrmSalaryMonthRecordMapper.updateById(record);
        return monthRecordId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void computeImport(Long monthRecordId) {
        computeMonthRecord(monthRecordId);
    }

    // ========== 工资条模板 ==========

    @Override
    public List<HrmSalarySlipTemplateVO> getSlipTemplateList() {
        return hrmSalarySlipTemplateMapper.selectListAll().stream()
                .map(this::toSlipTemplateVO).collect(Collectors.toList());
    }

    @Override
    public HrmSalarySlipTemplateVO getSlipTemplate(Long id) {
        return toSlipTemplateVO(validateSlipTemplateExists(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSlipTemplate(HrmSalarySlipTemplateVO reqVO) {
        HrmSalarySlipTemplateDO template = BeanUtils.toBean(reqVO, HrmSalarySlipTemplateDO.class);
        template.setOptions(toTemplateOptionMaps(reqVO.getOptions()));
        if (template.getHideEmpty() == null) {
            template.setHideEmpty(false);
        }
        if (template.getDefaultStatus() == null) {
            template.setDefaultStatus(false);
        }
        hrmSalarySlipTemplateMapper.insert(template);
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSlipTemplate(HrmSalarySlipTemplateVO reqVO) {
        validateSlipTemplateExists(reqVO.getId());
        HrmSalarySlipTemplateDO template = BeanUtils.toBean(reqVO, HrmSalarySlipTemplateDO.class);
        template.setOptions(toTemplateOptionMaps(reqVO.getOptions()));
        hrmSalarySlipTemplateMapper.updateById(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSlipTemplate(Long id) {
        validateSlipTemplateExists(id);
        hrmSalarySlipTemplateMapper.deleteById(id);
    }

    // ========== 工资条发放记录 ==========

    @Override
    public PageResult<HrmSalarySlipSendRecordVO> getSlipSendRecordPage(Integer pageNo, Integer pageSize, Long monthRecordId) {
        PageResult<HrmSalarySlipSendRecordDO> pageResult = hrmSalarySlipSendRecordMapper.selectPage(
                buildPage(pageNo, pageSize),
                new LambdaQueryWrapperX<HrmSalarySlipSendRecordDO>()
                        .eqIfPresent(HrmSalarySlipSendRecordDO::getMonthRecordId, monthRecordId)
                        .orderByDesc(HrmSalarySlipSendRecordDO::getId));
        List<HrmSalarySlipSendRecordVO> voList = BeanUtils.toBean(pageResult.getList(), HrmSalarySlipSendRecordVO.class);
        enrichSendRecordCreators(pageResult.getList(), voList);
        return new PageResult<>(voList, pageResult.getTotal());
    }

    @Override
    public HrmSalarySlipSendRecordVO getSlipSendRecord(Long id) {
        HrmSalarySlipSendRecordDO record = validateSlipSendRecordExists(id);
        HrmSalarySlipSendRecordVO vo = BeanUtils.toBean(record, HrmSalarySlipSendRecordVO.class);
        enrichSendRecordCreators(Collections.singletonList(record), Collections.singletonList(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSlipSendRecord(HrmSalarySlipSendRecordCreateReqVO reqVO) {
        HrmSalaryMonthRecordDO monthRecord = hrmSalaryMonthRecordMapper.selectById(reqVO.getMonthRecordId());
        if (monthRecord == null) {
            throw exception(HRM_SALARY_MONTH_RECORD_NOT_EXISTS);
        }
        List<HrmSalaryMonthEmployeeRecordDO> allRecords = hrmSalaryMonthEmployeeRecordMapper
                .selectListByMonthRecordId(reqVO.getMonthRecordId());
        List<HrmSalaryMonthEmployeeRecordDO> targets;
        if (Boolean.TRUE.equals(reqVO.getAll())) {
            targets = allRecords;
        } else {
            Set<Long> employeeIds = reqVO.getEmployeeIds() == null ? Collections.emptySet()
                    : new HashSet<>(reqVO.getEmployeeIds());
            targets = allRecords.stream().filter(r -> employeeIds.contains(r.getEmployeeId()))
                    .collect(Collectors.toList());
        }
        HrmSalarySlipSendRecordDO sendRecord = HrmSalarySlipSendRecordDO.builder()
                .monthRecordId(reqVO.getMonthRecordId())
                .employeeCount(targets.size())
                .sendEmployeeCount(targets.size())
                .readCount(0)
                .year(monthRecord.getYear())
                .month(monthRecord.getMonth())
                .options(toTemplateOptionMaps(reqVO.getOptions()))
                .build();
        hrmSalarySlipSendRecordMapper.insert(sendRecord);
        for (HrmSalaryMonthEmployeeRecordDO target : targets) {
            HrmSalarySlipDO slip = HrmSalarySlipDO.builder()
                    .sendRecordId(sendRecord.getId())
                    .monthEmployeeRecordId(target.getId())
                    .employeeId(target.getEmployeeId())
                    .year(monthRecord.getYear())
                    .month(monthRecord.getMonth())
                    .readStatus(0)
                    .build();
            hrmSalarySlipMapper.insert(slip);
        }
        return sendRecord.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSlipSendRecord(Long id) {
        validateSlipSendRecordExists(id);
        hrmSalarySlipMapper.delete(HrmSalarySlipDO::getSendRecordId, id);
        hrmSalarySlipSendRecordMapper.deleteById(id);
    }

    // ========== 工资条 ==========

    @Override
    public PageResult<HrmSalarySlipVO> getSlipPage(Integer pageNo, Integer pageSize, Long employeeId, Long sendRecordId) {
        PageResult<HrmSalarySlipDO> pageResult = hrmSalarySlipMapper.selectPage(
                buildPage(pageNo, pageSize),
                new LambdaQueryWrapperX<HrmSalarySlipDO>()
                        .eqIfPresent(HrmSalarySlipDO::getEmployeeId, employeeId)
                        .eqIfPresent(HrmSalarySlipDO::getSendRecordId, sendRecordId)
                        .orderByDesc(HrmSalarySlipDO::getId));
        return new PageResult<>(buildSlipVOs(pageResult.getList()), pageResult.getTotal());
    }

    @Override
    public HrmSalarySlipVO getSlip(Long id) {
        HrmSalarySlipDO slip = validateSlipExists(id);
        return buildSlipVOs(Collections.singletonList(slip)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remarkSlip(Long id) {
        validateSlipExists(id);
        HrmSalarySlipDO updateObj = new HrmSalarySlipDO();
        updateObj.setId(id);
        updateObj.setReadStatus(1);
        hrmSalarySlipMapper.updateById(updateObj);
    }

    // ========== 私有：分页 ==========

    private PageResult<HrmSalaryMonthEmployeeRecordVO> pageMonthEmployeeRecords(HrmSalaryMonthEmployeeRecordPageReqVO reqVO) {
        if (reqVO.getMonthRecordId() == null) {
            return PageResult.empty();
        }
        List<HrmSalaryMonthEmployeeRecordVO> all = buildMonthEmployeeVOs(reqVO.getMonthRecordId());
        // 员工维度过滤
        Map<Long, HrEmployeeDO> employeeMap = loadEmployeeMap(all.stream()
                .map(HrmSalaryMonthEmployeeRecordVO::getEmployeeId).collect(Collectors.toSet()));
        Set<Long> deptFilter = reqVO.getDeptId() == null ? Collections.emptySet() : new HashSet<>(expandDeptIds(reqVO.getDeptId()));
        Set<Long> changeTypeEmployeeIds = reqVO.getEmployeeChangeType() == null ? Collections.emptySet()
                : loadMonthChangeEmployeeIds(reqVO.getMonthRecordId(), reqVO.getEmployeeChangeType());
        Set<Long> sentEmployeeIds = Boolean.TRUE.equals(reqVO.getSalarySlipSent())
                ? loadMonthSlipSentEmployeeIds(reqVO.getMonthRecordId()) : Collections.emptySet();
        List<HrmSalaryMonthEmployeeRecordVO> filtered = all.stream()
                .filter(vo -> !StrUtil.isNotBlank(reqVO.getEmployeeName())
                        || StrUtil.containsIgnoreCase(vo.getEmployeeName(), reqVO.getEmployeeName()))
                .filter(vo -> !StrUtil.isNotBlank(reqVO.getJobNumber())
                        || StrUtil.containsIgnoreCase(vo.getJobNumber(), reqVO.getJobNumber()))
                .filter(vo -> deptFilter.isEmpty() || deptFilter.contains(vo.getDeptId()))
                .filter(vo -> !StrUtil.isNotBlank(reqVO.getSearch()) || matchSearch(vo, employeeMap.get(vo.getEmployeeId()), reqVO.getSearch()))
                .filter(vo -> changeTypeEmployeeIds.isEmpty() || changeTypeEmployeeIds.contains(vo.getEmployeeId()))
                .filter(vo -> !Boolean.TRUE.equals(reqVO.getSalarySlipSent()) || sentEmployeeIds.contains(vo.getEmployeeId()))
                .collect(Collectors.toList());
        return paginate(filtered, reqVO);
    }

    private <T> PageResult<T> paginate(List<T> list, PageParam reqVO) {
        int pageNo = reqVO.getPageNo() == null ? 1 : reqVO.getPageNo();
        int pageSize = reqVO.getPageSize() == null ? 10 : reqVO.getPageSize();
        if (PageParam.PAGE_SIZE_NONE.equals(pageSize)) {
            return new PageResult<>(list, (long) list.size());
        }
        int from = Math.min((pageNo - 1) * pageSize, list.size());
        int to = Math.min(from + pageSize, list.size());
        return new PageResult<>(new ArrayList<>(list.subList(from, to)), (long) list.size());
    }

    private PageParam buildPage(Integer pageNo, Integer pageSize) {
        PageParam pageParam = new PageParam();
        pageParam.setPageNo(pageNo == null ? 1 : pageNo);
        pageParam.setPageSize(pageSize == null ? 10 : pageSize);
        return pageParam;
    }

    // ========== 私有：构建员工过滤 ==========

    private LambdaQueryWrapperX<HrEmployeeDO> buildEmployeeWrapper(HrmSalaryEmployeeInfoPageReqVO reqVO) {
        LambdaQueryWrapperX<HrEmployeeDO> wrapper = new LambdaQueryWrapperX<HrEmployeeDO>()
                .likeIfPresent(HrEmployeeDO::getName, reqVO.getEmployeeName())
                .likeIfPresent(HrEmployeeDO::getEmpNo, reqVO.getJobNumber())
                .inIfPresent(HrEmployeeDO::getDeptId, expandDeptIds(reqVO.getDeptId()))
                .eqIfPresent(HrEmployeeDO::getEntryStatus, reqVO.getEntryStatus())
                .eqIfPresent(HrEmployeeDO::getEmployeeStatus, reqVO.getStatus());
        if (StrUtil.isNotBlank(reqVO.getSearch())) {
            String search = reqVO.getSearch();
            wrapper.and(w -> w.like(HrEmployeeDO::getName, search)
                    .or().like(HrEmployeeDO::getEmpNo, search)
                    .or().like(HrEmployeeDO::getPhone, search));
        }
        wrapper.orderByDesc(HrEmployeeDO::getId);
        return wrapper;
    }

    /**
     * 部门子树展开（含自身）
     */
    private Collection<Long> expandDeptIds(Long deptId) {
        if (deptId == null) {
            return Collections.emptyList();
        }
        Set<Long> deptIds = new HashSet<>();
        deptIds.add(deptId);
        deptApi.getChildDeptList(deptId).forEach(dept -> deptIds.add(dept.getId()));
        return deptIds;
    }

    // ========== 私有：enrich ==========

    private List<HrmSalaryEmployeeInfoVO> buildEmployeeInfoVOs(List<HrEmployeeDO> employees) {
        if (CollUtil.isEmpty(employees)) {
            return Collections.emptyList();
        }
        Set<Long> ids = employees.stream().map(HrEmployeeDO::getId).collect(Collectors.toSet());
        Map<Long, HrmSalaryEmployeeInfoDO> infoMap = hrmSalaryEmployeeInfoMapper.selectListByEmployeeIds(ids).stream()
                .collect(Collectors.toMap(HrmSalaryEmployeeInfoDO::getEmployeeId, x -> x, (a, b) -> a));
        Map<Long, DeptRespDTO> deptMap = loadDeptMap(employees);
        List<HrmSalaryEmployeeInfoVO> result = new ArrayList<>();
        for (HrEmployeeDO employee : employees) {
            HrmSalaryEmployeeInfoVO vo = new HrmSalaryEmployeeInfoVO();
            HrmSalaryEmployeeInfoDO info = infoMap.get(employee.getId());
            if (info != null) {
                vo.setId(info.getId());
                vo.setChangeReason(info.getChangeReason());
                vo.setEffectTime(info.getEffectTime());
                vo.setChangeType(info.getChangeType());
                vo.setProbationSalary(info.getProbationSalary());
                vo.setRegularSalary(info.getRegularSalary());
                vo.setRemark(info.getRemark());
                vo.setSalaryOptions(toOptionValueVOs(info.getSalaryOptions()));
                vo.setProbationSalaryOptions(toOptionValueVOs(info.getProbationSalaryOptions()));
                vo.setCreateTime(info.getCreateTime());
            }
            vo.setEmployeeId(employee.getId());
            fillEmployeeInfoFields(vo, employee, deptMap);
            result.add(vo);
        }
        return result;
    }

    private void fillEmployeeInfoFields(HrmSalaryEmployeeInfoVO vo, HrEmployeeDO employee, Map<Long, DeptRespDTO> deptMap) {
        vo.setEmployeeName(employee.getName());
        vo.setJobNumber(employee.getEmpNo());
        vo.setMobile(employee.getPhone());
        vo.setDeptId(employee.getDeptId());
        vo.setPostName(employee.getPostName());
        vo.setEntryStatus(employee.getEntryStatus());
        vo.setStatus(employee.getEmployeeStatus());
        vo.setEntryTime(employee.getEntryTime());
        vo.setRegularTime(employee.getRegularTime());
        DeptRespDTO dept = deptMap.get(employee.getDeptId());
        if (dept != null) {
            vo.setDeptName(dept.getName());
        }
    }

    private void fillMonthEmployeeFields(HrmSalaryMonthEmployeeRecordVO vo, HrEmployeeDO employee, Map<Long, DeptRespDTO> deptMap) {
        vo.setEmployeeName(employee.getName());
        vo.setJobNumber(employee.getEmpNo());
        vo.setDeptId(employee.getDeptId());
        vo.setPostName(employee.getPostName());
        DeptRespDTO dept = deptMap.get(employee.getDeptId());
        if (dept != null) {
            vo.setDeptName(dept.getName());
        }
    }

    private Map<Long, HrEmployeeDO> loadEmployeeMap(Collection<Long> employeeIds) {
        Set<Long> ids = employeeIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return hrmEmployeeMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(HrEmployeeDO::getId, x -> x, (a, b) -> a));
    }

    private Map<Long, DeptRespDTO> loadDeptMap(List<HrEmployeeDO> employees) {
        Set<Long> deptIds = employees.stream().map(HrEmployeeDO::getDeptId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        return deptIds.isEmpty() ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
    }

    private List<HrmSalaryMonthEmployeeRecordVO> buildMonthEmployeeVOs(Long monthRecordId) {
        List<HrmSalaryMonthEmployeeRecordDO> records = hrmSalaryMonthEmployeeRecordMapper
                .selectListByMonthRecordId(monthRecordId);
        if (CollUtil.isEmpty(records)) {
            return Collections.emptyList();
        }
        Set<Long> employeeIds = records.stream().map(HrmSalaryMonthEmployeeRecordDO::getEmployeeId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, HrEmployeeDO> employeeMap = loadEmployeeMap(employeeIds);
        Map<Long, DeptRespDTO> deptMap = loadDeptMap(new ArrayList<>(employeeMap.values()));
        List<HrmSalaryMonthEmployeeRecordVO> result = new ArrayList<>();
        for (HrmSalaryMonthEmployeeRecordDO record : records) {
            HrmSalaryMonthEmployeeRecordVO vo = BeanUtils.toBean(record, HrmSalaryMonthEmployeeRecordVO.class);
            vo.setOptionValues(toOptionValueVOs(record.getOptionValues()));
            HrEmployeeDO employee = employeeMap.get(record.getEmployeeId());
            if (employee != null) {
                fillMonthEmployeeFields(vo, employee, deptMap);
            }
            result.add(vo);
        }
        return result;
    }

    private List<HrmSalaryMonthRecordVO> enrichMonthRecords(List<HrmSalaryMonthRecordDO> records) {
        if (CollUtil.isEmpty(records)) {
            return Collections.emptyList();
        }
        List<HrmSalaryMonthRecordVO> voList = BeanUtils.toBean(records, HrmSalaryMonthRecordVO.class);
        for (int i = 0; i < records.size(); i++) {
            HrmSalaryMonthRecordDO record = records.get(i);
            HrmSalaryMonthRecordVO vo = voList.get(i);
            Long count = hrmSalaryMonthEmployeeRecordMapper.selectCountByMonthRecordId(record.getId());
            vo.setEmployeeCount(count == null ? 0 : count.intValue());
            List<HrmSalaryMonthEmployeeRecordDO> employeeRecords = hrmSalaryMonthEmployeeRecordMapper
                    .selectListByMonthRecordId(record.getId());
            Map<Integer, SalaryOptionValueVO> headers = new LinkedHashMap<>();
            for (HrmSalaryMonthEmployeeRecordDO employeeRecord : employeeRecords) {
                if (employeeRecord.getOptionValues() == null) {
                    continue;
                }
                for (Map<String, Object> option : employeeRecord.getOptionValues()) {
                    Integer code = toInteger(option.get("code"));
                    if (code != null && !headers.containsKey(code)) {
                        SalaryOptionValueVO header = new SalaryOptionValueVO();
                        header.setCode(code);
                        header.setName(toStr(option.get("name")));
                        headers.put(code, header);
                    }
                }
            }
            vo.setOptionHeaders(new ArrayList<>(headers.values()));
        }
        return voList;
    }

    private List<HrmSalarySlipVO> buildSlipVOs(List<HrmSalarySlipDO> slips) {
        if (CollUtil.isEmpty(slips)) {
            return Collections.emptyList();
        }
        Set<Long> employeeIds = slips.stream().map(HrmSalarySlipDO::getEmployeeId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, HrEmployeeDO> employeeMap = loadEmployeeMap(employeeIds);
        Map<Long, DeptRespDTO> deptMap = loadDeptMap(new ArrayList<>(employeeMap.values()));
        List<HrmSalarySlipVO> result = new ArrayList<>();
        for (HrmSalarySlipDO slip : slips) {
            HrmSalarySlipVO vo = BeanUtils.toBean(slip, HrmSalarySlipVO.class);
            HrEmployeeDO employee = employeeMap.get(slip.getEmployeeId());
            if (employee != null) {
                vo.setEmployeeName(employee.getName());
                vo.setJobNumber(employee.getEmpNo());
                vo.setMobile(employee.getPhone());
                vo.setDeptId(employee.getDeptId());
                vo.setPostName(employee.getPostName());
                DeptRespDTO dept = deptMap.get(employee.getDeptId());
                if (dept != null) {
                    vo.setDeptName(dept.getName());
                }
            }
            // 工资条项来自员工月度工资的工资项值
            if (slip.getMonthEmployeeRecordId() != null) {
                HrmSalaryMonthEmployeeRecordDO employeeRecord = hrmSalaryMonthEmployeeRecordMapper
                        .selectById(slip.getMonthEmployeeRecordId());
                if (employeeRecord != null) {
                    vo.setOptions(toSlipOptions(employeeRecord.getOptionValues()));
                }
            }
            result.add(vo);
        }
        return result;
    }

    private void enrichSendRecordCreators(List<HrmSalarySlipSendRecordDO> records, List<HrmSalarySlipSendRecordVO> voList) {
        if (CollUtil.isEmpty(records)) {
            return;
        }
        Set<Long> userIds = records.stream().map(HrmSalarySlipSendRecordDO::getCreator)
                .filter(StrUtil::isNotBlank).map(id -> {
                    try {
                        return Long.valueOf(id);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> userMap = userIds.isEmpty() ? Collections.emptyMap() : adminUserApi.getUserMap(userIds);
        for (int i = 0; i < records.size(); i++) {
            String creator = records.get(i).getCreator();
            if (StrUtil.isBlank(creator)) {
                continue;
            }
            try {
                AdminUserRespDTO user = userMap.get(Long.valueOf(creator));
                if (user != null) {
                    voList.get(i).setCreatorName(user.getNickname());
                }
            } catch (NumberFormatException ignored) {
                // creator 非数值编号，忽略
            }
        }
    }

    private Set<Long> loadMonthChangeEmployeeIds(Long monthRecordId, Integer changeReason) {
        HrmSalaryMonthRecordDO record = hrmSalaryMonthRecordMapper.selectById(monthRecordId);
        if (record == null || record.getYear() == null || record.getMonth() == null) {
            return Collections.emptySet();
        }
        YearMonth ym = YearMonth.of(record.getYear(), record.getMonth());
        List<HrmSalaryEmployeeChangeRecordDO> changes = hrmSalaryEmployeeChangeRecordMapper.selectList(
                new LambdaQueryWrapperX<HrmSalaryEmployeeChangeRecordDO>()
                        .eq(HrmSalaryEmployeeChangeRecordDO::getChangeReason, changeReason)
                        .ge(HrmSalaryEmployeeChangeRecordDO::getEffectTime, ym.atDay(1).atStartOfDay())
                        .le(HrmSalaryEmployeeChangeRecordDO::getEffectTime, ym.atEndOfMonth().atTime(23, 59, 59)));
        return changes.stream().map(HrmSalaryEmployeeChangeRecordDO::getEmployeeId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private Set<Long> loadMonthSlipSentEmployeeIds(Long monthRecordId) {
        List<HrmSalarySlipSendRecordDO> sendRecords = hrmSalarySlipSendRecordMapper.selectListByMonthRecordId(monthRecordId);
        Set<Long> employeeIds = new HashSet<>();
        for (HrmSalarySlipSendRecordDO sendRecord : sendRecords) {
            hrmSalarySlipMapper.selectListBySendRecordId(sendRecord.getId())
                    .forEach(slip -> employeeIds.add(slip.getEmployeeId()));
        }
        return employeeIds;
    }

    private boolean matchSearch(HrmSalaryMonthEmployeeRecordVO vo, HrEmployeeDO employee, String search) {
        if (StrUtil.containsIgnoreCase(vo.getEmployeeName(), search)
                || StrUtil.containsIgnoreCase(vo.getJobNumber(), search)) {
            return true;
        }
        return employee != null && StrUtil.containsIgnoreCase(employee.getPhone(), search);
    }

    // ========== 私有：转换 ==========

    private List<HrmSalaryEmployeeChangeRecordVO> toChangeRecordVOs(List<HrmSalaryEmployeeChangeRecordDO> records) {
        if (CollUtil.isEmpty(records)) {
            return Collections.emptyList();
        }
        return records.stream().map(this::toChangeRecordVO).collect(Collectors.toList());
    }

    private HrmSalaryEmployeeChangeRecordVO toChangeRecordVO(HrmSalaryEmployeeChangeRecordDO record) {
        HrmSalaryEmployeeChangeRecordVO vo = BeanUtils.toBean(record, HrmSalaryEmployeeChangeRecordVO.class);
        vo.setSalaryOptions(toOptionValueVOs(record.getSalaryOptions()));
        vo.setProbationSalaryOptions(toOptionValueVOs(record.getProbationSalaryOptions()));
        return vo;
    }

    private HrmSalarySlipTemplateVO toSlipTemplateVO(HrmSalarySlipTemplateDO template) {
        HrmSalarySlipTemplateVO vo = BeanUtils.toBean(template, HrmSalarySlipTemplateVO.class);
        vo.setOptions(toTemplateOptionVOs(template.getOptions()));
        return vo;
    }

    private List<SalaryOptionValueVO> toOptionValueVOs(List<Map<String, Object>> maps) {
        if (maps == null) {
            return null;
        }
        List<SalaryOptionValueVO> list = new ArrayList<>();
        for (Map<String, Object> map : maps) {
            SalaryOptionValueVO vo = new SalaryOptionValueVO();
            vo.setCode(toInteger(map.get("code")));
            vo.setName(toStr(map.get("name")));
            vo.setValue(toBigDecimal(map.get("value")));
            list.add(vo);
        }
        return list;
    }

    private List<Map<String, Object>> toOptionMaps(List<SalaryOptionValueVO> list) {
        if (list == null) {
            return null;
        }
        List<Map<String, Object>> maps = new ArrayList<>();
        for (SalaryOptionValueVO vo : list) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("code", vo.getCode());
            map.put("name", vo.getName());
            map.put("value", vo.getValue());
            maps.add(map);
        }
        return maps;
    }

    private List<HrmSalarySlipTemplateOptionVO> toTemplateOptionVOs(List<Map<String, Object>> maps) {
        if (maps == null) {
            return null;
        }
        List<HrmSalarySlipTemplateOptionVO> list = new ArrayList<>();
        for (Map<String, Object> map : maps) {
            HrmSalarySlipTemplateOptionVO vo = new HrmSalarySlipTemplateOptionVO();
            vo.setName(toStr(map.get("name")));
            vo.setType(toInteger(map.get("type")));
            vo.setCode(toInteger(map.get("code")));
            vo.setRemark(toStr(map.get("remark")));
            vo.setParentCode(toInteger(map.get("parentCode")));
            vo.setHidden(toBoolean(map.get("hidden")));
            vo.setSort(toInteger(map.get("sort")));
            list.add(vo);
        }
        return list;
    }

    private List<Map<String, Object>> toTemplateOptionMaps(List<HrmSalarySlipTemplateOptionVO> list) {
        if (list == null) {
            return null;
        }
        List<Map<String, Object>> maps = new ArrayList<>();
        for (HrmSalarySlipTemplateOptionVO vo : list) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", vo.getName());
            map.put("type", vo.getType());
            map.put("code", vo.getCode());
            map.put("remark", vo.getRemark());
            map.put("parentCode", vo.getParentCode());
            map.put("hidden", vo.getHidden());
            map.put("sort", vo.getSort());
            maps.add(map);
        }
        return maps;
    }

    private List<HrmSalarySlipOptionVO> toSlipOptions(List<Map<String, Object>> maps) {
        if (maps == null) {
            return null;
        }
        List<HrmSalarySlipOptionVO> list = new ArrayList<>();
        for (Map<String, Object> map : maps) {
            HrmSalarySlipOptionVO vo = new HrmSalarySlipOptionVO();
            vo.setName(toStr(map.get("name")));
            vo.setType(toInteger(map.get("type")));
            vo.setCode(toInteger(map.get("code")));
            vo.setValue(toBigDecimal(map.get("value")));
            vo.setRemark(toStr(map.get("remark")));
            vo.setSort(toInteger(map.get("sort")));
            list.add(vo);
        }
        return list;
    }

    // ========== 私有：计算 ==========

    private BigDecimal sumOptionValueVOs(List<SalaryOptionValueVO> list) {
        if (CollUtil.isEmpty(list)) {
            return BigDecimal.ZERO;
        }
        return list.stream().map(SalaryOptionValueVO::getValue).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumOptionMaps(List<Map<String, Object>> maps) {
        if (CollUtil.isEmpty(maps)) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> map : maps) {
            BigDecimal value = toBigDecimal(map.get("value"));
            if (value != null) {
                total = total.add(value);
            }
        }
        return total;
    }

    private BigDecimal sum(java.util.stream.Stream<BigDecimal> values) {
        return values.filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal extractOptionValue(List<SalaryOptionValueVO> list, int code) {
        if (CollUtil.isEmpty(list)) {
            return null;
        }
        return list.stream().filter(vo -> vo.getCode() != null && vo.getCode() == code)
                .map(SalaryOptionValueVO::getValue).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return new BigDecimal(value.toString()).intValue();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String toStr(Object value) {
        return value == null ? null : value.toString();
    }

    private static Boolean toBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.valueOf(value.toString());
    }

    // ========== 私有：校验 ==========

    private HrmSalaryEmployeeChangeRecordDO validateChangeRecordExists(Long id) {
        HrmSalaryEmployeeChangeRecordDO record = hrmSalaryEmployeeChangeRecordMapper.selectById(id);
        if (record == null) {
            throw exception(HRM_SALARY_CHANGE_RECORD_NOT_EXISTS);
        }
        return record;
    }

    private HrmSalaryMonthRecordDO validateMonthRecordExists(Long id) {
        HrmSalaryMonthRecordDO record = hrmSalaryMonthRecordMapper.selectById(id);
        if (record == null) {
            throw exception(HRM_SALARY_MONTH_RECORD_NOT_EXISTS);
        }
        return record;
    }

    private HrmSalarySlipTemplateDO validateSlipTemplateExists(Long id) {
        HrmSalarySlipTemplateDO template = hrmSalarySlipTemplateMapper.selectById(id);
        if (template == null) {
            throw exception(HRM_SALARY_SLIP_TEMPLATE_NOT_EXISTS);
        }
        return template;
    }

    private HrmSalarySlipSendRecordDO validateSlipSendRecordExists(Long id) {
        HrmSalarySlipSendRecordDO record = hrmSalarySlipSendRecordMapper.selectById(id);
        if (record == null) {
            throw exception(HRM_SALARY_SLIP_SEND_RECORD_NOT_EXISTS);
        }
        return record;
    }

    private HrmSalarySlipDO validateSlipExists(Long id) {
        HrmSalarySlipDO slip = hrmSalarySlipMapper.selectById(id);
        if (slip == null) {
            throw exception(HRM_SALARY_SLIP_NOT_EXISTS);
        }
        return slip;
    }

}
