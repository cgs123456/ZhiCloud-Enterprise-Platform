package cn.zhicloud.module.hr.service.hrm.portal;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.zhicloud.framework.common.pojo.PageParam;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.json.JsonUtils;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.framework.security.core.util.SecurityFrameworkUtils;
import cn.zhicloud.module.bpm.api.task.BpmProcessInstanceApi;
import cn.zhicloud.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.zhicloud.module.hr.controller.admin.hrm.performance.vo.HrmPerformanceAssessmentDimensionVO;
import cn.zhicloud.module.hr.controller.admin.hrm.performance.vo.HrmPerformanceAssessmentQuotaVO;
import cn.zhicloud.module.hr.controller.admin.hrm.performance.vo.HrmPerformanceAssessmentStageVO;
import cn.zhicloud.module.hr.controller.admin.hrm.portal.vo.*;
import cn.zhicloud.module.hr.controller.admin.hrm.vo.*;
import cn.zhicloud.module.hr.dal.dataobject.employee.HrEmployeeDO;
import cn.zhicloud.module.hr.dal.dataobject.hrm.*;
import cn.zhicloud.module.hr.dal.dataobject.insurance.HrmInsuranceEmployeeInfoDO;
import cn.zhicloud.module.hr.dal.dataobject.insurance.HrmInsuranceSchemeDO;
import cn.zhicloud.module.hr.dal.dataobject.insurance.HrmInsuranceSchemeProjectDO;
import cn.zhicloud.module.hr.dal.dataobject.performance.*;
import cn.zhicloud.module.hr.dal.dataobject.salary2.HrmSalaryMonthEmployeeRecordDO;
import cn.zhicloud.module.hr.dal.dataobject.salary2.HrmSalarySlipDO;
import cn.zhicloud.module.hr.dal.mysql.employee.HrEmployeeMapper;
import cn.zhicloud.module.hr.dal.mysql.hrm.*;
import cn.zhicloud.module.hr.dal.mysql.insurance.HrmInsuranceEmployeeInfoMapper;
import cn.zhicloud.module.hr.dal.mysql.insurance.HrmInsuranceSchemeMapper;
import cn.zhicloud.module.hr.dal.mysql.insurance.HrmInsuranceSchemeProjectMapper;
import cn.zhicloud.module.hr.dal.mysql.performance.*;
import cn.zhicloud.module.hr.dal.mysql.salary2.HrmSalaryMonthEmployeeRecordMapper;
import cn.zhicloud.module.hr.dal.mysql.salary2.HrmSalarySlipMapper;
import cn.zhicloud.module.system.api.dept.DeptApi;
import cn.zhicloud.module.system.api.dept.dto.DeptRespDTO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.hr.enums.ErrorCodeConstants.*;

/**
 * HRM 员工自助端（portal）Service 实现类
 *
 * 数据边界：所有查询/操作都以「当前登录用户绑定的员工」为准，未绑定员工时抛
 * {@code HRM_PORTAL_EMPLOYEE_NOT_BOUND}。
 *
 * 绩效任务口径（对齐前端 views/hrm/portal/performance/assessment）：
 * 阶段类型 stageType：1员工填写 2目标确认 3自评 4他人评分 5结果审核 6结果确认 7申诉确认 8归档。
 * 任务页签按 stageType 过滤：fill=1、target=2、review=4、resultAudit=5、resultConfirm=6、appeal=7（申诉以 appealStatus 判定）；
 * 各阶段「待处理」= 当前 stageType 等于该阶段，「已处理」= 当前 stageType 已越过该阶段。
 *
 * @author zhicloud
 */
@Service
@Validated
public class HrmPortalServiceImpl implements HrmPortalService {

    /** 绩效阶段类型 */
    private static final int STAGE_FILL = 1;
    private static final int STAGE_TARGET_CONFIRM = 2;
    private static final int STAGE_SELF_SCORE = 3;
    private static final int STAGE_OTHER_SCORE = 4;
    private static final int STAGE_RESULT_AUDIT = 5;
    private static final int STAGE_RESULT_CONFIRM = 6;
    private static final int STAGE_APPEAL_CONFIRM = 7;
    private static final int STAGE_ARCHIVED = 8;
    /** 阶段状态 */
    private static final int STAGE_STATUS_PROCESSED = 1;
    private static final int STAGE_STATUS_PENDING = 2;
    private static final int STAGE_STATUS_REJECTED = 3;
    /** 任务状态（前端 HrmPerformanceAssessmentStageStatus） */
    private static final int TASK_STATUS_PROCESSED = 1;
    private static final int TASK_STATUS_PENDING = 2;
    private static final int TASK_STATUS_APPEALED = 5;
    /** 申诉状态 */
    private static final int APPEAL_STATUS_NONE = 0;
    private static final int APPEAL_STATUS_PENDING = 1;
    private static final int APPEAL_STATUS_PASS = 2;
    private static final int APPEAL_STATUS_REJECT = 3;
    /** 请假审批状态（对齐 BPM 流程实例状态）：1审批中 4已取消 */
    /** 请假流程定义 KEY（HRM 考勤请假专用；与旧 /hr 请假域 hr_leave_request 区分，避免监听器串扰） */
    private static final String LEAVE_PROCESS_KEY = "hrm_leave_request";
    private static final int LEAVE_APPROVAL_RUNNING = 1;
    private static final int LEAVE_APPROVAL_APPROVED = 2;
    private static final int LEAVE_APPROVAL_REJECTED = 3;
    private static final int LEAVE_APPROVAL_CANCEL = 4;
    /** 绩效流程记录业务类型：2员工考核 */
    private static final int BIZ_TYPE_ASSESSMENT = 2;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Resource
    private HrEmployeeMapper hrmEmployeeMapper;
    @Resource
    private HrmAttendanceClockMapper hrmAttendanceClockMapper;
    @Resource
    private HrmAttendanceLeaveMapper hrmAttendanceLeaveMapper;
    @Resource
    private HrmPerformanceAssessmentMapper hrmPerformanceAssessmentMapper;
    @Resource
    private HrmPerformanceAssessmentDimensionMapper hrmPerformanceAssessmentDimensionMapper;
    @Resource
    private HrmPerformanceAssessmentQuotaMapper hrmPerformanceAssessmentQuotaMapper;
    @Resource
    private HrmPerformanceAssessmentStageMapper hrmPerformanceAssessmentStageMapper;
    @Resource
    private HrmPerformancePlanMapper hrmPerformancePlanMapper;
    @Resource
    private HrmPerformanceProcessRecordMapper hrmPerformanceProcessRecordMapper;
    @Resource
    private HrmSalarySlipMapper hrmSalarySlipMapper;
    @Resource
    private HrmSalaryMonthEmployeeRecordMapper hrmSalaryMonthEmployeeRecordMapper;
    @Resource
    private HrmInsuranceEmployeeInfoMapper hrmInsuranceEmployeeInfoMapper;
    @Resource
    private HrmInsuranceSchemeMapper hrmInsuranceSchemeMapper;
    @Resource
    private HrmInsuranceSchemeProjectMapper hrmInsuranceSchemeProjectMapper;
    @Resource
    private HrmEmployeeCertificateMapper hrmEmployeeCertificateMapper;
    @Resource
    private HrmEmployeeContactMapper hrmEmployeeContactMapper;
    @Resource
    private HrmEmployeeEducationExperienceMapper hrmEmployeeEducationExperienceMapper;
    @Resource
    private HrmEmployeeWorkExperienceMapper hrmEmployeeWorkExperienceMapper;
    @Resource
    private HrmEmployeeTrainingExperienceMapper hrmEmployeeTrainingExperienceMapper;
    @Resource
    private HrmEmployeeQuitInfoMapper hrmEmployeeQuitInfoMapper;
    @Resource
    private HrmEmployeeFieldConfigMapper hrmEmployeeFieldConfigMapper;
    @Resource
    private HrmEmployeePersonalNoteMapper hrmEmployeePersonalNoteMapper;
    @Resource
    private BpmProcessInstanceApi bpmProcessInstanceApi;
    @Resource
    private DeptApi deptApi;

    // ========== 员工档案 ==========

    @Override
    public boolean getBindStatus() {
        return resolveLoginEmployeeOrNull() != null;
    }

    @Override
    public HrmPortalEmployeeVO getEmployee() {
        HrEmployeeDO employee = resolveLoginEmployee();
        return buildEmployeeVO(employee);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEmployee(HrmPortalEmployeeUpdateReqVO reqVO) {
        HrEmployeeDO employee = resolveLoginEmployee();
        LambdaUpdateWrapper<HrEmployeeDO> update = new LambdaUpdateWrapper<HrEmployeeDO>()
                .eq(HrEmployeeDO::getId, employee.getId());
        // 仅更新可自助编辑字段；未传（null）的字段保持原值
        if (reqVO.getName() != null) {
            update.set(HrEmployeeDO::getName, reqVO.getName());
        }
        if (reqVO.getMobile() != null) {
            update.set(HrEmployeeDO::getPhone, reqVO.getMobile());
        }
        if (reqVO.getCountry() != null) {
            update.set(HrEmployeeDO::getCountry, reqVO.getCountry());
        }
        if (reqVO.getNation() != null) {
            update.set(HrEmployeeDO::getNation, reqVO.getNation());
        }
        if (reqVO.getIdType() != null) {
            update.set(HrEmployeeDO::getIdType, reqVO.getIdType());
        }
        if (reqVO.getIdNumber() != null) {
            update.set(HrEmployeeDO::getIdCard, reqVO.getIdNumber());
        }
        if (reqVO.getSex() != null) {
            update.set(HrEmployeeDO::getSex, reqVO.getSex());
        }
        if (reqVO.getEmail() != null) {
            update.set(HrEmployeeDO::getEmail, reqVO.getEmail());
        }
        if (reqVO.getNativePlace() != null) {
            update.set(HrEmployeeDO::getNativePlace, reqVO.getNativePlace());
        }
        if (reqVO.getBirthday() != null) {
            update.set(HrEmployeeDO::getBirthday, reqVO.getBirthday());
        }
        if (reqVO.getAddress() != null) {
            update.set(HrEmployeeDO::getAddress, reqVO.getAddress());
        }
        if (reqVO.getHighestEducation() != null) {
            update.set(HrEmployeeDO::getHighestEducation, reqVO.getHighestEducation());
        }
        hrmEmployeeMapper.update(null, update);
    }

    private HrmPortalEmployeeVO buildEmployeeVO(HrEmployeeDO employee) {
        HrmPortalEmployeeVO vo = new HrmPortalEmployeeVO();
        vo.setId(employee.getId());
        vo.setEmployeeId(employee.getId());
        vo.setName(employee.getName());
        vo.setJobNumber(employee.getEmpNo());
        vo.setMobile(employee.getPhone());
        vo.setCountry(employee.getCountry());
        vo.setNation(employee.getNation());
        vo.setIdType(employee.getIdType());
        vo.setIdNumber(employee.getIdCard());
        vo.setSex(employee.getSex());
        vo.setEmail(employee.getEmail());
        vo.setNativePlace(employee.getNativePlace());
        vo.setBirthday(employee.getBirthday());
        vo.setAge(calcAge(employee.getBirthday()));
        vo.setAddress(employee.getAddress());
        vo.setHighestEducation(employee.getHighestEducation());
        vo.setDeptId(employee.getDeptId());
        vo.setLeaderEmployeeId(employee.getLeaderEmployeeId());
        vo.setEntryStatus(employee.getEntryStatus());
        vo.setStatus(employee.getEmployeeStatus());
        vo.setType(employee.getType());
        vo.setEntryTime(employee.getEntryTime());
        vo.setEntryDay(calcEntryDay(employee.getEntryTime()));
        vo.setProbation(employee.getProbation());
        vo.setRegularTime(employee.getRegularTime());
        vo.setLeaveTime(employee.getLeaveTime());
        vo.setPostName(employee.getPostName());
        vo.setPostLevel(employee.getPostLevel());
        vo.setWorkCity(employee.getWorkCity());
        vo.setWorkAddress(employee.getWorkAddress());
        vo.setWorkDetailAddress(employee.getWorkDetailAddress());
        vo.setCompanyAgeStartTime(employee.getCompanyAgeStartTime());
        vo.setCompanyAge(calcCompanyAge(employee.getCompanyAgeStartTime()));
        vo.setRemark(employee.getRemark());
        // 部门名称
        if (employee.getDeptId() != null) {
            DeptRespDTO dept = deptApi.getDept(employee.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
        }
        // 直属上级姓名
        if (employee.getLeaderEmployeeId() != null) {
            HrEmployeeDO leader = hrmEmployeeMapper.selectById(employee.getLeaderEmployeeId());
            if (leader != null) {
                vo.setLeaderEmployeeName(leader.getName());
            }
        }
        return vo;
    }

    // ========== 考勤 ==========

    @Override
    public List<HrmPortalAttendanceClockVO> getClockList(String startDate, String endDate) {
        HrEmployeeDO employee = resolveLoginEmployee();
        LocalDateTime rangeStart = parseDateStart(startDate);
        LocalDateTime rangeEnd = parseDateEnd(endDate);
        List<HrmAttendanceClockDO> clocks = hrmAttendanceClockMapper.selectList(
                new LambdaQueryWrapperX<HrmAttendanceClockDO>()
                        .eq(HrmAttendanceClockDO::getEmployeeId, employee.getId())
                        .ge(HrmAttendanceClockDO::getAttendanceTime, rangeStart)
                        .le(HrmAttendanceClockDO::getAttendanceTime, rangeEnd)
                        .orderByDesc(HrmAttendanceClockDO::getClockTime));
        return BeanUtils.toBean(clocks, HrmPortalAttendanceClockVO.class);
    }

    @Override
    public List<HrmPortalAttendanceLeaveVO> getLeaveList(Integer year, Integer month) {
        HrEmployeeDO employee = resolveLoginEmployee();
        List<HrmAttendanceLeaveDO> leaves = hrmAttendanceLeaveMapper.selectList(
                new LambdaQueryWrapperX<HrmAttendanceLeaveDO>()
                        .eq(HrmAttendanceLeaveDO::getEmployeeId, employee.getId())
                        .orderByDesc(HrmAttendanceLeaveDO::getId));
        if (year != null) {
            leaves = leaves.stream()
                    .filter(l -> l.getStartTime() != null && l.getStartTime().getYear() == year)
                    .filter(l -> month == null || l.getStartTime().getMonthValue() == month)
                    .collect(Collectors.toList());
        }
        return BeanUtils.toBean(leaves, HrmPortalAttendanceLeaveVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createLeave(HrmPortalAttendanceLeaveCreateReqVO reqVO) {
        HrEmployeeDO employee = resolveLoginEmployee();
        HrmAttendanceLeaveDO leave = HrmAttendanceLeaveDO.builder()
                .employeeId(employee.getId())
                .type(reqVO.getType())
                .startTime(reqVO.getStartTime())
                .endTime(reqVO.getEndTime())
                .day(reqVO.getDay())
                .reason(reqVO.getReason())
                .remark(reqVO.getRemark())
                .approvalStatus(LEAVE_APPROVAL_RUNNING)
                .build();
        hrmAttendanceLeaveMapper.insert(leave);
        // 发起 BPM 审批流程（hr_leave_request；businessKey 回写假单编号，审批结果由监听器回写）
        Map<String, Object> variables = new HashMap<>();
        variables.put("leaveType", leave.getType());
        variables.put("day", leave.getDay());
        String processInstanceId = bpmProcessInstanceApi.createProcessInstance(
                SecurityFrameworkUtils.getLoginUserId(),
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(LEAVE_PROCESS_KEY)
                        .setVariables(variables).setBusinessKey(String.valueOf(leave.getId())));
        hrmAttendanceLeaveMapper.update(null, new LambdaUpdateWrapper<HrmAttendanceLeaveDO>()
                .eq(HrmAttendanceLeaveDO::getId, leave.getId())
                .set(HrmAttendanceLeaveDO::getProcessInstanceId, processInstanceId));
        return leave.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelLeave(HrmPortalAttendanceLeaveCancelReqVO reqVO) {
        HrEmployeeDO employee = resolveLoginEmployee();
        HrmAttendanceLeaveDO leave = hrmAttendanceLeaveMapper.selectById(reqVO.getId());
        if (leave == null) {
            throw exception(HRM_ATTENDANCE_LEAVE_NOT_EXISTS);
        }
        if (!leave.getEmployeeId().equals(employee.getId())) {
            throw exception(HRM_PORTAL_LEAVE_NOT_OWN);
        }
        if (leave.getApprovalStatus() != null && leave.getApprovalStatus() != LEAVE_APPROVAL_RUNNING) {
            throw exception(HRM_PORTAL_LEAVE_CANNOT_CANCEL);
        }
        hrmAttendanceLeaveMapper.update(null, new LambdaUpdateWrapper<HrmAttendanceLeaveDO>()
                .eq(HrmAttendanceLeaveDO::getId, leave.getId())
                .set(HrmAttendanceLeaveDO::getApprovalStatus, LEAVE_APPROVAL_CANCEL)
                .set(HrmAttendanceLeaveDO::getApprovalReason, reqVO.getReason())
                .set(HrmAttendanceLeaveDO::getApprovalTime, LocalDateTime.now()));
        // 注：BpmProcessInstanceApi 未暴露「取消实例」方法，此处只落本地撤销态；
        // 在途流程实例的撤销属 BPM 侧能力，后续如需可在 bpm 模块补 API 后接入。
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLeaveBpmResult(Long id, Integer bpmStatus, String processInstanceId) {
        HrmAttendanceLeaveDO leave = hrmAttendanceLeaveMapper.selectById(id);
        if (leave == null) {
            return;
        }
        // 幂等：审批结果只能从「审批中」落一次（BPM 事件至少一次投递）
        if (!Integer.valueOf(LEAVE_APPROVAL_RUNNING).equals(leave.getApprovalStatus())) {
            return;
        }
        Integer approvalStatus;
        if (Integer.valueOf(2).equals(bpmStatus)) {
            approvalStatus = LEAVE_APPROVAL_APPROVED;
        } else if (Integer.valueOf(3).equals(bpmStatus)) {
            approvalStatus = LEAVE_APPROVAL_REJECTED;
        } else if (Integer.valueOf(4).equals(bpmStatus)) {
            approvalStatus = LEAVE_APPROVAL_CANCEL;
        } else {
            return; // RUNNING 等中间态不倒写
        }
        hrmAttendanceLeaveMapper.update(null, new LambdaUpdateWrapper<HrmAttendanceLeaveDO>()
                .eq(HrmAttendanceLeaveDO::getId, id)
                .set(HrmAttendanceLeaveDO::getApprovalStatus, approvalStatus)
                .set(HrmAttendanceLeaveDO::getProcessInstanceId, processInstanceId)
                .set(HrmAttendanceLeaveDO::getApprovalTime, LocalDateTime.now()));
    }

    @Override
    public Map<String, Object> getAttendanceMonthDetail(Integer year, Integer month) {
        HrEmployeeDO employee = resolveLoginEmployee();
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
        LocalDateTime rangeStart = monthStart.atStartOfDay();
        LocalDateTime rangeEnd = monthEnd.atTime(LocalTime.MAX);
        List<HrmAttendanceClockDO> clocks = hrmAttendanceClockMapper.selectList(
                new LambdaQueryWrapperX<HrmAttendanceClockDO>()
                        .eq(HrmAttendanceClockDO::getEmployeeId, employee.getId())
                        .ge(HrmAttendanceClockDO::getAttendanceTime, rangeStart)
                        .le(HrmAttendanceClockDO::getAttendanceTime, rangeEnd)
                        .orderByAsc(HrmAttendanceClockDO::getClockTime));
        List<HrmAttendanceLeaveDO> leaves = hrmAttendanceLeaveMapper.selectList(
                new LambdaQueryWrapperX<HrmAttendanceLeaveDO>()
                        .eq(HrmAttendanceLeaveDO::getEmployeeId, employee.getId())
                        .ge(HrmAttendanceLeaveDO::getStartTime, rangeStart)
                        .le(HrmAttendanceLeaveDO::getStartTime, rangeEnd)
                        .orderByAsc(HrmAttendanceLeaveDO::getStartTime));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("employeeId", employee.getId());
        summary.put("employeeName", employee.getName());
        summary.put("jobNumber", employee.getEmpNo());
        summary.put("year", year);
        summary.put("month", month);
        summary.put("actualDays", BigDecimal.valueOf(clocks.stream()
                .map(HrmAttendanceClockDO::getAttendanceTime).filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate).distinct().count()));
        summary.put("lateCount", countClockStatus(clocks, 2));
        summary.put("earlyCount", countClockStatus(clocks, 3));
        summary.put("misscardCount", countClockStatus(clocks, 4));
        summary.put("absenteeismDays", BigDecimal.valueOf(countClockStatus(clocks, 5)));
        summary.put("leaveDays", leaves.stream()
                .map(l -> l.getDay() != null ? l.getDay() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("dailyDetails", Collections.emptyList());
        result.put("leaves", BeanUtils.toBean(leaves, HrmPortalAttendanceLeaveVO.class));
        return result;
    }

    @Override
    public List<HrmPortalAttendanceMonthExportExcelVO> getAttendanceMonthExportList(Integer year, Integer month) {
        HrEmployeeDO employee = resolveLoginEmployee();
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
        LocalDateTime rangeStart = monthStart.atStartOfDay();
        LocalDateTime rangeEnd = monthEnd.atTime(LocalTime.MAX);
        List<HrmAttendanceClockDO> clocks = hrmAttendanceClockMapper.selectList(
                new LambdaQueryWrapperX<HrmAttendanceClockDO>()
                        .eq(HrmAttendanceClockDO::getEmployeeId, employee.getId())
                        .ge(HrmAttendanceClockDO::getAttendanceTime, rangeStart)
                        .le(HrmAttendanceClockDO::getAttendanceTime, rangeEnd));
        List<HrmAttendanceLeaveDO> leaves = hrmAttendanceLeaveMapper.selectList(
                new LambdaQueryWrapperX<HrmAttendanceLeaveDO>()
                        .eq(HrmAttendanceLeaveDO::getEmployeeId, employee.getId())
                        .ge(HrmAttendanceLeaveDO::getStartTime, rangeStart)
                        .le(HrmAttendanceLeaveDO::getStartTime, rangeEnd));
        HrmPortalAttendanceMonthExportExcelVO row = new HrmPortalAttendanceMonthExportExcelVO();
        row.setEmployeeName(employee.getName());
        row.setYear(year);
        row.setMonth(month);
        row.setActualDays(BigDecimal.valueOf(clocks.stream()
                .map(HrmAttendanceClockDO::getAttendanceTime).filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate).distinct().count()));
        row.setLeaveDays(leaves.stream()
                .map(l -> l.getDay() != null ? l.getDay() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return Collections.singletonList(row);
    }

    // ========== 社保 ==========

    @Override
    public List<HrmPortalInsuranceRecordVO> getInsuranceRecordList(Integer year) {
        HrEmployeeDO employee = resolveLoginEmployee();
        HrmInsuranceEmployeeInfoDO info = hrmInsuranceEmployeeInfoMapper.selectByEmployeeId(employee.getId());
        if (info == null || info.getSchemeId() == null) {
            return Collections.emptyList();
        }
        HrmInsuranceSchemeDO scheme = hrmInsuranceSchemeMapper.selectById(info.getSchemeId());
        if (scheme == null) {
            return Collections.emptyList();
        }
        int targetYear = year != null ? year : LocalDate.now().getYear();
        // 参保起缴月份（无则取目标年 1 月）
        YearMonth start = info.getSocialSecurityStartMonth() != null
                ? YearMonth.from(info.getSocialSecurityStartMonth()) : YearMonth.of(targetYear, 1);
        YearMonth now = YearMonth.now();
        List<HrmPortalInsuranceRecordVO> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(targetYear, m);
            if (ym.isBefore(start) || ym.isAfter(now)) {
                continue;
            }
            result.add(buildInsuranceRecord(employee.getId(), scheme, ym.getYear(), m, false));
        }
        return result;
    }

    @Override
    public HrmPortalInsuranceRecordVO getInsuranceRecord(Integer year, Integer month) {
        HrEmployeeDO employee = resolveLoginEmployee();
        HrmInsuranceEmployeeInfoDO info = hrmInsuranceEmployeeInfoMapper.selectByEmployeeId(employee.getId());
        if (info == null || info.getSchemeId() == null) {
            return null;
        }
        HrmInsuranceSchemeDO scheme = hrmInsuranceSchemeMapper.selectById(info.getSchemeId());
        if (scheme == null) {
            return null;
        }
        return buildInsuranceRecord(employee.getId(), scheme, year, month, true);
    }

    private HrmPortalInsuranceRecordVO buildInsuranceRecord(Long employeeId, HrmInsuranceSchemeDO scheme,
                                                            Integer year, Integer month, boolean withProjects) {
        HrmPortalInsuranceRecordVO vo = new HrmPortalInsuranceRecordVO();
        vo.setId(year * 100L + month);
        vo.setEmployeeId(employeeId);
        vo.setSchemeId(scheme.getId());
        vo.setSchemeName(scheme.getName());
        vo.setSchemeType(scheme.getType());
        vo.setYear(year);
        vo.setMonth(month);
        vo.setPersonalInsuranceAmount(scheme.getPersonalInsuranceAmount());
        vo.setCorporateInsuranceAmount(scheme.getCorporateInsuranceAmount());
        vo.setPersonalProvidentFundAmount(scheme.getPersonalProvidentFundAmount());
        vo.setCorporateProvidentFundAmount(scheme.getCorporateProvidentFundAmount());
        if (withProjects) {
            List<HrmInsuranceSchemeProjectDO> projects = hrmInsuranceSchemeProjectMapper
                    .selectListBySchemeId(scheme.getId());
            vo.setProjectList(projects.stream().map(p -> {
                HrmPortalInsuranceProjectVO project = new HrmPortalInsuranceProjectVO();
                project.setSchemeProjectId(p.getId());
                project.setType(p.getType());
                project.setName(p.getName());
                project.setBaseAmount(p.getBaseAmount());
                project.setCorporateRate(p.getCorporateRate());
                project.setPersonalRate(p.getPersonalRate());
                project.setCorporateAmount(p.getCorporateAmount());
                project.setPersonalAmount(p.getPersonalAmount());
                return project;
            }).collect(Collectors.toList()));
        }
        return vo;
    }

    // ========== 绩效 ==========

    @Override
    public PageResult<HrmPortalPerformanceAssessmentVO> getAssessmentPage(HrmPortalPerformanceAssessmentPageReqVO reqVO) {
        HrEmployeeDO employee = resolveLoginEmployee();
        List<HrmPerformanceAssessmentDO> assessments = loadEmployeeAssessments(employee.getId());
        List<HrmPortalPerformanceAssessmentVO> voList = enrichAssessments(assessments);
        voList = voList.stream()
                .filter(vo -> reqVO.getPlanId() == null || reqVO.getPlanId().equals(vo.getPlanId()))
                .filter(vo -> reqVO.getStageType() == null || reqVO.getStageType().equals(vo.getStageType()))
                .filter(vo -> reqVO.getStatus() == null || reqVO.getStatus().equals(vo.getStatus()))
                .filter(vo -> StrUtil.isEmpty(reqVO.getSearch())
                        || StrUtil.containsIgnoreCase(vo.getName(), reqVO.getSearch()))
                .collect(Collectors.toList());
        return paginate(voList, reqVO);
    }

    @Override
    public HrmPortalPerformanceTaskCountVO getAssessmentTaskCount(String search) {
        HrEmployeeDO employee = resolveLoginEmployee();
        List<HrmPerformanceAssessmentDO> assessments = filterBySearch(loadEmployeeAssessments(employee.getId()), search);
        HrmPortalPerformanceTaskCountVO vo = new HrmPortalPerformanceTaskCountVO();
        vo.setFillPendingCount(countByStage(assessments, STAGE_FILL));
        vo.setFillCompletedCount(countPassedStage(assessments, STAGE_FILL));
        vo.setTargetPendingCount(countByStage(assessments, STAGE_TARGET_CONFIRM));
        vo.setTargetCompletedCount(countPassedStage(assessments, STAGE_TARGET_CONFIRM));
        vo.setReviewPendingCount(countByStage(assessments, STAGE_OTHER_SCORE));
        vo.setReviewCompletedCount(countPassedStage(assessments, STAGE_OTHER_SCORE));
        vo.setResultAuditPendingCount(countByStage(assessments, STAGE_RESULT_AUDIT));
        vo.setResultAuditCompletedCount(countPassedStage(assessments, STAGE_RESULT_AUDIT));
        vo.setResultConfirmationPendingCount(countByStage(assessments, STAGE_RESULT_CONFIRM));
        vo.setResultConfirmationCompletedCount(countPassedStage(assessments, STAGE_RESULT_CONFIRM));
        vo.setResultConfirmationAppealedCount(assessments.stream()
                .filter(a -> a.getAppealStatus() != null && a.getAppealStatus() != APPEAL_STATUS_NONE).count());
        vo.setAppealPendingCount(assessments.stream()
                .filter(a -> Integer.valueOf(APPEAL_STATUS_PENDING).equals(a.getAppealStatus())).count());
        vo.setAppealCompletedCount(assessments.stream()
                .filter(a -> Integer.valueOf(APPEAL_STATUS_PASS).equals(a.getAppealStatus())
                        || Integer.valueOf(APPEAL_STATUS_REJECT).equals(a.getAppealStatus())).count());
        return vo;
    }

    @Override
    public HrmPortalPerformanceAssessmentVO getAssessment(Long id, Long stageId) {
        HrEmployeeDO employee = resolveLoginEmployee();
        HrmPerformanceAssessmentDO assessment = validateOwnAssessment(id, employee.getId());
        HrmPortalPerformanceAssessmentVO vo = enrichAssessments(Collections.singletonList(assessment)).get(0);
        // 明细：维度 + 指标 + 阶段
        vo.setDimensions(buildDimensionList(id));
        vo.setQuotas(buildQuotaList(id));
        List<HrmPerformanceAssessmentStageVO> stages = buildStageList(id);
        vo.setStages(stages);
        List<HrmPerformanceAssessmentStageVO> reviewStages = stages.stream()
                .filter(s -> s.getType() != null
                        && (s.getType() == STAGE_SELF_SCORE || s.getType() == STAGE_OTHER_SCORE))
                .collect(Collectors.toList());
        vo.setReviewStages(reviewStages);
        vo.setCurrentStage(stages.stream()
                .filter(s -> Integer.valueOf(STAGE_STATUS_PENDING).equals(s.getStatus())).findFirst().orElse(null));
        vo.setCurrentReviewStage(reviewStages.stream()
                .filter(s -> Integer.valueOf(STAGE_STATUS_PENDING).equals(s.getStatus())).findFirst()
                .orElse(reviewStages.isEmpty() ? null : reviewStages.get(reviewStages.size() - 1)));
        return vo;
    }

    @Override
    public List<HrmPortalPerformanceProcessRecordVO> getProcessRecordList(Long id, Long stageId) {
        HrEmployeeDO employee = resolveLoginEmployee();
        validateOwnAssessment(id, employee.getId());
        List<HrmPerformanceProcessRecordDO> records = hrmPerformanceProcessRecordMapper
                .selectListByBiz(BIZ_TYPE_ASSESSMENT, id);
        List<HrmPortalPerformanceProcessRecordVO> voList =
                BeanUtils.toBean(records, HrmPortalPerformanceProcessRecordVO.class);
        Set<Long> operatorIds = records.stream().map(HrmPerformanceProcessRecordDO::getOperatorEmployeeId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> nameMap = operatorIds.isEmpty() ? Collections.emptyMap()
                : hrmEmployeeMapper.selectByIds(operatorIds).stream()
                        .collect(Collectors.toMap(HrEmployeeDO::getId, HrEmployeeDO::getName, (a, b) -> a));
        for (int i = 0; i < records.size(); i++) {
            voList.get(i).setOperatorName(nameMap.get(records.get(i).getOperatorEmployeeId()));
        }
        return voList;
    }

    @Override
    public PageResult<HrmPortalPerformanceAssessmentVO> getFillQuotaTaskPage(HrmPortalPerformanceAssessmentPageReqVO reqVO) {
        return taskPage(reqVO, STAGE_FILL);
    }

    @Override
    public PageResult<HrmPortalPerformanceAssessmentVO> getTargetConfirmationTaskPage(HrmPortalPerformanceAssessmentPageReqVO reqVO) {
        return taskPage(reqVO, STAGE_TARGET_CONFIRM);
    }

    @Override
    public PageResult<HrmPortalPerformanceAssessmentVO> getReviewTaskPage(HrmPortalPerformanceAssessmentPageReqVO reqVO) {
        return taskPage(reqVO, STAGE_OTHER_SCORE);
    }

    @Override
    public PageResult<HrmPortalPerformanceAssessmentVO> getResultAuditTaskPage(HrmPortalPerformanceAssessmentPageReqVO reqVO) {
        return taskPage(reqVO, STAGE_RESULT_AUDIT);
    }

    @Override
    public PageResult<HrmPortalPerformanceAssessmentVO> getResultConfirmationTaskPage(HrmPortalPerformanceAssessmentPageReqVO reqVO) {
        return taskPage(reqVO, STAGE_RESULT_CONFIRM);
    }

    @Override
    public PageResult<HrmPortalPerformanceAssessmentVO> getAppealTaskPage(HrmPortalPerformanceAssessmentPageReqVO reqVO) {
        return taskPage(reqVO, STAGE_APPEAL_CONFIRM);
    }

    /**
     * 绩效任务分页：按阶段 + 任务状态过滤
     */
    private PageResult<HrmPortalPerformanceAssessmentVO> taskPage(HrmPortalPerformanceAssessmentPageReqVO reqVO, int stage) {
        HrEmployeeDO employee = resolveLoginEmployee();
        List<HrmPerformanceAssessmentDO> assessments = filterBySearch(
                loadEmployeeAssessments(employee.getId()), reqVO.getSearch());
        Integer taskStatus = reqVO.getStageStatus();
        if (stage == STAGE_APPEAL_CONFIRM) {
            // 申诉任务以申诉状态判定
            if (Integer.valueOf(TASK_STATUS_PENDING).equals(taskStatus)) {
                assessments = assessments.stream()
                        .filter(a -> Integer.valueOf(APPEAL_STATUS_PENDING).equals(a.getAppealStatus()))
                        .collect(Collectors.toList());
            } else if (Integer.valueOf(TASK_STATUS_PROCESSED).equals(taskStatus)) {
                assessments = assessments.stream()
                        .filter(a -> Integer.valueOf(APPEAL_STATUS_PASS).equals(a.getAppealStatus())
                                || Integer.valueOf(APPEAL_STATUS_REJECT).equals(a.getAppealStatus()))
                        .collect(Collectors.toList());
            }
        } else if (Integer.valueOf(TASK_STATUS_PROCESSED).equals(taskStatus)) {
            assessments = assessments.stream()
                    .filter(a -> a.getStageType() != null && a.getStageType() > stage)
                    .collect(Collectors.toList());
        } else if (Integer.valueOf(TASK_STATUS_APPEALED).equals(taskStatus)) {
            assessments = assessments.stream()
                    .filter(a -> a.getAppealStatus() != null && a.getAppealStatus() != APPEAL_STATUS_NONE)
                    .collect(Collectors.toList());
        } else {
            // 默认待处理
            assessments = assessments.stream()
                    .filter(a -> Integer.valueOf(stage).equals(a.getStageType()))
                    .collect(Collectors.toList());
        }
        List<HrmPortalPerformanceAssessmentVO> voList = enrichAssessments(assessments);
        return paginate(voList, reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void fillQuota(HrmPortalPerformanceQuotaFillReqVO reqVO) {
        HrEmployeeDO employee = resolveLoginEmployee();
        HrmPerformanceAssessmentDO assessment = validateOwnAssessment(reqVO.getAssessmentId(), employee.getId());
        List<HrmPerformanceAssessmentQuotaDO> quotas = hrmPerformanceAssessmentQuotaMapper
                .selectListByAssessmentId(assessment.getId());
        Map<Long, HrmPortalPerformanceQuotaValueVO> valueMap = toQuotaValueMap(reqVO.getQuotas());
        for (HrmPerformanceAssessmentQuotaDO quota : quotas) {
            HrmPortalPerformanceQuotaValueVO value = valueMap.get(quota.getId());
            if (value == null) {
                continue;
            }
            HrmPerformanceAssessmentQuotaDO update = new HrmPerformanceAssessmentQuotaDO();
            update.setId(quota.getId());
            update.setTargetValue(value.getTargetValue());
            update.setActualValue(value.getActualValue());
            update.setSelfScore(value.getSelfScore());
            update.setComment(value.getComment());
            hrmPerformanceAssessmentQuotaMapper.updateById(update);
        }
        // 填写完成 → 推进阶段：开启目标确认则进入目标确认，否则进入评分
        HrmPerformancePlanDO plan = hrmPerformancePlanMapper.selectById(assessment.getPlanId());
        int next = plan != null && Boolean.TRUE.equals(plan.getTargetConfirmation())
                ? STAGE_TARGET_CONFIRM : STAGE_OTHER_SCORE;
        updateAssessmentStage(assessment.getId(), next);
        recordProcess(assessment.getId(), employee.getId(), "填写绩效指标", "员工提交绩效指标", "STAGE");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmTarget(HrmPortalPerformanceConfirmReqVO reqVO) {
        HrEmployeeDO employee = resolveLoginEmployee();
        HrmPerformanceAssessmentDO assessment = validateOwnAssessment(reqVO.getAssessmentId(), employee.getId());
        boolean pass = reqVO.getPass() != null && reqVO.getPass() == 1;
        hrmPerformanceAssessmentMapper.update(null, new LambdaUpdateWrapper<HrmPerformanceAssessmentDO>()
                .eq(HrmPerformanceAssessmentDO::getId, assessment.getId())
                .set(HrmPerformanceAssessmentDO::getTargetConfirmationResult, pass ? 1 : 0)
                .set(HrmPerformanceAssessmentDO::getTargetConfirmationComment, reqVO.getComment())
                .set(HrmPerformanceAssessmentDO::getTargetConfirmationTime, LocalDateTime.now())
                .set(HrmPerformanceAssessmentDO::getStageType, pass ? STAGE_OTHER_SCORE : STAGE_FILL));
        recordProcess(assessment.getId(), employee.getId(),
                pass ? "确认绩效目标" : "退回绩效目标", reqVO.getComment(), "STAGE");
    }

    @Override
    public Object previewScore(HrmPortalPerformanceScoreReqVO reqVO) {
        HrEmployeeDO employee = resolveLoginEmployee();
        HrmPerformanceAssessmentDO assessment = validateOwnAssessment(reqVO.getAssessmentId(), employee.getId());
        List<HrmPerformanceAssessmentQuotaDO> quotas = hrmPerformanceAssessmentQuotaMapper
                .selectListByAssessmentId(assessment.getId());
        BigDecimal score = calcWeightedScore(quotas, reqVO.getQuotaScores());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("score", score);
        result.put("stageScore", score);
        result.put("cumulativeScore", score);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void score(HrmPortalPerformanceScoreReqVO reqVO) {
        HrEmployeeDO employee = resolveLoginEmployee();
        HrmPerformanceAssessmentDO assessment = validateOwnAssessment(reqVO.getAssessmentId(), employee.getId());
        List<HrmPerformanceAssessmentQuotaDO> quotas = hrmPerformanceAssessmentQuotaMapper
                .selectListByAssessmentId(assessment.getId());
        Map<Long, HrmPortalPerformanceQuotaValueVO> valueMap = toQuotaValueMap(reqVO.getQuotaScores());
        for (HrmPerformanceAssessmentQuotaDO quota : quotas) {
            HrmPortalPerformanceQuotaValueVO value = valueMap.get(quota.getId());
            if (value == null) {
                continue;
            }
            HrmPerformanceAssessmentQuotaDO update = new HrmPerformanceAssessmentQuotaDO();
            update.setId(quota.getId());
            update.setActualValue(value.getActualValue());
            update.setReviewerScore(value.getSelfScore());
            update.setFinalScore(value.getSelfScore());
            update.setComment(value.getComment());
            hrmPerformanceAssessmentQuotaMapper.updateById(update);
        }
        // 重新读取以计算加权总分
        quotas = hrmPerformanceAssessmentQuotaMapper.selectListByAssessmentId(assessment.getId());
        BigDecimal total = calcWeightedScore(quotas, reqVO.getQuotaScores());
        // 推进阶段：结果审核 → 结果确认 → 归档（按计划开关）
        HrmPerformancePlanDO plan = hrmPerformancePlanMapper.selectById(assessment.getPlanId());
        int next = STAGE_ARCHIVED;
        if (plan != null) {
            if (Boolean.TRUE.equals(plan.getResultAudit())) {
                next = STAGE_RESULT_AUDIT;
            } else if (Boolean.TRUE.equals(plan.getResultConfirmation())) {
                next = STAGE_RESULT_CONFIRM;
            }
        }
        hrmPerformanceAssessmentMapper.update(null, new LambdaUpdateWrapper<HrmPerformanceAssessmentDO>()
                .eq(HrmPerformanceAssessmentDO::getId, assessment.getId())
                .set(HrmPerformanceAssessmentDO::getScore, total)
                .set(HrmPerformanceAssessmentDO::getReviewerComment, reqVO.getComment())
                .set(HrmPerformanceAssessmentDO::getStageType, next));
        recordProcess(assessment.getId(), employee.getId(), "提交绩效评分", reqVO.getComment(), "STAGE");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectReviewStage(HrmPortalPerformanceRejectReqVO reqVO) {
        HrEmployeeDO employee = resolveLoginEmployee();
        HrmPerformanceAssessmentDO assessment = validateOwnAssessment(reqVO.getAssessmentId(), employee.getId());
        List<HrmPerformanceAssessmentStageDO> stages = hrmPerformanceAssessmentStageMapper
                .selectListByAssessmentId(assessment.getId());
        // 将当前待处理评分阶段标记驳回，并回退至上一评分阶段
        HrmPerformanceAssessmentStageDO current = stages.stream()
                .filter(s -> Integer.valueOf(STAGE_STATUS_PENDING).equals(s.getStatus()))
                .findFirst().orElse(null);
        if (current != null) {
            hrmPerformanceAssessmentStageMapper.update(null, new LambdaUpdateWrapper<HrmPerformanceAssessmentStageDO>()
                    .eq(HrmPerformanceAssessmentStageDO::getId, current.getId())
                    .set(HrmPerformanceAssessmentStageDO::getStatus, STAGE_STATUS_REJECTED)
                    .set(HrmPerformanceAssessmentStageDO::getRejectReason, reqVO.getReason()));
        }
        updateAssessmentStage(assessment.getId(), STAGE_OTHER_SCORE);
        recordProcess(assessment.getId(), employee.getId(), "驳回绩效评分阶段", reqVO.getReason(), "STAGE");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleResultAudit(HrmPortalPerformanceHandleStageReqVO reqVO) {
        HrEmployeeDO employee = resolveLoginEmployee();
        HrmPerformanceAssessmentDO assessment = validateOwnAssessment(reqVO.getId(), employee.getId());
        boolean pass = reqVO.getPass() != null && reqVO.getPass() == 1;
        // 结果审核结果落 result_comment（本域无独立结果审核列），并推进阶段
        HrmPerformancePlanDO plan = hrmPerformancePlanMapper.selectById(assessment.getPlanId());
        int next = pass
                ? (plan != null && Boolean.TRUE.equals(plan.getResultConfirmation()) ? STAGE_RESULT_CONFIRM : STAGE_ARCHIVED)
                : STAGE_OTHER_SCORE;
        hrmPerformanceAssessmentMapper.update(null, new LambdaUpdateWrapper<HrmPerformanceAssessmentDO>()
                .eq(HrmPerformanceAssessmentDO::getId, assessment.getId())
                .set(HrmPerformanceAssessmentDO::getResultComment, reqVO.getComment())
                .set(HrmPerformanceAssessmentDO::getStageType, next));
        recordProcess(assessment.getId(), employee.getId(),
                pass ? "结果审核通过" : "结果审核驳回", reqVO.getComment(), "STAGE");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmResult(HrmPortalPerformanceConfirmReqVO reqVO) {
        HrEmployeeDO employee = resolveLoginEmployee();
        HrmPerformanceAssessmentDO assessment = validateOwnAssessment(reqVO.getAssessmentId(), employee.getId());
        boolean pass = reqVO.getPass() != null && reqVO.getPass() == 1;
        if (pass) {
            hrmPerformanceAssessmentMapper.update(null, new LambdaUpdateWrapper<HrmPerformanceAssessmentDO>()
                    .eq(HrmPerformanceAssessmentDO::getId, assessment.getId())
                    .set(HrmPerformanceAssessmentDO::getResultComment, reqVO.getComment())
                    .set(HrmPerformanceAssessmentDO::getStageType, STAGE_ARCHIVED));
        } else {
            // 驳回结果 → 申诉待处理
            hrmPerformanceAssessmentMapper.update(null, new LambdaUpdateWrapper<HrmPerformanceAssessmentDO>()
                    .eq(HrmPerformanceAssessmentDO::getId, assessment.getId())
                    .set(HrmPerformanceAssessmentDO::getAppealStatus, APPEAL_STATUS_PENDING));
        }
        recordProcess(assessment.getId(), employee.getId(),
                pass ? "确认绩效结果" : "驳回绩效结果", reqVO.getComment(), "STAGE");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitAppeal(HrmPortalPerformanceAppealReqVO reqVO) {
        HrEmployeeDO employee = resolveLoginEmployee();
        HrmPerformanceAssessmentDO assessment = validateOwnAssessment(reqVO.getAssessmentId(), employee.getId());
        // json 列必须走 DO + updateById（JacksonTypeHandler 生效）；LambdaUpdateWrapper.set() 绕开 typeHandler
        HrmPerformanceAssessmentDO updateObj = new HrmPerformanceAssessmentDO();
        updateObj.setId(assessment.getId());
        updateObj.setAppealReason(reqVO.getAppealReason());
        updateObj.setAppealStatus(APPEAL_STATUS_PENDING);
        updateObj.setStageType(STAGE_APPEAL_CONFIRM);
        updateObj.setAppealFileUrls(reqVO.getAppealFileUrls());
        hrmPerformanceAssessmentMapper.updateById(updateObj);
        recordProcess(assessment.getId(), employee.getId(), "提交绩效申诉", reqVO.getAppealReason(), "STAGE");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleAppeal(HrmPortalPerformanceHandleStageReqVO reqVO) {
        HrEmployeeDO employee = resolveLoginEmployee();
        HrmPerformanceAssessmentDO assessment = validateOwnAssessment(reqVO.getId(), employee.getId());
        boolean pass = reqVO.getPass() != null && reqVO.getPass() == 1;
        hrmPerformanceAssessmentMapper.update(null, new LambdaUpdateWrapper<HrmPerformanceAssessmentDO>()
                .eq(HrmPerformanceAssessmentDO::getId, assessment.getId())
                .set(HrmPerformanceAssessmentDO::getAppealStatus, pass ? APPEAL_STATUS_PASS : APPEAL_STATUS_REJECT)
                .set(HrmPerformanceAssessmentDO::getResultComment, reqVO.getComment())
                .set(HrmPerformanceAssessmentDO::getStageType, pass ? STAGE_OTHER_SCORE : STAGE_ARCHIVED));
        recordProcess(assessment.getId(), employee.getId(),
                pass ? "绩效申诉通过" : "绩效申诉驳回", reqVO.getComment(), "STAGE");
    }

    // ========== 工资条 ==========

    @Override
    public PageResult<HrmPortalSalarySlipVO> getSalarySlipPage(Integer pageNo, Integer pageSize,
                                                               String startMonth, String endMonth) {
        HrEmployeeDO employee = resolveLoginEmployee();
        PageParam pageParam = new PageParam();
        pageParam.setPageNo(pageNo == null || pageNo < 1 ? 1 : pageNo);
        pageParam.setPageSize(pageSize == null ? 10 : pageSize);
        LambdaQueryWrapperX<HrmSalarySlipDO> wrapper = new LambdaQueryWrapperX<HrmSalarySlipDO>()
                .eq(HrmSalarySlipDO::getEmployeeId, employee.getId());
        YearMonth start = parseYearMonth(startMonth);
        YearMonth end = parseYearMonth(endMonth);
        if (start != null) {
            wrapper.and(w -> w.gt(HrmSalarySlipDO::getYear, start.getYear())
                    .or().eq(HrmSalarySlipDO::getYear, start.getYear())
                    .ge(HrmSalarySlipDO::getMonth, start.getMonthValue()));
        }
        if (end != null) {
            wrapper.and(w -> w.lt(HrmSalarySlipDO::getYear, end.getYear())
                    .or().eq(HrmSalarySlipDO::getYear, end.getYear())
                    .le(HrmSalarySlipDO::getMonth, end.getMonthValue()));
        }
        wrapper.orderByDesc(HrmSalarySlipDO::getYear).orderByDesc(HrmSalarySlipDO::getMonth)
                .orderByDesc(HrmSalarySlipDO::getId);
        PageResult<HrmSalarySlipDO> pageResult = hrmSalarySlipMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(buildSlipVOs(pageResult.getList()), pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void readSalarySlip(Long id) {
        HrEmployeeDO employee = resolveLoginEmployee();
        HrmSalarySlipDO slip = hrmSalarySlipMapper.selectById(id);
        if (slip == null) {
            throw exception(HRM_SALARY_SLIP_NOT_EXISTS);
        }
        if (!slip.getEmployeeId().equals(employee.getId())) {
            throw exception(HRM_PORTAL_SALARY_SLIP_NOT_OWN);
        }
        hrmSalarySlipMapper.update(null, new LambdaUpdateWrapper<HrmSalarySlipDO>()
                .eq(HrmSalarySlipDO::getId, id)
                .set(HrmSalarySlipDO::getReadStatus, 1));
    }

    @Override
    public HrmPortalSalarySlipUnreadSummaryVO getSalarySlipUnreadSummary() {
        HrEmployeeDO employee = resolveLoginEmployee();
        List<HrmSalarySlipDO> slips = hrmSalarySlipMapper.selectList(
                new LambdaQueryWrapperX<HrmSalarySlipDO>()
                        .eq(HrmSalarySlipDO::getEmployeeId, employee.getId())
                        .eq(HrmSalarySlipDO::getReadStatus, 0));
        HrmPortalSalarySlipUnreadSummaryVO vo = new HrmPortalSalarySlipUnreadSummaryVO();
        vo.setUnreadCount((long) slips.size());
        slips.stream().max(Comparator.comparing(s -> (s.getYear() == null ? 0 : s.getYear()) * 100
                        + (s.getMonth() == null ? 0 : s.getMonth())))
                .ifPresent(s -> {
                    vo.setLatestYear(s.getYear());
                    vo.setLatestMonth(s.getMonth());
                });
        return vo;
    }

    private List<HrmPortalSalarySlipVO> buildSlipVOs(List<HrmSalarySlipDO> slips) {
        if (CollUtil.isEmpty(slips)) {
            return Collections.emptyList();
        }
        List<HrmPortalSalarySlipVO> result = new ArrayList<>();
        for (HrmSalarySlipDO slip : slips) {
            HrmPortalSalarySlipVO vo = BeanUtils.toBean(slip, HrmPortalSalarySlipVO.class);
            if (slip.getMonthEmployeeRecordId() != null) {
                HrmSalaryMonthEmployeeRecordDO record = hrmSalaryMonthEmployeeRecordMapper
                        .selectById(slip.getMonthEmployeeRecordId());
                if (record != null) {
                    vo.setRealPaySalary(record.getRealPaySalary());
                    vo.setOptions(toSlipOptions(record.getOptionValues()));
                }
            }
            result.add(vo);
        }
        return result;
    }

    private List<HrmPortalSalarySlipOptionVO> toSlipOptions(List<Map<String, Object>> maps) {
        if (maps == null) {
            return Collections.emptyList();
        }
        List<HrmPortalSalarySlipOptionVO> list = new ArrayList<>();
        for (Map<String, Object> map : maps) {
            HrmPortalSalarySlipOptionVO vo = new HrmPortalSalarySlipOptionVO();
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

    // ========== 首页日历 ==========

    @Override
    public List<HrmPortalCalendarItemVO> getHomeCalendar(String startDate, String endDate) {
        HrEmployeeDO employee = resolveLoginEmployee();
        LocalDateTime rangeStart = parseDateStart(startDate);
        LocalDateTime rangeEnd = parseDateEnd(endDate);
        List<HrmPortalCalendarItemVO> items = new ArrayList<>();
        // 个人备忘（当前登录用户）
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId != null) {
            hrmEmployeePersonalNoteMapper.selectListByUserId(userId).forEach(note -> {
                if (inRange(note.getReminderTime(), rangeStart, rangeEnd)) {
                    items.add(buildCalendarItem(note.getId(), 1, "备忘", note.getContent(), null,
                            note.getReminderTime()));
                }
            });
        }
        // 本人事项：入职/转正/离职
        if (inRange(employee.getEntryTime(), rangeStart, rangeEnd)) {
            items.add(buildCalendarItem(null, 3, "入职", employee.getName() + " 入职",
                    employee.getId(), employee.getEntryTime()));
        }
        if (inRange(employee.getRegularTime(), rangeStart, rangeEnd)) {
            items.add(buildCalendarItem(null, 4, "转正", employee.getName() + " 转正",
                    employee.getId(), employee.getRegularTime()));
        }
        if (inRange(employee.getLeaveTime(), rangeStart, rangeEnd)) {
            items.add(buildCalendarItem(null, 5, "离职", employee.getName() + " 离职",
                    employee.getId(), employee.getLeaveTime()));
        }
        items.sort(Comparator.comparing(HrmPortalCalendarItemVO::getEventTime,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return items;
    }

    // ========== 员工子档案 ==========

    @Override
    public List<HrmEmployeeCertificateVO> getCertificateList() {
        HrEmployeeDO employee = resolveLoginEmployee();
        return BeanUtils.toBean(hrmEmployeeCertificateMapper.selectListByEmployeeId(employee.getId()),
                HrmEmployeeCertificateVO.class);
    }

    @Override
    public List<HrmEmployeeContactVO> getContactList() {
        HrEmployeeDO employee = resolveLoginEmployee();
        return BeanUtils.toBean(hrmEmployeeContactMapper.selectListByEmployeeId(employee.getId()),
                HrmEmployeeContactVO.class);
    }

    @Override
    public List<HrmEmployeeEducationExperienceVO> getEducationExperienceList() {
        HrEmployeeDO employee = resolveLoginEmployee();
        return BeanUtils.toBean(hrmEmployeeEducationExperienceMapper.selectListByEmployeeId(employee.getId()),
                HrmEmployeeEducationExperienceVO.class);
    }

    @Override
    public List<HrmEmployeeWorkExperienceVO> getWorkExperienceList() {
        HrEmployeeDO employee = resolveLoginEmployee();
        return BeanUtils.toBean(hrmEmployeeWorkExperienceMapper.selectListByEmployeeId(employee.getId()),
                HrmEmployeeWorkExperienceVO.class);
    }

    @Override
    public List<HrmEmployeeTrainingExperienceVO> getTrainingExperienceList() {
        HrEmployeeDO employee = resolveLoginEmployee();
        return BeanUtils.toBean(hrmEmployeeTrainingExperienceMapper.selectListByEmployeeId(employee.getId()),
                HrmEmployeeTrainingExperienceVO.class);
    }

    @Override
    public HrmEmployeeQuitInfoRespVO getQuitInfo() {
        HrEmployeeDO employee = resolveLoginEmployee();
        HrmEmployeeQuitInfoDO quitInfo = hrmEmployeeQuitInfoMapper.selectByEmployeeId(employee.getId());
        return BeanUtils.toBean(quitInfo, HrmEmployeeQuitInfoRespVO.class);
    }

    @Override
    public List<HrmEmployeeFieldConfigRespVO> getFieldConfigList() {
        resolveLoginEmployee();
        List<HrmEmployeeFieldConfigDO> list = hrmEmployeeFieldConfigMapper
                .selectListByKind(HrmEmployeeFieldConfigDO.KIND_ARCHIVE);
        return BeanUtils.toBean(list, HrmEmployeeFieldConfigRespVO.class);
    }

    // ========== 私有：员工解析 ==========

    /**
     * 解析当前登录用户绑定的员工；未绑定抛业务异常
     */
    private HrEmployeeDO resolveLoginEmployee() {
        HrEmployeeDO employee = resolveLoginEmployeeOrNull();
        if (employee == null) {
            throw exception(HRM_PORTAL_EMPLOYEE_NOT_BOUND);
        }
        return employee;
    }

    private HrEmployeeDO resolveLoginEmployeeOrNull() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            return null;
        }
        List<HrEmployeeDO> list = hrmEmployeeMapper.selectListByUserIds(Collections.singletonList(userId));
        return CollUtil.isNotEmpty(list) ? list.get(0) : null;
    }

    // ========== 私有：绩效 ==========

    private List<HrmPerformanceAssessmentDO> loadEmployeeAssessments(Long employeeId) {
        return hrmPerformanceAssessmentMapper.selectList(
                new LambdaQueryWrapperX<HrmPerformanceAssessmentDO>()
                        .eq(HrmPerformanceAssessmentDO::getEmployeeId, employeeId)
                        .orderByDesc(HrmPerformanceAssessmentDO::getId));
    }

    private List<HrmPerformanceAssessmentDO> filterBySearch(List<HrmPerformanceAssessmentDO> assessments, String search) {
        if (StrUtil.isEmpty(search)) {
            return assessments;
        }
        Set<Long> planIds = assessments.stream().map(HrmPerformanceAssessmentDO::getPlanId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> planNameMap = planIds.isEmpty() ? Collections.emptyMap()
                : hrmPerformancePlanMapper.selectListByIds(planIds).stream()
                        .collect(Collectors.toMap(HrmPerformancePlanDO::getId, HrmPerformancePlanDO::getName, (a, b) -> a));
        String kw = search.trim();
        return assessments.stream()
                .filter(a -> StrUtil.containsIgnoreCase(planNameMap.get(a.getPlanId()), kw))
                .collect(Collectors.toList());
    }

    private HrmPerformanceAssessmentDO validateOwnAssessment(Long id, Long employeeId) {
        HrmPerformanceAssessmentDO assessment = hrmPerformanceAssessmentMapper.selectById(id);
        if (assessment == null) {
            throw exception(HRM_PERFORMANCE_ASSESSMENT_NOT_EXISTS);
        }
        if (!employeeId.equals(assessment.getEmployeeId())) {
            throw exception(HRM_PORTAL_ASSESSMENT_NOT_OWN);
        }
        return assessment;
    }

    private long countByStage(List<HrmPerformanceAssessmentDO> assessments, int stage) {
        return assessments.stream().filter(a -> Integer.valueOf(stage).equals(a.getStageType())).count();
    }

    private long countPassedStage(List<HrmPerformanceAssessmentDO> assessments, int stage) {
        return assessments.stream()
                .filter(a -> a.getStageType() != null && a.getStageType() > stage).count();
    }

    private List<HrmPortalPerformanceAssessmentVO> enrichAssessments(List<HrmPerformanceAssessmentDO> assessments) {
        if (CollUtil.isEmpty(assessments)) {
            return Collections.emptyList();
        }
        Set<Long> planIds = assessments.stream().map(HrmPerformanceAssessmentDO::getPlanId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, HrmPerformancePlanDO> planMap = planIds.isEmpty() ? Collections.emptyMap()
                : hrmPerformancePlanMapper.selectListByIds(planIds).stream()
                        .collect(Collectors.toMap(HrmPerformancePlanDO::getId, p -> p, (a, b) -> a));
        Set<Long> confirmationEmployeeIds = assessments.stream()
                .map(HrmPerformanceAssessmentDO::getTargetConfirmationEmployeeId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> confirmationNameMap = confirmationEmployeeIds.isEmpty() ? Collections.emptyMap()
                : hrmEmployeeMapper.selectByIds(confirmationEmployeeIds).stream()
                        .collect(Collectors.toMap(HrEmployeeDO::getId, HrEmployeeDO::getName, (a, b) -> a));
        List<HrmPortalPerformanceAssessmentVO> result = new ArrayList<>(assessments.size());
        for (HrmPerformanceAssessmentDO assessment : assessments) {
            HrmPortalPerformanceAssessmentVO vo = new HrmPortalPerformanceAssessmentVO();
            vo.setId(assessment.getId());
            vo.setPlanId(assessment.getPlanId());
            vo.setStatus(assessment.getStatus());
            vo.setStageType(assessment.getStageType());
            vo.setScore(assessment.getScore());
            vo.setResultLevel(assessment.getResultLevel());
            vo.setCoefficient(assessment.getCoefficient());
            vo.setAppealReason(assessment.getAppealReason());
            vo.setAppealStatus(assessment.getAppealStatus());
            vo.setArchiveTime(assessment.getArchiveTime());
            vo.setTargetConfirmationResult(assessment.getTargetConfirmationResult());
            vo.setTargetConfirmationComment(assessment.getTargetConfirmationComment());
            vo.setTargetConfirmationTime(assessment.getTargetConfirmationTime());
            vo.setTargetConfirmationEmployeeName(confirmationNameMap.get(assessment.getTargetConfirmationEmployeeId()));
            HrmPerformancePlanDO plan = planMap.get(assessment.getPlanId());
            if (plan != null) {
                vo.setName(plan.getName());
                vo.setCycleType(plan.getCycleType());
                vo.setCycle(plan.getCycle());
                vo.setStartTime(plan.getStartTime());
                vo.setEndTime(plan.getEndTime());
                if (plan.getAssessmentConfig() != null && plan.getAssessmentConfig().get("upperLimitScore") != null) {
                    vo.setUpperLimitScore(new BigDecimal(plan.getAssessmentConfig().get("upperLimitScore").toString()));
                }
            }
            // 单条评估的阶段计数
            vo.setFillPendingCount(Integer.valueOf(STAGE_FILL).equals(assessment.getStageType()) ? 1L : 0L);
            vo.setFillCompletedCount(assessment.getStageType() != null && assessment.getStageType() > STAGE_FILL ? 1L : 0L);
            vo.setTargetPendingCount(Integer.valueOf(STAGE_TARGET_CONFIRM).equals(assessment.getStageType()) ? 1L : 0L);
            vo.setTargetCompletedCount(assessment.getStageType() != null && assessment.getStageType() > STAGE_TARGET_CONFIRM ? 1L : 0L);
            vo.setReviewPendingCount(Integer.valueOf(STAGE_OTHER_SCORE).equals(assessment.getStageType()) ? 1L : 0L);
            vo.setReviewCompletedCount(assessment.getStageType() != null && assessment.getStageType() > STAGE_OTHER_SCORE ? 1L : 0L);
            vo.setResultAuditPendingCount(Integer.valueOf(STAGE_RESULT_AUDIT).equals(assessment.getStageType()) ? 1L : 0L);
            vo.setResultAuditCompletedCount(assessment.getStageType() != null && assessment.getStageType() > STAGE_RESULT_AUDIT ? 1L : 0L);
            vo.setResultConfirmationPendingCount(Integer.valueOf(STAGE_RESULT_CONFIRM).equals(assessment.getStageType()) ? 1L : 0L);
            vo.setResultConfirmationCompletedCount(assessment.getStageType() != null && assessment.getStageType() > STAGE_RESULT_CONFIRM ? 1L : 0L);
            vo.setResultConfirmationAppealedCount(assessment.getAppealStatus() != null
                    && assessment.getAppealStatus() != APPEAL_STATUS_NONE ? 1L : 0L);
            vo.setAppealPendingCount(Integer.valueOf(APPEAL_STATUS_PENDING).equals(assessment.getAppealStatus()) ? 1L : 0L);
            vo.setAppealCompletedCount(Integer.valueOf(APPEAL_STATUS_PASS).equals(assessment.getAppealStatus())
                    || Integer.valueOf(APPEAL_STATUS_REJECT).equals(assessment.getAppealStatus()) ? 1L : 0L);
            result.add(vo);
        }
        return result;
    }

    private List<HrmPerformanceAssessmentDimensionVO> buildDimensionList(Long assessmentId) {
        List<HrmPerformanceAssessmentDimensionDO> dimensions = hrmPerformanceAssessmentDimensionMapper
                .selectListByAssessmentId(assessmentId);
        return BeanUtils.toBean(dimensions, HrmPerformanceAssessmentDimensionVO.class);
    }

    private List<HrmPerformanceAssessmentQuotaVO> buildQuotaList(Long assessmentId) {
        List<HrmPerformanceAssessmentQuotaDO> quotas = hrmPerformanceAssessmentQuotaMapper
                .selectListByAssessmentId(assessmentId);
        return BeanUtils.toBean(quotas, HrmPerformanceAssessmentQuotaVO.class);
    }

    private List<HrmPerformanceAssessmentStageVO> buildStageList(Long assessmentId) {
        List<HrmPerformanceAssessmentStageDO> stages = hrmPerformanceAssessmentStageMapper
                .selectListByAssessmentId(assessmentId);
        List<HrmPerformanceAssessmentStageVO> voList =
                BeanUtils.toBean(stages, HrmPerformanceAssessmentStageVO.class);
        Set<Long> handlerIds = stages.stream().map(HrmPerformanceAssessmentStageDO::getHandlerEmployeeId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> nameMap = handlerIds.isEmpty() ? Collections.emptyMap()
                : hrmEmployeeMapper.selectByIds(handlerIds).stream()
                        .collect(Collectors.toMap(HrEmployeeDO::getId, HrEmployeeDO::getName, (a, b) -> a));
        for (int i = 0; i < stages.size(); i++) {
            HrmPerformanceAssessmentStageVO vo = voList.get(i);
            vo.setHandlerName(nameMap.get(stages.get(i).getHandlerEmployeeId()));
            vo.setCanHandle(true);
            vo.setCanScore(true);
        }
        return voList;
    }

    private void updateAssessmentStage(Long assessmentId, int stageType) {
        hrmPerformanceAssessmentMapper.update(null, new LambdaUpdateWrapper<HrmPerformanceAssessmentDO>()
                .eq(HrmPerformanceAssessmentDO::getId, assessmentId)
                .set(HrmPerformanceAssessmentDO::getStageType, stageType));
    }

    private void recordProcess(Long assessmentId, Long operatorEmployeeId, String title, String content, String source) {
        HrmPerformanceProcessRecordDO record = HrmPerformanceProcessRecordDO.builder()
                .bizType(BIZ_TYPE_ASSESSMENT)
                .bizId(assessmentId)
                .title(title)
                .content(content)
                .source(source)
                .operatorEmployeeId(operatorEmployeeId)
                .operateTime(LocalDateTime.now())
                .build();
        hrmPerformanceProcessRecordMapper.insert(record);
    }

    private Map<Long, HrmPortalPerformanceQuotaValueVO> toQuotaValueMap(List<HrmPortalPerformanceQuotaValueVO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyMap();
        }
        Map<Long, HrmPortalPerformanceQuotaValueVO> map = new LinkedHashMap<>();
        for (HrmPortalPerformanceQuotaValueVO vo : list) {
            if (vo.getQuotaId() != null) {
                map.put(vo.getQuotaId(), vo);
            }
        }
        return map;
    }

    /**
     * 加权总分 = Σ(指标得分 × 指标权重 / 100)
     */
    private BigDecimal calcWeightedScore(List<HrmPerformanceAssessmentQuotaDO> quotas,
                                         List<HrmPortalPerformanceQuotaValueVO> values) {
        Map<Long, HrmPortalPerformanceQuotaValueVO> valueMap = toQuotaValueMap(values);
        BigDecimal total = BigDecimal.ZERO;
        for (HrmPerformanceAssessmentQuotaDO quota : quotas) {
            HrmPortalPerformanceQuotaValueVO value = valueMap.get(quota.getId());
            BigDecimal score = value != null ? value.getSelfScore() : quota.getFinalScore();
            if (score == null) {
                continue;
            }
            BigDecimal weight = quota.getWeight() != null ? quota.getWeight() : BigDecimal.ZERO;
            total = total.add(score.multiply(weight).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    // ========== 私有：分页 / 工具 ==========

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

    private Integer calcAge(LocalDateTime birthday) {
        if (birthday == null) {
            return null;
        }
        return java.time.Period.between(birthday.toLocalDate(), LocalDate.now()).getYears();
    }

    private Long calcEntryDay(LocalDateTime entryTime) {
        if (entryTime == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(entryTime.toLocalDate(), LocalDate.now()) + 1;
    }

    private Integer calcCompanyAge(LocalDateTime companyAgeStartTime) {
        if (companyAgeStartTime == null) {
            return null;
        }
        return java.time.Period.between(companyAgeStartTime.toLocalDate(), LocalDate.now()).getYears();
    }

    private long countClockStatus(List<HrmAttendanceClockDO> clocks, int status) {
        return clocks.stream().filter(c -> Integer.valueOf(status).equals(c.getStatus())).count();
    }

    private HrmPortalCalendarItemVO buildCalendarItem(Long personalNoteId, int type, String typeName,
                                                      String content, Long typeId, LocalDateTime eventTime) {
        HrmPortalCalendarItemVO item = new HrmPortalCalendarItemVO();
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

    private static LocalDateTime parseDateStart(String date) {
        if (StrUtil.isEmpty(date)) {
            return LocalDate.now().withDayOfMonth(1).atStartOfDay();
        }
        return LocalDate.parse(date.trim(), DATE_FMT).atStartOfDay();
    }

    private static LocalDateTime parseDateEnd(String date) {
        if (StrUtil.isEmpty(date)) {
            LocalDate today = LocalDate.now();
            return today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX);
        }
        return LocalDate.parse(date.trim(), DATE_FMT).atTime(LocalTime.MAX);
    }

    private static YearMonth parseYearMonth(String month) {
        if (StrUtil.isEmpty(month)) {
            return null;
        }
        try {
            return YearMonth.parse(month.trim(), MONTH_FMT);
        } catch (Exception ex) {
            return null;
        }
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

}
