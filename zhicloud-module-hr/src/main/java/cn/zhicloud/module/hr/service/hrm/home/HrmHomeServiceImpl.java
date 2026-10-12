package cn.zhicloud.module.hr.service.hrm.home;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.security.core.util.SecurityFrameworkUtils;
import cn.zhicloud.module.hr.controller.admin.hrm.home.vo.*;
import cn.zhicloud.module.hr.dal.dataobject.employee.HrEmployeeDO;
import cn.zhicloud.module.hr.dal.dataobject.hrm.*;
import cn.zhicloud.module.hr.dal.dataobject.recruit.HrmRecruitCandidateDO;
import cn.zhicloud.module.hr.dal.dataobject.recruit.HrmRecruitPostDO;
import cn.zhicloud.module.hr.dal.dataobject.salary2.HrmSalaryMonthEmployeeRecordDO;
import cn.zhicloud.module.hr.dal.dataobject.salary2.HrmSalaryMonthRecordDO;
import cn.zhicloud.module.hr.dal.mysql.employee.HrEmployeeMapper;
import cn.zhicloud.module.hr.dal.mysql.hrm.*;
import cn.zhicloud.module.hr.dal.mysql.recruit.HrmRecruitCandidateMapper;
import cn.zhicloud.module.hr.dal.mysql.recruit.HrmRecruitPostMapper;
import cn.zhicloud.module.hr.dal.mysql.salary2.HrmSalaryMonthEmployeeRecordMapper;
import cn.zhicloud.module.hr.dal.mysql.salary2.HrmSalaryMonthRecordMapper;
import cn.zhicloud.module.system.api.dept.DeptApi;
import cn.zhicloud.module.system.api.dept.dto.DeptRespDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HRM 工作台 Service 实现类
 *
 * 统计口径（对齐前端 api/hrm/home 契约）：
 * * 员工概览：在职=entry_status in (1,2,3)；本月入职/离职/待入职/待离职按 entry_time/leave_time 落本月；
 *   本月转正/调岗从 hr_employee_change_record 的 type=4/5 且 effect_time 落本月。
 * * 招聘概览：招聘中职位=status=1；流程中候选人=status in (2,3,4,5)；待入职候选人=6；已入职候选人=8。
 * * 薪资概览：取最近一个月度工资表（hr_salary_month_record）的实发合计；部门占比按员工所属部门聚合实发工资。
 * * 待办：待入职/待离职按状态计数；合同待到期=hr_employee_contract 30 天内到期且未到期；待转正=试用中(employee_status=2)；
 *   待核算工资表=0（薪资核算域未接线，诚实置 0）；本月生日=生日月=当前月。
 * * 团队工作台：以「当前登录用户绑定的员工」为 leader，其 leader_employee_id 指向他的员工即团队成员。
 *
 * @author zhicloud
 */
@Service
@Validated
public class HrmHomeServiceImpl implements HrmHomeService {

    @Resource
    private HrEmployeeMapper hrmEmployeeMapper;
    @Resource
    private HrmEmployeeChangeRecordMapper hrmChangeRecordMapper;
    @Resource
    private HrmEmployeePersonalNoteMapper hrmPersonalNoteMapper;
    @Resource
    private HrmRecruitPostMapper hrmRecruitPostMapper;
    @Resource
    private HrmRecruitCandidateMapper hrmRecruitCandidateMapper;
    @Resource
    private HrmSalaryMonthRecordMapper hrmSalaryMonthRecordMapper;
    @Resource
    private HrmSalaryMonthEmployeeRecordMapper hrmSalaryMonthEmployeeRecordMapper;
    @Resource
    private HrmEmployeeContractMapper hrmEmployeeContractMapper;
    @Resource
    private DeptApi deptApi;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ========== HR 首页统计 ==========

    @Override
    public HrmHrHomeStatisticsRespVO getHrStatisticsSummary() {
        HrmHrHomeStatisticsRespVO resp = new HrmHrHomeStatisticsRespVO();
        resp.setEmployeeSurvey(buildEmployeeSurvey());
        resp.setRecruitSurvey(buildRecruitSurvey());
        resp.setSalarySurvey(buildSalarySurvey());
        resp.setTodoSurvey(buildTodoSurvey());
        return resp;
    }

    private HrmHrHomeEmployeeSurveyVO buildEmployeeSurvey() {
        HrmHrHomeEmployeeSurveyVO vo = new HrmHrHomeEmployeeSurveyVO();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()).atTime(LocalTime.MAX);
        List<HrEmployeeDO> all = hrmEmployeeMapper.selectList(new LambdaQueryWrapper<>());
        vo.setActiveCount(all.stream().filter(e -> e.getEntryStatus() != null
                && (e.getEntryStatus() == 1 || e.getEntryStatus() == 2 || e.getEntryStatus() == 3)).count());
        vo.setEntryThisMonthCount(all.stream().filter(e -> inRange(e.getEntryTime(), monthStart, monthEnd)).count());
        vo.setPendingEntryThisMonthCount(all.stream().filter(e -> Integer.valueOf(2).equals(e.getEntryStatus())
                && inRange(e.getEntryTime(), monthStart, monthEnd)).count());
        vo.setLeaveThisMonthCount(all.stream().filter(e -> Integer.valueOf(4).equals(e.getEntryStatus())
                && inRange(e.getLeaveTime(), monthStart, monthEnd)).count());
        vo.setPendingLeaveThisMonthCount(all.stream().filter(e -> Integer.valueOf(3).equals(e.getEntryStatus())).count());
        // 本月转正/调岗：从异动记录
        List<HrmEmployeeChangeRecordDO> records = hrmChangeRecordMapper.selectList(new LambdaQueryWrapper<>());
        vo.setRegularThisMonthCount(records.stream()
                .filter(r -> Integer.valueOf(HrmEmployeeChangeRecordDO.TYPE_REGULAR).equals(r.getType())
                        && inRange(r.getEffectTime(), monthStart, monthEnd)).count());
        vo.setTransferThisMonthCount(records.stream()
                .filter(r -> Integer.valueOf(HrmEmployeeChangeRecordDO.TYPE_TRANSFER).equals(r.getType())
                        && inRange(r.getEffectTime(), monthStart, monthEnd)).count());
        return vo;
    }

    private HrmHrHomeRecruitSurveyVO buildRecruitSurvey() {
        HrmHrHomeRecruitSurveyVO vo = new HrmHrHomeRecruitSurveyVO();
        List<HrmRecruitPostDO> posts = hrmRecruitPostMapper.selectList(new LambdaQueryWrapper<>());
        vo.setRecruitingPostCount(posts.stream().filter(p -> Integer.valueOf(1).equals(p.getStatus())).count());
        List<HrmRecruitCandidateDO> candidates = hrmRecruitCandidateMapper.selectList(new LambdaQueryWrapper<>());
        vo.setCandidateInProcessCount(candidates.stream().filter(c -> c.getStatus() != null
                && (c.getStatus() >= 2 && c.getStatus() <= 5)).count());
        vo.setPendingEntryCount(candidates.stream().filter(c -> Integer.valueOf(6).equals(c.getStatus())).count());
        vo.setJoinedCount(candidates.stream().filter(c -> Integer.valueOf(8).equals(c.getStatus())).count());
        return vo;
    }

    private HrmHrHomeSalarySurveyVO buildSalarySurvey() {
        HrmHrHomeSalarySurveyVO vo = new HrmHrHomeSalarySurveyVO();
        // 取最近一个月度工资表（HRM 自有 hr_salary_month_record）作为统计口径
        HrmSalaryMonthRecordDO latest = hrmSalaryMonthRecordMapper.selectLast();
        if (latest == null) {
            vo.setEmployeeCount(0L);
            vo.setRealPaySalary(BigDecimal.ZERO);
            vo.setDeptProportions(Collections.emptyList());
            return vo;
        }
        List<HrmSalaryMonthEmployeeRecordDO> monthRecords =
                hrmSalaryMonthEmployeeRecordMapper.selectListByMonthRecordId(latest.getId());
        if (CollUtil.isEmpty(monthRecords)) {
            vo.setEmployeeCount(0L);
            vo.setRealPaySalary(BigDecimal.ZERO);
            vo.setDeptProportions(Collections.emptyList());
            return vo;
        }
        vo.setEmployeeCount((long) monthRecords.size());
        BigDecimal total = monthRecords.stream()
                .map(s -> s.getRealPaySalary() != null ? s.getRealPaySalary() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setRealPaySalary(total);
        // 部门占比：员工月度工资按员工所属部门聚合
        Map<Long, List<HrmSalaryMonthEmployeeRecordDO>> byEmployee = monthRecords.stream()
                .filter(s -> s.getEmployeeId() != null)
                .collect(Collectors.groupingBy(HrmSalaryMonthEmployeeRecordDO::getEmployeeId));
        Map<Long, HrEmployeeDO> employeeMap = byEmployee.isEmpty() ? Collections.emptyMap()
                : hrmEmployeeMapper.selectByIds(byEmployee.keySet()).stream()
                        .collect(Collectors.toMap(HrEmployeeDO::getId, e -> e, (a, b) -> a));
        Map<Long, BigDecimal> deptSalary = new HashMap<>();
        byEmployee.forEach((empId, list) -> {
            HrEmployeeDO employee = employeeMap.get(empId);
            if (employee == null || employee.getDeptId() == null) {
                return;
            }
            BigDecimal sum = list.stream()
                    .map(s -> s.getRealPaySalary() != null ? s.getRealPaySalary() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            deptSalary.merge(employee.getDeptId(), sum, BigDecimal::add);
        });
        Set<Long> deptIds = deptSalary.keySet();
        Map<Long, DeptRespDTO> deptMap = deptIds.isEmpty() ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        List<HrmHrHomeSalaryDeptVO> deptVOs = new ArrayList<>();
        BigDecimal grandTotal = total.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ONE : total;
        deptSalary.forEach((deptId, sum) -> {
            HrmHrHomeSalaryDeptVO deptVO = new HrmHrHomeSalaryDeptVO();
            deptVO.setDeptId(deptId);
            DeptRespDTO dept = deptMap.get(deptId);
            deptVO.setDeptName(dept != null ? dept.getName() : String.valueOf(deptId));
            deptVO.setTotalSalary(sum);
            deptVO.setProportion(sum.multiply(BigDecimal.valueOf(100))
                    .divide(grandTotal, 2, RoundingMode.HALF_UP));
            deptVOs.add(deptVO);
        });
        vo.setDeptProportions(deptVOs);
        return vo;
    }

    private HrmHrHomeTodoSurveyVO buildTodoSurvey() {
        HrmHrHomeTodoSurveyVO vo = new HrmHrHomeTodoSurveyVO();
        List<HrEmployeeDO> all = hrmEmployeeMapper.selectList(new LambdaQueryWrapper<>());
        vo.setToEntryCount(all.stream().filter(e -> Integer.valueOf(2).equals(e.getEntryStatus())).count());
        vo.setToLeaveCount(all.stream().filter(e -> Integer.valueOf(3).equals(e.getEntryStatus())).count());
        // 待转正：试用中
        vo.setToRegularCount(all.stream().filter(e -> Integer.valueOf(2).equals(e.getEmployeeStatus())).count());
        // 本月生日
        int currentMonth = LocalDate.now().getMonthValue();
        vo.setToBirthdayCount(all.stream().filter(e -> e.getBirthday() != null
                && e.getBirthday().getMonthValue() == currentMonth).count());
        // 合同待到期：HRM 员工合同 30 天内到期且未到期（hr_employee_contract.status != 2 已到期）
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(30);
        List<HrmEmployeeContractDO> contracts = hrmEmployeeContractMapper.selectList(new LambdaQueryWrapper<>());
        vo.setToExpireContractCount(contracts.stream().filter(c -> c.getEndTime() != null
                && !c.getEndTime().toLocalDate().isBefore(today)
                && !c.getEndTime().toLocalDate().isAfter(deadline)
                && (c.getStatus() == null || c.getStatus() != 2)).count());
        // 待核算工资表：薪资核算域未接线，诚实置 0
        vo.setToSalaryComputeCount(0L);
        return vo;
    }

    // ========== 日历 ==========

    @Override
    public List<HrmHomeCalendarItemVO> getHrCalendar(String startDate, String endDate) {
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        LocalDateTime rangeStart = start.atStartOfDay();
        LocalDateTime rangeEnd = end.atTime(LocalTime.MAX);
        List<HrmHomeCalendarItemVO> items = new ArrayList<>();
        // 个人备忘（当前登录用户）
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId != null) {
            hrmPersonalNoteMapper.selectListByUserId(userId).forEach(note -> {
                if (inRange(note.getReminderTime(), rangeStart, rangeEnd)) {
                    items.add(buildCalendarItem(note.getId(), 1, "备忘", note.getContent(), null,
                            note.getReminderTime()));
                }
            });
        }
        // 员工事项：入职/转正/离职
        List<HrEmployeeDO> employees = hrmEmployeeMapper.selectList(new LambdaQueryWrapper<>());
        for (HrEmployeeDO e : employees) {
            if (inRange(e.getEntryTime(), rangeStart, rangeEnd)) {
                items.add(buildCalendarItem(null, 3, "入职", e.getName() + " 入职", e.getId(), e.getEntryTime()));
            }
            if (inRange(e.getRegularTime(), rangeStart, rangeEnd)) {
                items.add(buildCalendarItem(null, 4, "转正", e.getName() + " 转正", e.getId(), e.getRegularTime()));
            }
            if (inRange(e.getLeaveTime(), rangeStart, rangeEnd)) {
                items.add(buildCalendarItem(null, 5, "离职", e.getName() + " 离职", e.getId(), e.getLeaveTime()));
            }
        }
        // 面试事项
        List<HrmRecruitCandidateDO> candidates = hrmRecruitCandidateMapper.selectList(new LambdaQueryWrapper<>());
        for (HrmRecruitCandidateDO c : candidates) {
            if (inRange(c.getInterviewTime(), rangeStart, rangeEnd)) {
                items.add(buildCalendarItem(null, 6, "面试", c.getName() + " 面试", c.getId(), c.getInterviewTime()));
            }
        }
        items.sort(Comparator.comparing(HrmHomeCalendarItemVO::getEventTime, Comparator.nullsLast(Comparator.naturalOrder())));
        return items;
    }

    @Override
    public List<HrmHomeCalendarItemVO> getTeamCalendar(String startDate, String endDate) {
        HrEmployeeDO leader = resolveLoginEmployee();
        if (leader == null) {
            return Collections.emptyList();
        }
        Set<Long> teamIds = teamMemberIds(leader.getId());
        if (teamIds.isEmpty()) {
            return Collections.emptyList();
        }
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        LocalDateTime rangeStart = start.atStartOfDay();
        LocalDateTime rangeEnd = end.atTime(LocalTime.MAX);
        List<HrmHomeCalendarItemVO> items = new ArrayList<>();
        List<HrEmployeeDO> members = hrmEmployeeMapper.selectByIds(teamIds);
        for (HrEmployeeDO e : members) {
            if (inRange(e.getEntryTime(), rangeStart, rangeEnd)) {
                items.add(buildCalendarItem(null, 3, "入职", e.getName() + " 入职", e.getId(), e.getEntryTime()));
            }
            if (inRange(e.getRegularTime(), rangeStart, rangeEnd)) {
                items.add(buildCalendarItem(null, 4, "转正", e.getName() + " 转正", e.getId(), e.getRegularTime()));
            }
            if (inRange(e.getLeaveTime(), rangeStart, rangeEnd)) {
                items.add(buildCalendarItem(null, 5, "离职", e.getName() + " 离职", e.getId(), e.getLeaveTime()));
            }
        }
        items.sort(Comparator.comparing(HrmHomeCalendarItemVO::getEventTime, Comparator.nullsLast(Comparator.naturalOrder())));
        return items;
    }

    // ========== 团队工作台 ==========

    @Override
    public HrmTeamHomeStatisticsRespVO getTeamStatisticsSummary() {
        HrmTeamHomeStatisticsRespVO resp = new HrmTeamHomeStatisticsRespVO();
        HrEmployeeDO leader = resolveLoginEmployee();
        if (leader == null) {
            resp.setTeamOverview(emptyTeamOverview());
            resp.setTeamSurvey(emptyTeamSurvey());
            return resp;
        }
        resp.setLeaderEmployeeId(leader.getId());
        Set<Long> teamIds = teamMemberIds(leader.getId());
        List<HrEmployeeDO> members = teamIds.isEmpty() ? Collections.emptyList()
                : hrmEmployeeMapper.selectByIds(teamIds);

        HrmTeamHomeOverviewVO overview = new HrmTeamHomeOverviewVO();
        overview.setEmployeeCount((long) members.size());
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()).atTime(LocalTime.MAX);
        overview.setEntryThisMonthCount(members.stream().filter(e -> inRange(e.getEntryTime(), monthStart, monthEnd)).count());
        overview.setLeaveThisMonthCount(members.stream().filter(e -> Integer.valueOf(4).equals(e.getEntryStatus())
                && inRange(e.getLeaveTime(), monthStart, monthEnd)).count());
        overview.setRegularThisMonthCount(members.stream().filter(e -> inRange(e.getRegularTime(), monthStart, monthEnd)).count());
        resp.setTeamOverview(overview);

        HrmTeamHomeSurveyVO survey = new HrmTeamHomeSurveyVO();
        survey.setStatusAnalysis(groupCount(members, HrEmployeeDO::getEmployeeStatus));
        survey.setSexAnalysis(groupCount(members, HrEmployeeDO::getSex));
        // 年龄分段：<25, 25-30, 31-35, 36-40, >40（type 用段序号）
        survey.setAgeAnalysis(ageAnalysis(members));
        // 司龄分段：<1, 1-3, 3-5, 5-10, >10（type 用段序号）
        survey.setCompanyAgeAnalysis(companyAgeAnalysis(members));
        resp.setTeamSurvey(survey);
        return resp;
    }

    private HrmTeamHomeOverviewVO emptyTeamOverview() {
        HrmTeamHomeOverviewVO vo = new HrmTeamHomeOverviewVO();
        vo.setEmployeeCount(0L);
        vo.setEntryThisMonthCount(0L);
        vo.setLeaveThisMonthCount(0L);
        vo.setRegularThisMonthCount(0L);
        return vo;
    }

    private HrmTeamHomeSurveyVO emptyTeamSurvey() {
        HrmTeamHomeSurveyVO vo = new HrmTeamHomeSurveyVO();
        vo.setStatusAnalysis(Collections.emptyList());
        vo.setSexAnalysis(Collections.emptyList());
        vo.setAgeAnalysis(Collections.emptyList());
        vo.setCompanyAgeAnalysis(Collections.emptyList());
        return vo;
    }

    private List<HrmTeamHomeAnalysisItemVO> groupCount(List<HrEmployeeDO> members,
                                                       java.util.function.Function<HrEmployeeDO, Integer> keyFn) {
        Map<Integer, Long> map = members.stream()
                .map(keyFn).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(k -> k, Collectors.counting()));
        List<HrmTeamHomeAnalysisItemVO> result = new ArrayList<>();
        map.forEach((type, count) -> {
            HrmTeamHomeAnalysisItemVO item = new HrmTeamHomeAnalysisItemVO();
            item.setType(type);
            item.setCount(count);
            result.add(item);
        });
        result.sort(Comparator.comparing(HrmTeamHomeAnalysisItemVO::getType));
        return result;
    }

    private List<HrmTeamHomeAnalysisItemVO> ageAnalysis(List<HrEmployeeDO> members) {
        LocalDate today = LocalDate.now();
        int[] buckets = new int[5]; // <25, 25-30, 31-35, 36-40, >40
        for (HrEmployeeDO e : members) {
            if (e.getBirthday() == null) {
                continue;
            }
            int age = java.time.Period.between(e.getBirthday().toLocalDate(), today).getYears();
            if (age < 25) {
                buckets[0]++;
            } else if (age <= 30) {
                buckets[1]++;
            } else if (age <= 35) {
                buckets[2]++;
            } else if (age <= 40) {
                buckets[3]++;
            } else {
                buckets[4]++;
            }
        }
        return bucketsToItems(buckets);
    }

    private List<HrmTeamHomeAnalysisItemVO> companyAgeAnalysis(List<HrEmployeeDO> members) {
        LocalDate today = LocalDate.now();
        int[] buckets = new int[5]; // <1, 1-3, 3-5, 5-10, >10
        for (HrEmployeeDO e : members) {
            if (e.getCompanyAgeStartTime() == null) {
                continue;
            }
            long years = java.time.temporal.ChronoUnit.YEARS.between(e.getCompanyAgeStartTime().toLocalDate(), today);
            if (years < 1) {
                buckets[0]++;
            } else if (years < 3) {
                buckets[1]++;
            } else if (years < 5) {
                buckets[2]++;
            } else if (years <= 10) {
                buckets[3]++;
            } else {
                buckets[4]++;
            }
        }
        return bucketsToItems(buckets);
    }

    private List<HrmTeamHomeAnalysisItemVO> bucketsToItems(int[] buckets) {
        List<HrmTeamHomeAnalysisItemVO> result = new ArrayList<>();
        for (int i = 0; i < buckets.length; i++) {
            HrmTeamHomeAnalysisItemVO item = new HrmTeamHomeAnalysisItemVO();
            item.setType(i + 1);
            item.setCount((long) buckets[i]);
            result.add(item);
        }
        return result;
    }

    // ========== 私有：小工具 ==========

    /**
     * 解析当前登录用户绑定的员工（无绑定返回 null）
     */
    private HrEmployeeDO resolveLoginEmployee() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            return null;
        }
        List<HrEmployeeDO> list = hrmEmployeeMapper.selectListByUserIds(Collections.singletonList(userId));
        return CollUtil.isNotEmpty(list) ? list.get(0) : null;
    }

    /**
     * 团队成员编号：leader_employee_id = leaderId 的员工（一层，按契约「我的团队」口径）
     */
    private Set<Long> teamMemberIds(Long leaderId) {
        List<HrEmployeeDO> members = hrmEmployeeMapper.selectList(
                new LambdaQueryWrapper<HrEmployeeDO>().eq(HrEmployeeDO::getLeaderEmployeeId, leaderId));
        return members.stream().map(HrEmployeeDO::getId).collect(Collectors.toSet());
    }

    private HrmHomeCalendarItemVO buildCalendarItem(Long personalNoteId, int type, String typeName,
                                                     String content, Long typeId, LocalDateTime eventTime) {
        HrmHomeCalendarItemVO item = new HrmHomeCalendarItemVO();
        item.setPersonalNoteId(personalNoteId);
        item.setType(type);
        item.setTypeName(typeName);
        item.setContent(content);
        item.setTypeId(typeId);
        item.setEventTime(eventTime);
        item.setDate(eventTime != null ? eventTime.toLocalDate().format(DATE_FMT) : null);
        return item;
    }

    private static boolean inRange(LocalDateTime time, LocalDateTime start, LocalDateTime end) {
        return time != null && !time.isBefore(start) && !time.isAfter(end);
    }

    private static LocalDate parseDate(String date) {
        if (date == null || date.isEmpty()) {
            LocalDate today = LocalDate.now();
            return today.withDayOfMonth(1);
        }
        return LocalDate.parse(date.trim(), DATE_FMT);
    }

}
