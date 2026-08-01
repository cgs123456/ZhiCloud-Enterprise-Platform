package cn.iocoder.yudao.module.hr.service.salary;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.hr.controller.admin.attendance.vo.HrAttendanceMonthlySummaryRespVO;
import cn.iocoder.yudao.module.hr.controller.admin.salary.vo.HrSalaryApproveReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.salary.vo.HrSalaryCalculateReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.salary.vo.HrSalaryPageReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.salary.vo.HrSalarySaveReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.socialinsurance.vo.SocialInsuranceDetailRespVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.employee.HrEmployeeDO;
import cn.iocoder.yudao.module.hr.dal.dataobject.position.HrPositionDO;
import cn.iocoder.yudao.module.hr.dal.dataobject.salary.HrSalaryDO;
import cn.iocoder.yudao.module.hr.dal.mysql.salary.HrSalaryMapper;
import cn.iocoder.yudao.module.hr.enums.salary.HrSalaryStatusEnum;
import cn.iocoder.yudao.module.hr.service.attendance.HrAttendanceService;
import cn.iocoder.yudao.module.hr.service.employee.HrEmployeeService;
import cn.iocoder.yudao.module.hr.service.position.HrPositionService;
import cn.iocoder.yudao.module.hr.service.socialinsurance.HrSocialInsuranceService;
import cn.iocoder.yudao.module.hr.util.HrIitCalculator;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hr.enums.ErrorCodeConstants.*;

/**
 * HR 薪资记录 Service 实现类
 *
 * @author yudao
 */
@Service
@Validated
public class HrSalaryServiceImpl implements HrSalaryService {

    /**
     * 月计薪天数（劳动法标准）
     */
    private static final BigDecimal MONTHLY_WORK_DAYS = new BigDecimal("21.75");
    /**
     * 日工作时长（小时）
     */
    private static final BigDecimal DAILY_WORK_HOURS = new BigDecimal("8");
    /**
     * 加班费倍率（工作日延时 1.5 倍）
     */
    private static final BigDecimal OVERTIME_RATE = new BigDecimal("1.5");
    /**
     * 个税起征点（每月减除费用，用于旧版简化计算）
     */
    private static final BigDecimal TAX_THRESHOLD = new BigDecimal("5000");

    @Resource
    private HrSalaryMapper salaryMapper;
    @Resource
    private HrEmployeeService employeeService;
    @Resource
    private HrPositionService positionService;
    @Resource
    private HrAttendanceService attendanceService;
    @Resource
    private HrSocialInsuranceService socialInsuranceService;

    @Override
    public Long createSalary(HrSalarySaveReqVO createReqVO) {
        HrSalaryDO salary = BeanUtils.toBean(createReqVO, HrSalaryDO.class);
        salaryMapper.insert(salary);
        return salary.getId();
    }

    @Override
    public void updateSalary(HrSalarySaveReqVO updateReqVO) {
        // 校验存在
        HrSalaryDO salary = validateSalaryExists(updateReqVO.getId());
        // 已审核的薪资不允许修改
        if (!HrSalaryStatusEnum.DRAFT.getStatus().equals(salary.getStatus())) {
            throw exception(HR_SALARY_ALREADY_APPROVED);
        }
        // 更新
        HrSalaryDO updateObj = BeanUtils.toBean(updateReqVO, HrSalaryDO.class);
        salaryMapper.updateById(updateObj);
    }

    @Override
    public void deleteSalary(Long id) {
        // 校验存在
        validateSalaryExists(id);
        // 删除
        salaryMapper.deleteById(id);
    }

    private HrSalaryDO validateSalaryExists(Long id) {
        HrSalaryDO salary = salaryMapper.selectById(id);
        if (salary == null) {
            throw exception(HR_SALARY_NOT_EXISTS);
        }
        return salary;
    }

    @Override
    public HrSalaryDO getSalary(Long id) {
        return salaryMapper.selectById(id);
    }

    @Override
    public PageResult<HrSalaryDO> getSalaryPage(HrSalaryPageReqVO pageReqVO) {
        return salaryMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long calculateMonthlySalary(HrSalaryCalculateReqVO reqVO) {
        // 0. 幂等校验：同一员工同一月份不可重复计算
        HrSalaryDO existingSalary = salaryMapper.selectByEmployeeAndMonth(reqVO.getEmployeeId(), reqVO.getSalaryMonth());
        if (existingSalary != null) {
            throw exception(HR_SALARY_ALREADY_CALCULATED);
        }
        // 1. 获取员工
        HrEmployeeDO employee = employeeService.getEmployee(reqVO.getEmployeeId());
        if (employee == null) {
            throw exception(HR_EMPLOYEE_NOT_EXISTS);
        }
        // 2. 获取职位（基本工资）
        BigDecimal baseSalary = BigDecimal.ZERO;
        if (employee.getPositionId() != null) {
            HrPositionDO position = positionService.getPosition(employee.getPositionId());
            if (position != null && position.getBaseSalary() != null) {
                baseSalary = position.getBaseSalary();
            }
        }
        // 3. 获取考勤月度汇总（加班时长）
        HrAttendanceMonthlySummaryRespVO summary = attendanceService.getMonthlySummary(
                reqVO.getEmployeeId(), reqVO.getSalaryMonth());
        BigDecimal overtimeHours = summary.getTotalOvertimeHours() == null
                ? BigDecimal.ZERO : summary.getTotalOvertimeHours();
        // 4. 计算加班费：加班时长 × (基本工资 / 21.75 / 8) × 1.5
        BigDecimal hourlyRate = baseSalary.divide(MONTHLY_WORK_DAYS, 4, RoundingMode.HALF_UP)
                .divide(DAILY_WORK_HOURS, 4, RoundingMode.HALF_UP);
        BigDecimal overtimePay = hourlyRate.multiply(OVERTIME_RATE).multiply(overtimeHours)
                .setScale(2, RoundingMode.HALF_UP);
        // 5. 奖金、扣款
        BigDecimal bonus = reqVO.getBonus() == null ? BigDecimal.ZERO : reqVO.getBonus();
        BigDecimal deduction = reqVO.getDeduction() == null ? BigDecimal.ZERO : reqVO.getDeduction();
        // 6. 社保、公积金：优先使用请求参数，未传则通过社保服务计算
        BigDecimal socialInsurance;
        BigDecimal housingFund;
        SocialInsuranceDetailRespVO insuranceDetail = null;
        if (reqVO.getSocialInsurance() == null || reqVO.getHousingFund() == null) {
            insuranceDetail = socialInsuranceService.calculateMonthly(reqVO.getEmployeeId());
        }
        socialInsurance = reqVO.getSocialInsurance() != null ? reqVO.getSocialInsurance()
                : (insuranceDetail != null && insuranceDetail.getPersonalTotal() != null
                ? insuranceDetail.getPersonalTotal() : BigDecimal.ZERO);
        housingFund = reqVO.getHousingFund() != null ? reqVO.getHousingFund()
                : (insuranceDetail != null && insuranceDetail.getPersonalHousingFund() != null
                ? insuranceDetail.getPersonalHousingFund() : BigDecimal.ZERO);
        // 7. 个税（累计预扣预缴法）
        // 月度应纳税所得额 = 基本工资 + 加班费 + 奖金 - 扣款 - 社保 - 公积金 - 5000（每月减除费用）
        BigDecimal monthlyTaxableIncome = baseSalary.add(overtimePay).add(bonus)
                .subtract(deduction).subtract(socialInsurance).subtract(housingFund)
                .subtract(HrIitCalculator.MONTHLY_DEDUCTION);
        if (monthlyTaxableIncome.compareTo(BigDecimal.ZERO) < 0) {
            monthlyTaxableIncome = BigDecimal.ZERO;
        }
        int[] yearMonth = parseYearMonth(reqVO.getSalaryMonth());
        BigDecimal tax = HrIitCalculator.calculateMonthlyIIT(monthlyTaxableIncome,
                BigDecimal.ZERO, BigDecimal.ZERO, yearMonth[0], yearMonth[1]);
        // 8. 实发工资 = 基本工资 + 加班费 + 奖金 - 扣款 - 社保 - 公积金 - 个税
        BigDecimal netSalary = baseSalary.add(overtimePay).add(bonus)
                .subtract(deduction).subtract(socialInsurance).subtract(housingFund).subtract(tax)
                .setScale(2, RoundingMode.HALF_UP);
        // 9. 插入薪资记录（草稿状态）
        HrSalaryDO salary = HrSalaryDO.builder()
                .employeeId(reqVO.getEmployeeId())
                .salaryMonth(reqVO.getSalaryMonth())
                .baseSalary(baseSalary)
                .overtimePay(overtimePay)
                .bonus(bonus)
                .deduction(deduction)
                .socialInsurance(socialInsurance)
                .housingFund(housingFund)
                .tax(tax)
                .netSalary(netSalary)
                .status(HrSalaryStatusEnum.DRAFT.getStatus())
                .build();
        salaryMapper.insert(salary);
        return salary.getId();
    }

    /**
     * 解析薪资月份（yyyyMM）为 [year, month]
     */
    private int[] parseYearMonth(String salaryMonth) {
        try {
            if (salaryMonth != null && salaryMonth.length() >= 6) {
                int year = Integer.parseInt(salaryMonth.substring(0, 4));
                int month = Integer.parseInt(salaryMonth.substring(4, 6));
                return new int[]{year, month};
            }
        } catch (NumberFormatException ignored) {
        }
        return new int[]{java.time.LocalDate.now().getYear(), java.time.LocalDate.now().getMonthValue()};
    }

    @Override
    public void approveSalary(HrSalaryApproveReqVO reqVO) {
        // 校验存在
        HrSalaryDO salary = validateSalaryExists(reqVO.getId());
        // 校验状态：仅草稿状态可审核
        if (!HrSalaryStatusEnum.DRAFT.getStatus().equals(salary.getStatus())) {
            throw exception(HR_SALARY_STATUS_INVALID);
        }
        // 更新为已审核
        HrSalaryDO updateObj = new HrSalaryDO();
        updateObj.setId(reqVO.getId());
        updateObj.setStatus(HrSalaryStatusEnum.APPROVED.getStatus());
        salaryMapper.updateById(updateObj);
    }

    /**
     * 简化版个人所得税计算（月度简化版，已被 {@link HrIitCalculator} 累计预扣预缴法替代）。
     *
     * @param monthlyIncome 月度应纳税所得额（已扣除社保公积金等）
     * @return 个税金额
     * @deprecated 请使用 {@link HrIitCalculator#calculateMonthlyIIT(BigDecimal, BigDecimal, BigDecimal, int, int)}
     */
    @Deprecated
    private BigDecimal calculateTax(BigDecimal monthlyIncome) {
        if (monthlyIncome == null || monthlyIncome.compareTo(TAX_THRESHOLD) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal taxable = monthlyIncome.subtract(TAX_THRESHOLD);
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal[] brackets = {
                new BigDecimal("3000"), new BigDecimal("12000"),
                new BigDecimal("25000"), new BigDecimal("35000")
        };
        BigDecimal[] rates = {
                new BigDecimal("0.03"), new BigDecimal("0.10"),
                new BigDecimal("0.20"), new BigDecimal("0.25"), new BigDecimal("0.30")
        };
        BigDecimal lower = BigDecimal.ZERO;
        for (int i = 0; i < brackets.length; i++) {
            BigDecimal upper = brackets[i];
            if (taxable.compareTo(lower) > 0) {
                BigDecimal portion = taxable.min(upper).subtract(lower);
                tax = tax.add(portion.multiply(rates[i]));
            }
            lower = upper;
            if (taxable.compareTo(upper) <= 0) {
                break;
            }
        }
        if (taxable.compareTo(new BigDecimal("35000")) > 0) {
            tax = tax.add(taxable.subtract(new BigDecimal("35000"))
                    .multiply(new BigDecimal("0.30")));
        }
        return tax.setScale(2, RoundingMode.HALF_UP);
    }

}