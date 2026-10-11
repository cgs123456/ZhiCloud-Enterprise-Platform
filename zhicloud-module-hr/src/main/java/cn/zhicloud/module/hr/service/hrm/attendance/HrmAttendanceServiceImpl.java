package cn.zhicloud.module.hr.service.hrm.attendance;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.hr.controller.admin.hrm.attendance.vo.*;
import cn.zhicloud.module.hr.dal.dataobject.employee.HrEmployeeDO;
import cn.zhicloud.module.hr.dal.dataobject.hrm.*;
import cn.zhicloud.module.hr.dal.mysql.employee.HrEmployeeMapper;
import cn.zhicloud.module.hr.dal.mysql.hrm.*;
import cn.zhicloud.module.system.api.dept.DeptApi;
import cn.zhicloud.module.system.api.dept.dto.DeptRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.hr.enums.ErrorCodeConstants.*;

/**
 * HRM 考勤 Service 实现类
 *
 * 聚合口径（与前端 views/hrm/attendance 契约一致）：
 * * 打卡状态字典 hrm_attendance_clock_status：1正常 2迟到 3早退 4缺卡 5旷工。
 * * 月度汇总以「打卡行 + 请假行」实时聚合，落 hr_attendance_month_record（(employee,year,month) 唯一）作幂等缓存；
 *   重复请求命中已有行不重算（需要重算时先删缓存行，属运维口径）。
 * * 应出勤天数 = 当月按考勤组班次 weeks 的排班日 − 休息类节假日 + 上班类节假日（无考勤组时回退周一~周五）。
 * * 扣款规则按考勤组的 deduct_rule：迟到/早退 method 1每月固定 2按分钟 3按次数；旷工按天；缺卡按次数。
 *
 * @author zhicloud
 */
@Slf4j
@Service
@Validated
public class HrmAttendanceServiceImpl implements HrmAttendanceService {

    /** 打卡状态（hrm_attendance_clock_status 字典） */
    private static final int CLOCK_STATUS_NORMAL = 1;
    private static final int CLOCK_STATUS_LATE = 2;
    private static final int CLOCK_STATUS_EARLY = 3;
    private static final int CLOCK_STATUS_MISSCARD = 4;
    private static final int CLOCK_STATUS_ABSENTEEISM = 5;
    /** 打卡类型 */
    private static final int CLOCK_TYPE_ON_DUTY = 1;

    @Resource
    private HrEmployeeMapper employeeMapper;
    @Resource
    private HrmAttendanceClockMapper clockMapper;
    @Resource
    private HrmAttendanceGroupMapper groupMapper;
    @Resource
    private HrmAttendanceGroupEmployeeMapper groupEmployeeMapper;
    @Resource
    private HrmAttendanceHolidayMapper holidayMapper;
    @Resource
    private HrmAttendanceLeaveMapper leaveMapper;
    @Resource
    private HrmAttendanceMonthRecordMapper monthRecordMapper;
    @Resource
    private DeptApi deptApi;

    // ========== 打卡 ==========

    @Override
    public PageResult<HrmAttendanceClockRespVO> getClockPage(HrmAttendanceClockPageReqVO reqVO) {
        Collection<Long> employeeIds = resolveEmployeeIds(reqVO.getDeptIds(), reqVO.getSearch());
        if (employeeIds != null && employeeIds.isEmpty()) {
            return PageResult.empty();
        }
        PageResult<HrmAttendanceClockDO> pageResult = clockMapper.selectPage(reqVO, employeeIds);
        return new PageResult<>(enrichClocks(pageResult.getList()), pageResult.getTotal());
    }

    @Override
    public HrmAttendanceClockRespVO getClock(Long id) {
        HrmAttendanceClockDO clock = validateClockExists(id);
        return enrichClocks(Collections.singletonList(clock)).get(0);
    }

    @Override
    public HrmAttendanceClockShiftRespVO getClockShift(Long employeeId, String attendanceTime) {
        validateEmployeeExists(employeeId);
        LocalDate date = parseDate(attendanceTime);
        HrmAttendanceGroupDO group = resolveEmployeeGroup(employeeId);
        if (group == null) {
            return null;
        }
        Map<String, Object> shift = matchShiftForDate(group, date);
        if (shift == null) {
            return null;
        }
        HrmAttendanceClockShiftRespVO resp = new HrmAttendanceClockShiftRespVO();
        resp.setStartTime(atTime(date, str(shift.get("startTime"))));
        resp.setEndTime(atTime(date, str(shift.get("endTime"))));
        resp.setClockInStartTime(atTime(date, str(shift.get("clockInStartTime"))));
        resp.setClockInEndTime(atTime(date, str(shift.get("clockInEndTime"))));
        resp.setClockOutStartTime(atTime(date, str(shift.get("clockOutStartTime"))));
        resp.setClockOutEndTime(atTime(date, str(shift.get("clockOutEndTime"))));
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createClock(HrmAttendanceClockSaveReqVO reqVO) {
        validateEmployeeExists(reqVO.getEmployeeId());
        HrmAttendanceClockDO clock = BeanUtils.toBean(reqVO, HrmAttendanceClockDO.class);
        if (clock.getSourceType() == null) {
            clock.setSourceType(2); // 手工录入
        }
        if (clock.getStatus() == null) {
            clock.setStatus(CLOCK_STATUS_NORMAL);
        }
        clockMapper.insert(clock);
        // 打卡落库后失效当月汇总缓存，下次查询重算
        invalidateMonthRecord(reqVO.getEmployeeId(), reqVO.getAttendanceTime());
        return clock.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateClock(HrmAttendanceClockSaveReqVO reqVO) {
        HrmAttendanceClockDO exist = validateClockExists(reqVO.getId());
        HrmAttendanceClockDO updateObj = BeanUtils.toBean(reqVO, HrmAttendanceClockDO.class);
        clockMapper.updateById(updateObj);
        invalidateMonthRecord(exist.getEmployeeId(), exist.getAttendanceTime());
        if (!exist.getEmployeeId().equals(reqVO.getEmployeeId())) {
            invalidateMonthRecord(reqVO.getEmployeeId(), reqVO.getAttendanceTime());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteClock(Long id) {
        HrmAttendanceClockDO exist = validateClockExists(id);
        clockMapper.deleteById(id);
        invalidateMonthRecord(exist.getEmployeeId(), exist.getAttendanceTime());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteClockList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        List<HrmAttendanceClockDO> clocks = clockMapper.selectByIds(ids);
        clocks.forEach(c -> invalidateMonthRecord(c.getEmployeeId(), c.getAttendanceTime()));
        clockMapper.deleteByIds(ids);
    }

    @Override
    public List<HrmAttendanceClockExportExcelVO> getClockExportList(HrmAttendanceClockPageReqVO reqVO) {
        Collection<Long> employeeIds = resolveEmployeeIds(reqVO.getDeptIds(), reqVO.getSearch());
        if (employeeIds != null && employeeIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<HrmAttendanceClockDO> clocks = clockMapper.selectPage(reqVO, employeeIds).getList();
        // 导出不分页：转全量查询（复用过滤条件，放宽分页）
        HrmAttendanceClockPageReqVO exportReq = reqVO;
        exportReq.setPageSize(-1);
        clocks = clockMapper.selectPage(exportReq, employeeIds).getList();
        return enrichClocks(clocks).stream().map(vo -> {
            HrmAttendanceClockExportExcelVO row = new HrmAttendanceClockExportExcelVO();
            row.setEmployeeName(vo.getEmployeeName());
            row.setJobNumber(vo.getJobNumber());
            row.setDeptName(vo.getDeptName());
            row.setAttendanceTime(vo.getAttendanceTime());
            row.setClockTime(vo.getClockTime());
            row.setType(vo.getType());
            row.setStatus(vo.getStatus());
            row.setAddress(vo.getAddress());
            row.setRemark(vo.getRemark());
            return row;
        }).collect(Collectors.toList());
    }

    // ========== 考勤组 ==========

    @Override
    public PageResult<HrmAttendanceGroupRespVO> getGroupPage(Integer pageNo, Integer pageSize, String name) {
        cn.zhicloud.framework.common.pojo.PageParam pageParam = new cn.zhicloud.framework.common.pojo.PageParam();
        pageParam.setPageNo(pageNo != null ? pageNo : 1);
        pageParam.setPageSize(pageSize != null ? pageSize : 10);
        PageResult<HrmAttendanceGroupDO> pageResult = groupMapper.selectPage(pageParam, name);
        List<HrmAttendanceGroupRespVO> voList = pageResult.getList().stream()
                .map(this::toGroupRespVO).collect(Collectors.toList());
        return new PageResult<>(voList, pageResult.getTotal());
    }

    @Override
    public HrmAttendanceGroupRespVO getGroup(Long id) {
        return toGroupRespVO(validateGroupExists(id));
    }

    @Override
    public HrmAttendanceGroupRespVO getMyGroup(Long employeeId) {
        validateEmployeeExists(employeeId);
        return toGroupRespVO(resolveEmployeeGroup(employeeId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGroup(HrmAttendanceGroupSaveReqVO reqVO) {
        validateGroupNameUnique(null, reqVO.getName());
        HrmAttendanceGroupDO group = BeanUtils.toBean(reqVO, HrmAttendanceGroupDO.class);
        applyGroupDefaults(group);
        groupMapper.insert(group);
        saveGroupEmployees(group.getId(), reqVO.getEmployeeIds());
        if (Boolean.TRUE.equals(group.getDefaultStatus())) {
            clearOtherDefaultGroups(group.getId());
        }
        return group.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGroup(HrmAttendanceGroupSaveReqVO reqVO) {
        validateGroupExists(reqVO.getId());
        validateGroupNameUnique(reqVO.getId(), reqVO.getName());
        HrmAttendanceGroupDO updateObj = BeanUtils.toBean(reqVO, HrmAttendanceGroupDO.class);
        applyGroupDefaults(updateObj);
        groupMapper.updateById(updateObj);
        // 员工关联整组替换
        groupEmployeeMapper.delete(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<HrmAttendanceGroupEmployeeDO>()
                .eq(HrmAttendanceGroupEmployeeDO::getGroupId, reqVO.getId()));
        saveGroupEmployees(reqVO.getId(), reqVO.getEmployeeIds());
        if (Boolean.TRUE.equals(updateObj.getDefaultStatus())) {
            clearOtherDefaultGroups(reqVO.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Long id) {
        validateGroupExists(id);
        groupEmployeeMapper.delete(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<HrmAttendanceGroupEmployeeDO>()
                .eq(HrmAttendanceGroupEmployeeDO::getGroupId, id));
        groupMapper.deleteById(id);
    }

    // ========== 节假日 ==========

    @Override
    public PageResult<HrmAttendanceHolidayRespVO> getHolidayPage(HrmAttendanceHolidayPageReqVO reqVO) {
        PageResult<HrmAttendanceHolidayDO> pageResult = holidayMapper.selectPage(reqVO, reqVO.getDates(), reqVO.getType());
        return new PageResult<>(BeanUtils.toBean(pageResult.getList(), HrmAttendanceHolidayRespVO.class),
                pageResult.getTotal());
    }

    @Override
    public HrmAttendanceHolidayRespVO getHoliday(Long id) {
        return BeanUtils.toBean(validateHolidayExists(id), HrmAttendanceHolidayRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHoliday(HrmAttendanceHolidaySaveReqVO reqVO) {
        if (holidayMapper.selectByDate(reqVO.getDate()) != null) {
            throw exception(HRM_ATTENDANCE_HOLIDAY_DATE_DUPLICATE);
        }
        HrmAttendanceHolidayDO holiday = BeanUtils.toBean(reqVO, HrmAttendanceHolidayDO.class);
        holidayMapper.insert(holiday);
        return holiday.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHoliday(HrmAttendanceHolidaySaveReqVO reqVO) {
        validateHolidayExists(reqVO.getId());
        HrmAttendanceHolidayDO exist = holidayMapper.selectByDate(reqVO.getDate());
        if (exist != null && !exist.getId().equals(reqVO.getId())) {
            throw exception(HRM_ATTENDANCE_HOLIDAY_DATE_DUPLICATE);
        }
        holidayMapper.updateById(BeanUtils.toBean(reqVO, HrmAttendanceHolidayDO.class));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHoliday(Long id) {
        validateHolidayExists(id);
        holidayMapper.deleteById(id);
    }

    // ========== 请假（读侧） ==========

    @Override
    public PageResult<HrmAttendanceLeaveRespVO> getLeavePage(HrmAttendanceLeavePageReqVO reqVO) {
        Collection<Long> employeeIds = resolveEmployeeIds(reqVO.getDeptIds(), reqVO.getSearch());
        if (employeeIds != null && employeeIds.isEmpty()) {
            return PageResult.empty();
        }
        PageResult<HrmAttendanceLeaveDO> pageResult = leaveMapper.selectPage(reqVO, employeeIds);
        return new PageResult<>(enrichLeaves(pageResult.getList()), pageResult.getTotal());
    }

    @Override
    public HrmAttendanceLeaveRespVO getLeave(Long id) {
        HrmAttendanceLeaveDO leave = leaveMapper.selectById(id);
        if (leave == null) {
            throw exception(HRM_ATTENDANCE_LEAVE_NOT_EXISTS);
        }
        return enrichLeaves(Collections.singletonList(leave)).get(0);
    }

    @Override
    public List<HrmAttendanceLeaveExportExcelVO> getLeaveExportList(HrmAttendanceLeavePageReqVO reqVO) {
        reqVO.setPageSize(-1);
        return getLeavePage(reqVO).getList().stream().map(vo -> {
            HrmAttendanceLeaveExportExcelVO row = new HrmAttendanceLeaveExportExcelVO();
            row.setEmployeeName(vo.getEmployeeName());
            row.setJobNumber(vo.getJobNumber());
            row.setDeptName(vo.getDeptName());
            row.setType(vo.getType());
            row.setStartTime(vo.getStartTime());
            row.setEndTime(vo.getEndTime());
            row.setDay(vo.getDay());
            row.setReason(vo.getReason());
            row.setApprovalStatus(vo.getApprovalStatus());
            return row;
        }).collect(Collectors.toList());
    }

    // ========== 统计 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PageResult<HrmAttendanceMonthRecordRespVO> getMonthRecordPage(HrmAttendanceMonthRecordPageReqVO reqVO) {
        List<HrEmployeeDO> employees = resolveEmployees(reqVO.getDeptIds(), reqVO.getSearch());
        if (employees.isEmpty()) {
            return PageResult.empty();
        }
        // 用员工维度分页（内存分页：员工量级为 HR 场景，万级以内可接受）
        int pageNo = reqVO.getPageNo() != null && reqVO.getPageNo() > 0 ? reqVO.getPageNo() : 1;
        int pageSize = reqVO.getPageSize() != null && reqVO.getPageSize() > 0 ? reqVO.getPageSize() : 10;
        int from = Math.min((pageNo - 1) * pageSize, employees.size());
        int to = Math.min(from + pageSize, employees.size());
        List<HrEmployeeDO> pageEmployees = employees.subList(from, to);
        List<HrmAttendanceMonthRecordDO> records = new ArrayList<>(pageEmployees.size());
        for (HrEmployeeDO employee : pageEmployees) {
            records.add(ensureMonthRecord(employee, reqVO.getYear(), reqVO.getMonth()));
        }
        List<HrmAttendanceMonthRecordRespVO> voList = enrichMonthRecords(records);
        return new PageResult<>(voList, (long) employees.size());
    }

    @Override
    public PageResult<HrmAttendanceMonthDailyOverviewRespVO> getMonthDailyPage(HrmAttendanceMonthRecordPageReqVO reqVO) {
        List<HrEmployeeDO> employees = resolveEmployees(reqVO.getDeptIds(), reqVO.getSearch());
        if (employees.isEmpty()) {
            return PageResult.empty();
        }
        int pageNo = reqVO.getPageNo() != null && reqVO.getPageNo() > 0 ? reqVO.getPageNo() : 1;
        int pageSize = reqVO.getPageSize() != null && reqVO.getPageSize() > 0 ? reqVO.getPageSize() : 10;
        int from = Math.min((pageNo - 1) * pageSize, employees.size());
        int to = Math.min(from + pageSize, employees.size());
        List<HrEmployeeDO> pageEmployees = employees.subList(from, to);
        List<HrmAttendanceMonthDailyOverviewRespVO> voList = pageEmployees.stream()
                .map(e -> buildMonthDailyOverview(e, reqVO.getYear(), reqVO.getMonth()))
                .collect(Collectors.toList());
        return new PageResult<>(voList, (long) employees.size());
    }

    @Override
    public HrmAttendanceMonthDetailRespVO getMonthDetail(Long employeeId, Integer year, Integer month) {
        HrEmployeeDO employee = validateEmployeeExists(employeeId);
        HrmAttendanceMonthRecordDO record = ensureMonthRecord(employee, year, month);
        HrmAttendanceMonthDetailRespVO resp = new HrmAttendanceMonthDetailRespVO();
        resp.setSummary(enrichMonthRecords(Collections.singletonList(record)).get(0));
        // 每日明细：当月每一天
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
        List<HrmAttendanceDailyDetailRespVO> dailyDetails = new ArrayList<>();
        for (LocalDate date = monthStart; !date.isAfter(monthEnd); date = date.plusDays(1)) {
            dailyDetails.add(buildDailyDetail(employee, date));
        }
        resp.setDailyDetails(dailyDetails);
        // 请假记录
        List<HrmAttendanceLeaveDO> leaves = leaveMapper.selectListByEmployeeAndMonth(employeeId,
                monthStart.atStartOfDay(), monthEnd.atTime(LocalTime.MAX));
        resp.setLeaves(enrichLeaves(leaves));
        return resp;
    }

    @Override
    public HrmAttendanceDailyDetailRespVO getDailyDetail(Long employeeId, String date) {
        HrEmployeeDO employee = validateEmployeeExists(employeeId);
        return buildDailyDetail(employee, parseDate(date));
    }

    @Override
    public List<HrmAttendanceMonthRecordExportExcelVO> getMonthRecordExportList(HrmAttendanceMonthRecordPageReqVO reqVO) {
        reqVO.setPageSize(-1);
        reqVO.setPageNo(1);
        return getMonthRecordPage(reqVO).getList().stream().map(vo -> {
            HrmAttendanceMonthRecordExportExcelVO row = new HrmAttendanceMonthRecordExportExcelVO();
            row.setEmployeeName(vo.getEmployeeName());
            row.setJobNumber(vo.getJobNumber());
            row.setDeptName(vo.getDeptName());
            row.setYear(vo.getYear());
            row.setMonth(vo.getMonth());
            row.setAttendDays(vo.getAttendDays());
            row.setActualDays(vo.getActualDays());
            row.setLateCount(vo.getLateCount());
            row.setEarlyCount(vo.getEarlyCount());
            row.setMisscardCount(vo.getMisscardCount());
            row.setAbsenteeismDays(vo.getAbsenteeismDays());
            row.setLeaveDays(vo.getLeaveDays());
            row.setAttendanceDeductAmount(vo.getAttendanceDeductAmount());
            row.setFullAttendance(vo.getFullAttendance());
            return row;
        }).collect(Collectors.toList());
    }

    @Override
    public List<HrmAttendanceMonthDailyExportExcelVO> getMonthDailyExportList(HrmAttendanceMonthRecordPageReqVO reqVO) {
        reqVO.setPageSize(-1);
        reqVO.setPageNo(1);
        // 扁平导出：FastExcel 不支持嵌套 Map/List 列，只导概览计数
        return getMonthDailyPage(reqVO).getList().stream().map(vo -> {
            HrmAttendanceMonthDailyExportExcelVO row = new HrmAttendanceMonthDailyExportExcelVO();
            row.setEmployeeName(vo.getEmployeeName());
            row.setJobNumber(vo.getJobNumber());
            row.setDeptName(vo.getDeptName());
            row.setYear(vo.getYear());
            row.setMonth(vo.getMonth());
            row.setClockDays(vo.getDailyClockMap() != null ? vo.getDailyClockMap().size() : 0);
            return row;
        }).collect(Collectors.toList());
    }

    // ========== 私有：员工/部门解析 ==========

    /**
     * 按部门（含子部门）与关键字解析员工编号集；两个条件都没有时返回 null（表示不限制）
     */
    private Collection<Long> resolveEmployeeIds(Long[] deptIds, String search) {
        if ((deptIds == null || deptIds.length == 0) && StrUtil.isEmpty(search)) {
            return null;
        }
        List<HrEmployeeDO> employees = resolveEmployees(deptIds, search);
        return employees.stream().map(HrEmployeeDO::getId).collect(Collectors.toSet());
    }

    private List<HrEmployeeDO> resolveEmployees(Long[] deptIds, String search) {
        Set<Long> expandedDeptIds = new HashSet<>();
        if (deptIds != null) {
            for (Long deptId : deptIds) {
                if (deptId == null) {
                    continue;
                }
                expandedDeptIds.add(deptId);
                deptApi.getChildDeptList(deptId).forEach(d -> expandedDeptIds.add(d.getId()));
            }
        }
        List<HrEmployeeDO> employees = employeeMapper.selectList(
                new cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX<HrEmployeeDO>()
                        .inIfPresent(HrEmployeeDO::getDeptId, expandedDeptIds.isEmpty() ? null : expandedDeptIds)
                        // 考勤范围：在职/待离职（离职员工不再计考勤）
                        .in(HrEmployeeDO::getEntryStatus, Arrays.asList(1, 3))
                        .orderByAsc(HrEmployeeDO::getId));
        if (StrUtil.isEmpty(search)) {
            return employees;
        }
        String kw = search.trim();
        List<Long> matchedIds = employees.stream()
                .filter(e -> (e.getName() != null && e.getName().contains(kw))
                        || (e.getEmpNo() != null && e.getEmpNo().contains(kw)))
                .map(HrEmployeeDO::getId)
                .collect(Collectors.toList());
        return employees.stream().filter(e -> matchedIds.contains(e.getId())).collect(Collectors.toList());
    }

    private HrEmployeeDO validateEmployeeExists(Long id) {
        HrEmployeeDO employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw exception(HRM_EMPLOYEE_NOT_EXISTS);
        }
        return employee;
    }

    private HrmAttendanceClockDO validateClockExists(Long id) {
        HrmAttendanceClockDO clock = clockMapper.selectById(id);
        if (clock == null) {
            throw exception(HRM_ATTENDANCE_CLOCK_NOT_EXISTS);
        }
        return clock;
    }

    private HrmAttendanceGroupDO validateGroupExists(Long id) {
        HrmAttendanceGroupDO group = groupMapper.selectById(id);
        if (group == null) {
            throw exception(HRM_ATTENDANCE_GROUP_NOT_EXISTS);
        }
        return group;
    }

    private HrmAttendanceHolidayDO validateHolidayExists(Long id) {
        HrmAttendanceHolidayDO holiday = holidayMapper.selectById(id);
        if (holiday == null) {
            throw exception(HRM_ATTENDANCE_HOLIDAY_NOT_EXISTS);
        }
        return holiday;
    }

    private void validateGroupNameUnique(Long id, String name) {
        HrmAttendanceGroupDO exist = groupMapper.selectByName(name);
        if (exist != null && (id == null || !exist.getId().equals(id))) {
            throw exception(HRM_ATTENDANCE_GROUP_NAME_DUPLICATE);
        }
    }

    private void applyGroupDefaults(HrmAttendanceGroupDO group) {
        if (group.getOpenWifiCard() == null) {
            group.setOpenWifiCard(false);
        }
        if (group.getOpenPointCard() == null) {
            group.setOpenPointCard(false);
        }
        if (group.getRest() == null) {
            group.setRest(true);
        }
        if (group.getDefaultStatus() == null) {
            group.setDefaultStatus(false);
        }
    }

    private void saveGroupEmployees(Long groupId, List<Long> employeeIds) {
        if (CollUtil.isEmpty(employeeIds)) {
            return;
        }
        for (Long employeeId : employeeIds) {
            HrmAttendanceGroupEmployeeDO relation = HrmAttendanceGroupEmployeeDO.builder()
                    .groupId(groupId)
                    .employeeId(employeeId)
                    .build();
            groupEmployeeMapper.insert(relation);
        }
    }

    private void clearOtherDefaultGroups(Long keepId) {
        groupMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<HrmAttendanceGroupDO>()
                .ne(HrmAttendanceGroupDO::getId, keepId)
                .set(HrmAttendanceGroupDO::getDefaultStatus, false));
    }

    /**
     * 解析员工所在考勤组：员工关联优先，回退默认组
     */
    private HrmAttendanceGroupDO resolveEmployeeGroup(Long employeeId) {
        List<HrmAttendanceGroupEmployeeDO> relations = groupEmployeeMapper.selectListByEmployeeId(employeeId);
        if (CollUtil.isNotEmpty(relations)) {
            HrmAttendanceGroupDO group = groupMapper.selectById(relations.get(0).getGroupId());
            if (group != null) {
                return group;
            }
        }
        return groupMapper.selectDefaultGroup();
    }

    // ========== 私有：enrich ==========

    private List<HrmAttendanceClockRespVO> enrichClocks(List<HrmAttendanceClockDO> clocks) {
        if (CollUtil.isEmpty(clocks)) {
            return Collections.emptyList();
        }
        List<HrmAttendanceClockRespVO> voList = BeanUtils.toBean(clocks, HrmAttendanceClockRespVO.class);
        Map<Long, HrEmployeeDO> employeeMap = loadEmployeeMap(clocks.stream()
                .map(HrmAttendanceClockDO::getEmployeeId).collect(Collectors.toSet()));
        Map<Long, DeptRespDTO> deptMap = loadDeptMap(employeeMap.values());
        for (int i = 0; i < clocks.size(); i++) {
            fillClockEmployee(voList.get(i), employeeMap.get(clocks.get(i).getEmployeeId()), deptMap);
        }
        return voList;
    }

    private void fillClockEmployee(HrmAttendanceClockRespVO vo, HrEmployeeDO employee, Map<Long, DeptRespDTO> deptMap) {
        if (employee == null) {
            return;
        }
        vo.setEmployeeName(employee.getName());
        vo.setJobNumber(employee.getEmpNo());
        vo.setDeptId(employee.getDeptId());
        vo.setPostName(employee.getPostName());
        DeptRespDTO dept = deptMap.get(employee.getDeptId());
        if (dept != null) {
            vo.setDeptName(dept.getName());
        }
    }

    private List<HrmAttendanceLeaveRespVO> enrichLeaves(List<HrmAttendanceLeaveDO> leaves) {
        if (CollUtil.isEmpty(leaves)) {
            return Collections.emptyList();
        }
        List<HrmAttendanceLeaveRespVO> voList = BeanUtils.toBean(leaves, HrmAttendanceLeaveRespVO.class);
        Map<Long, HrEmployeeDO> employeeMap = loadEmployeeMap(leaves.stream()
                .map(HrmAttendanceLeaveDO::getEmployeeId).collect(Collectors.toSet()));
        Map<Long, DeptRespDTO> deptMap = loadDeptMap(employeeMap.values());
        for (int i = 0; i < leaves.size(); i++) {
            HrmAttendanceLeaveRespVO vo = voList.get(i);
            HrEmployeeDO employee = employeeMap.get(leaves.get(i).getEmployeeId());
            if (employee == null) {
                continue;
            }
            vo.setEmployeeName(employee.getName());
            vo.setJobNumber(employee.getEmpNo());
            vo.setDeptId(employee.getDeptId());
            vo.setPostName(employee.getPostName());
            DeptRespDTO dept = deptMap.get(employee.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
        }
        return voList;
    }

    private HrmAttendanceGroupRespVO toGroupRespVO(HrmAttendanceGroupDO group) {
        if (group == null) {
            return null;
        }
        HrmAttendanceGroupRespVO vo = BeanUtils.toBean(group, HrmAttendanceGroupRespVO.class);
        // 员工名 enrich（deptNames 由 deptIds 解析）
        if (CollUtil.isNotEmpty(group.getEmployeeIds())) {
            List<HrEmployeeDO> employees = employeeMapper.selectByIds(group.getEmployeeIds());
            Map<Long, String> nameMap = employees.stream()
                    .collect(Collectors.toMap(HrEmployeeDO::getId, HrEmployeeDO::getName));
            vo.setEmployeeNames(group.getEmployeeIds().stream()
                    .map(id -> nameMap.getOrDefault(id, String.valueOf(id)))
                    .collect(Collectors.toList()));
        }
        if (CollUtil.isNotEmpty(group.getDeptIds())) {
            Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(group.getDeptIds());
            vo.setDeptNames(group.getDeptIds().stream()
                    .map(id -> {
                        DeptRespDTO dept = deptMap.get(id);
                        return dept != null ? dept.getName() : String.valueOf(id);
                    })
                    .collect(Collectors.toList()));
        }
        return vo;
    }

    private Map<Long, HrEmployeeDO> loadEmployeeMap(Collection<Long> employeeIds) {
        if (CollUtil.isEmpty(employeeIds)) {
            return Collections.emptyMap();
        }
        return employeeMapper.selectByIds(employeeIds).stream()
                .collect(Collectors.toMap(HrEmployeeDO::getId, e -> e, (a, b) -> a));
    }

    private Map<Long, DeptRespDTO> loadDeptMap(Collection<HrEmployeeDO> employees) {
        Set<Long> deptIds = employees.stream()
                .map(HrEmployeeDO::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
        return deptIds.isEmpty() ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
    }

    // ========== 私有：班次与天数 ==========

    /**
     * 匹配某日适用的班次配置（weeks 含该日 ISO 周几时命中）
     */
    private Map<String, Object> matchShiftForDate(HrmAttendanceGroupDO group, LocalDate date) {
        if (group == null || CollUtil.isEmpty(group.getShifts())) {
            return null;
        }
        int dayOfWeek = date.getDayOfWeek().getValue(); // 1=周一 .. 7=周日
        for (Map<String, Object> shift : group.getShifts()) {
            Object weeks = shift.get("weeks");
            if (weeks instanceof List) {
                for (Object w : (List<?>) weeks) {
                    if (w != null && String.valueOf((int) Double.parseDouble(w.toString())).equals(String.valueOf(dayOfWeek))) {
                        return shift;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 当月应出勤天数：按考勤组班次 weeks（无组回退周一~周五），扣除休息类节假日、补回上班类节假日
     */
    private BigDecimal resolveAttendDays(HrmAttendanceGroupDO group, int year, int month) {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
        Set<Integer> workWeeks = new HashSet<>();
        boolean hasShift = false;
        if (group != null && CollUtil.isNotEmpty(group.getShifts())) {
            for (Map<String, Object> shift : group.getShifts()) {
                Object weeks = shift.get("weeks");
                if (weeks instanceof List) {
                    for (Object w : (List<?>) weeks) {
                        if (w != null) {
                            workWeeks.add((int) Double.parseDouble(w.toString()));
                            hasShift = true;
                        }
                    }
                }
            }
        }
        if (!hasShift) {
            workWeeks.addAll(Arrays.asList(1, 2, 3, 4, 5));
        }
        // 节假日映射（1上班 2休息）
        List<LocalDate> monthDates = new ArrayList<>();
        for (LocalDate d = monthStart; !d.isAfter(monthEnd); d = d.plusDays(1)) {
            monthDates.add(d);
        }
        Map<LocalDate, Integer> holidayMap = holidayMapper.selectListByDates(monthDates).stream()
                .collect(Collectors.toMap(HrmAttendanceHolidayDO::getDate, HrmAttendanceHolidayDO::getType, (a, b) -> a));
        int days = 0;
        for (LocalDate d = monthStart; !d.isAfter(monthEnd); d = d.plusDays(1)) {
            Integer holidayType = holidayMap.get(d);
            if (holidayType != null) {
                // 1上班 2休息
                if (holidayType == 1) {
                    days++;
                }
                continue;
            }
            if (workWeeks.contains(d.getDayOfWeek().getValue())) {
                days++;
            }
        }
        return BigDecimal.valueOf(days);
    }

    // ========== 私有：月度汇总 ==========

    /**
     * 获得（或计算并落库）员工某月汇总
     */
    private HrmAttendanceMonthRecordDO ensureMonthRecord(HrEmployeeDO employee, Integer year, Integer month) {
        HrmAttendanceMonthRecordDO record = monthRecordMapper.selectByEmployeeAndMonth(employee.getId(), year, month);
        if (record != null) {
            return record;
        }
        record = computeMonthRecord(employee, year, month);
        monthRecordMapper.insert(record);
        return record;
    }

    private HrmAttendanceMonthRecordDO computeMonthRecord(HrEmployeeDO employee, Integer year, Integer month) {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
        LocalDateTime rangeStart = monthStart.atStartOfDay();
        LocalDateTime rangeEnd = monthEnd.atTime(LocalTime.MAX);
        List<HrmAttendanceClockDO> clocks = clockMapper.selectListByEmployeeAndMonth(
                Collections.singletonList(employee.getId()), rangeStart, rangeEnd);
        HrmAttendanceGroupDO group = resolveEmployeeGroup(employee.getId());
        Map<String, Object> deductRule = group != null && group.getDeductRule() != null
                ? group.getDeductRule() : Collections.emptyMap();

        int lateCount = 0, earlyCount = 0, misscardCount = 0, absenteeismClocks = 0;
        int lateMinutes = 0, earlyMinutes = 0;
        int actualDays = 0;
        Set<LocalDate> actualDates = new HashSet<>();
        for (HrmAttendanceClockDO clock : clocks) {
            Integer status = clock.getStatus();
            if (status == null) {
                continue;
            }
            if (clock.getAttendanceTime() != null) {
                actualDates.add(clock.getAttendanceTime().toLocalDate());
            }
            if (status == CLOCK_STATUS_LATE) {
                lateCount++;
                // 迟到分钟：班次上班时间与打卡时间差
                lateMinutes += minutesLate(employee, clock, true);
            } else if (status == CLOCK_STATUS_EARLY) {
                earlyCount++;
                earlyMinutes += minutesLate(employee, clock, false);
            } else if (status == CLOCK_STATUS_MISSCARD) {
                misscardCount++;
            } else if (status == CLOCK_STATUS_ABSENTEEISM) {
                absenteeismClocks++;
            }
        }
        actualDays = actualDates.size();

        // 请假聚合（当月）
        List<HrmAttendanceLeaveDO> leaves = leaveMapper.selectListByEmployeeAndMonth(
                employee.getId(), rangeStart, rangeEnd);
        BigDecimal leaveDays = BigDecimal.ZERO;
        int leaveMinutes = 0;
        for (HrmAttendanceLeaveDO leave : leaves) {
            if (leave.getDay() != null) {
                leaveDays = leaveDays.add(leave.getDay());
            }
            if (leave.getStartTime() != null && leave.getEndTime() != null) {
                leaveMinutes += (int) Duration.between(leave.getStartTime(), leave.getEndTime()).toMinutes();
            }
        }

        BigDecimal attendanceDays = resolveAttendDays(group, year, month);
        // 旷工天数：应出勤 − 实际出勤 − 请假（下限 0）
        double absentDaysRaw = attendanceDays.doubleValue() - actualDays - leaveDays.doubleValue();
        BigDecimal absenteeismDays = BigDecimal.valueOf(Math.max(absentDaysRaw, 0)).setScale(2, RoundingMode.HALF_UP);

        // 扣款
        BigDecimal lateDeduct = calcDeduct(deductRule, "lateMethod", "lateDeductMoney", lateCount, lateMinutes);
        BigDecimal earlyDeduct = calcDeduct(deductRule, "earlyMethod", "earlyDeductMoney", earlyCount, earlyMinutes);
        BigDecimal misscardDeduct = calcDeduct(deductRule, "misscardMethod", "misscardDeductMoney", misscardCount, 0);
        BigDecimal absenteeismDeduct = calcDeduct(deductRule, "absenteeismMethod", "absenteeismDeductMoney",
                absenteeismDays.setScale(0, RoundingMode.CEILING).intValue(), 0);
        BigDecimal totalDeduct = lateDeduct.add(earlyDeduct).add(misscardDeduct).add(absenteeismDeduct);

        HrmAttendanceMonthRecordDO record = HrmAttendanceMonthRecordDO.builder()
                .employeeId(employee.getId())
                .year(year)
                .month(month)
                .attendDays(attendanceDays)
                .actualDays(BigDecimal.valueOf(actualDays))
                .lateMinute(lateMinutes)
                .lateCount(lateCount)
                .earlyMinute(earlyMinutes)
                .earlyCount(earlyCount)
                .misscardCount(misscardCount)
                .absenteeismDays(absenteeismDays)
                .absenteeismMinutes(absenteeismClocks * 0) // 分钟级旷工需班次比对，按天口径先记 0
                .leaveDays(leaveDays)
                .leaveMinutes(leaveMinutes)
                .lateDeductAmount(lateDeduct)
                .earlyDeductAmount(earlyDeduct)
                .misscardDeductAmount(misscardDeduct)
                .absenteeismDeductAmount(absenteeismDeduct)
                .attendanceDeductAmount(totalDeduct)
                .fullAttendance(totalDeduct.compareTo(BigDecimal.ZERO) == 0
                        && absenteeismDays.compareTo(BigDecimal.ZERO) == 0
                        && misscardCount == 0)
                .build();
        return record;
    }

    /**
     * 迟到/早退分钟（班次上下班时间与该次打卡时间差，取正数）
     */
    private int minutesLate(HrEmployeeDO employee, HrmAttendanceClockDO clock, boolean onDuty) {
        if (clock.getClockTime() == null || clock.getAttendanceTime() == null) {
            return 0;
        }
        HrmAttendanceGroupDO group = resolveEmployeeGroup(employee.getId());
        Map<String, Object> shift = matchShiftForDate(group, clock.getAttendanceTime().toLocalDate());
        if (shift == null) {
            return 0;
        }
        String refTime = onDuty ? str(shift.get("startTime")) : str(shift.get("endTime"));
        LocalDateTime ref = atTime(clock.getAttendanceTime().toLocalDate(), refTime);
        if (ref == null) {
            return 0;
        }
        long minutes = onDuty
                ? Duration.between(ref, clock.getClockTime()).toMinutes()
                : Duration.between(clock.getClockTime(), ref).toMinutes();
        return (int) Math.max(minutes, 0);
    }

    @SuppressWarnings("unchecked")
    private BigDecimal calcDeduct(Map<String, Object> rule, String methodKey, String moneyKey,
                                  int count, int minutes) {
        if (rule == null || rule.isEmpty() || count <= 0) {
            return BigDecimal.ZERO;
        }
        Object methodObj = rule.get(methodKey);
        Object moneyObj = rule.get(moneyKey);
        if (methodObj == null || moneyObj == null) {
            return BigDecimal.ZERO;
        }
        int method = (int) Double.parseDouble(methodObj.toString());
        BigDecimal money = new BigDecimal(moneyObj.toString());
        switch (method) {
            case 1: // 每月固定
                return money;
            case 2: // 按分钟
                return money.multiply(BigDecimal.valueOf(minutes));
            case 3: // 按次数
                return money.multiply(BigDecimal.valueOf(count));
            default:
                return BigDecimal.ZERO;
        }
    }

    private List<HrmAttendanceMonthRecordRespVO> enrichMonthRecords(List<HrmAttendanceMonthRecordDO> records) {
        if (CollUtil.isEmpty(records)) {
            return Collections.emptyList();
        }
        List<HrmAttendanceMonthRecordRespVO> voList = BeanUtils.toBean(records, HrmAttendanceMonthRecordRespVO.class);
        Map<Long, HrEmployeeDO> employeeMap = loadEmployeeMap(records.stream()
                .map(HrmAttendanceMonthRecordDO::getEmployeeId).collect(Collectors.toSet()));
        Map<Long, DeptRespDTO> deptMap = loadDeptMap(employeeMap.values());
        for (int i = 0; i < records.size(); i++) {
            HrmAttendanceMonthRecordRespVO vo = voList.get(i);
            HrEmployeeDO employee = employeeMap.get(records.get(i).getEmployeeId());
            if (employee == null) {
                continue;
            }
            vo.setEmployeeName(employee.getName());
            vo.setJobNumber(employee.getEmpNo());
            vo.setDeptId(employee.getDeptId());
            vo.setPostName(employee.getPostName());
            vo.setEntryTime(employee.getEntryTime());
            vo.setEmployeeStatus(employee.getEmployeeStatus());
            vo.setWorkCity(employee.getWorkCity());
            DeptRespDTO dept = deptMap.get(employee.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
            HrmAttendanceGroupDO group = resolveEmployeeGroup(employee.getId());
            if (group != null) {
                vo.setAttendanceGroupName(group.getName());
            }
        }
        return voList;
    }

    private HrmAttendanceMonthDailyOverviewRespVO buildMonthDailyOverview(HrEmployeeDO employee, Integer year, Integer month) {
        HrmAttendanceMonthDailyOverviewRespVO vo = new HrmAttendanceMonthDailyOverviewRespVO();
        vo.setEmployeeId(employee.getId());
        vo.setEmployeeName(employee.getName());
        vo.setJobNumber(employee.getEmpNo());
        vo.setDeptId(employee.getDeptId());
        vo.setPostName(employee.getPostName());
        DeptRespDTO dept = employee.getDeptId() != null ? deptApi.getDept(employee.getDeptId()) : null;
        if (dept != null) {
            vo.setDeptName(dept.getName());
        }
        vo.setYear(year);
        vo.setMonth(month);
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
        List<HrmAttendanceClockDO> clocks = clockMapper.selectListByEmployeeAndMonth(
                Collections.singletonList(employee.getId()), monthStart.atStartOfDay(), monthEnd.atTime(LocalTime.MAX));
        Map<String, List<HrmAttendanceClockDO>> byDate = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (HrmAttendanceClockDO clock : clocks) {
            if (clock.getAttendanceTime() == null) {
                continue;
            }
            byDate.computeIfAbsent(clock.getAttendanceTime().toLocalDate().format(fmt), k -> new ArrayList<>()).add(clock);
        }
        Map<String, HrmAttendanceDailyOverviewRespVO> dailyMap = new LinkedHashMap<>();
        for (Map.Entry<String, List<HrmAttendanceClockDO>> entry : byDate.entrySet()) {
            HrmAttendanceDailyOverviewRespVO day = new HrmAttendanceDailyOverviewRespVO();
            day.setClocks(enrichClocks(entry.getValue()));
            // 考勤结果：以最差状态呈现
            day.setAttendanceResult(resolveAttendanceResult(entry.getValue()));
            List<HrmAttendanceDailyOverviewItemRespVO> items = new ArrayList<>();
            for (HrmAttendanceClockDO clock : entry.getValue()) {
                HrmAttendanceDailyOverviewItemRespVO item = new HrmAttendanceDailyOverviewItemRespVO();
                item.setType(clock.getType() != null && clock.getType() == CLOCK_TYPE_ON_DUTY ? "上班" : "下班");
                item.setTime(clock.getClockTime() != null
                        ? clock.getClockTime().format(DateTimeFormatter.ofPattern("HH:mm")) : null);
                item.setStatus(clockStatusName(clock.getStatus()));
                item.setText(clockStatusName(clock.getStatus()));
                items.add(item);
            }
            day.setOverviews(items);
            dailyMap.put(entry.getKey(), day);
        }
        vo.setDailyClockMap(dailyMap);
        return vo;
    }

    private HrmAttendanceDailyDetailRespVO buildDailyDetail(HrEmployeeDO employee, LocalDate date) {
        HrmAttendanceDailyDetailRespVO vo = new HrmAttendanceDailyDetailRespVO();
        vo.setEmployeeId(employee.getId());
        vo.setEmployeeName(employee.getName());
        vo.setJobNumber(employee.getEmpNo());
        vo.setDeptId(employee.getDeptId());
        vo.setPostName(employee.getPostName());
        DeptRespDTO dept = employee.getDeptId() != null ? deptApi.getDept(employee.getDeptId()) : null;
        if (dept != null) {
            vo.setDeptName(dept.getName());
        }
        vo.setAttendanceTime(date.atStartOfDay());

        // 班次
        HrmAttendanceGroupDO group = resolveEmployeeGroup(employee.getId());
        Map<String, Object> shift = matchShiftForDate(group, date);
        vo.setScheduled(shift != null);
        int requiredClockCount = 0;
        if (shift != null) {
            requiredClockCount = 2;
            vo.setShiftName(group.getName());
            LocalDateTime start = atTime(date, str(shift.get("startTime")));
            LocalDateTime end = atTime(date, str(shift.get("endTime")));
            if (start != null && end != null) {
                vo.setScheduledMinutes((int) Duration.between(start, end).toMinutes());
            }
        }
        vo.setRequiredClockCount(requiredClockCount);

        // 打卡
        List<HrmAttendanceClockDO> clocks = clockMapper.selectListByEmployeeAndDate(
                employee.getId(), date.atStartOfDay(), date.atTime(LocalTime.MAX));
        vo.setClockList(enrichClocks(clocks));
        int lateCount = 0, earlyCount = 0, misscardCount = 0;
        int lateMinutes = 0, earlyMinutes = 0;
        boolean absenteeism = false;
        for (HrmAttendanceClockDO clock : clocks) {
            if (clock.getStatus() == null) {
                continue;
            }
            if (clock.getStatus() == CLOCK_STATUS_LATE) {
                lateCount++;
                lateMinutes += minutesLate(employee, clock, true);
            } else if (clock.getStatus() == CLOCK_STATUS_EARLY) {
                earlyCount++;
                earlyMinutes += minutesLate(employee, clock, false);
            } else if (clock.getStatus() == CLOCK_STATUS_MISSCARD) {
                misscardCount++;
            } else if (clock.getStatus() == CLOCK_STATUS_ABSENTEEISM) {
                absenteeism = true;
            }
        }
        // 缺卡：应打卡 2 次但实际不足
        if (shift != null && clocks.size() < requiredClockCount) {
            misscardCount += (requiredClockCount - clocks.size());
        }
        // 旷工：排班日出勤无任何打卡且无请假
        if (shift != null && clocks.isEmpty()) {
            List<HrmAttendanceLeaveDO> leaves = leaveMapper.selectListByEmployeeAndMonth(
                    employee.getId(), date.atStartOfDay(), date.atTime(LocalTime.MAX));
            if (leaves.isEmpty()) {
                absenteeism = true;
            }
        }
        vo.setLateCount(lateCount);
        vo.setLateMinutes(lateMinutes);
        vo.setEarlyCount(earlyCount);
        vo.setEarlyMinutes(earlyMinutes);
        vo.setMisscardCount(misscardCount);
        vo.setAbsenteeism(absenteeism);
        if (absenteeism) {
            vo.setAbsenteeismDays(BigDecimal.ONE);
        }
        // 请假
        List<HrmAttendanceLeaveDO> leaves = leaveMapper.selectListByEmployeeAndMonth(
                employee.getId(), date.atStartOfDay(), date.atTime(LocalTime.MAX));
        if (CollUtil.isNotEmpty(leaves)) {
            vo.setLeaveStatus(true);
            BigDecimal leaveDays = leaves.stream()
                    .map(l -> l.getDay() != null ? l.getDay() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            vo.setLeaveDays(leaveDays);
            int leaveMinutes = 0;
            for (HrmAttendanceLeaveDO leave : leaves) {
                if (leave.getStartTime() != null && leave.getEndTime() != null) {
                    leaveMinutes += (int) Duration.between(leave.getStartTime(), leave.getEndTime()).toMinutes();
                }
            }
            vo.setLeaveMinutes(leaveMinutes);
        } else {
            vo.setLeaveStatus(false);
        }
        vo.setAttendanceResult(resolveAttendanceResult(clocks));
        return vo;
    }

    /**
     * 考勤结果文案（按最差状态呈现：旷工 > 缺卡 > 迟到/早退 > 正常）
     */
    private String resolveAttendanceResult(List<HrmAttendanceClockDO> clocks) {
        if (CollUtil.isEmpty(clocks)) {
            return null;
        }
        Set<Integer> statuses = clocks.stream()
                .map(HrmAttendanceClockDO::getStatus).filter(Objects::nonNull).collect(Collectors.toSet());
        if (statuses.contains(CLOCK_STATUS_ABSENTEEISM)) {
            return clockStatusName(CLOCK_STATUS_ABSENTEEISM);
        }
        if (statuses.contains(CLOCK_STATUS_MISSCARD)) {
            return clockStatusName(CLOCK_STATUS_MISSCARD);
        }
        if (statuses.contains(CLOCK_STATUS_LATE)) {
            return clockStatusName(CLOCK_STATUS_LATE);
        }
        if (statuses.contains(CLOCK_STATUS_EARLY)) {
            return clockStatusName(CLOCK_STATUS_EARLY);
        }
        return clockStatusName(CLOCK_STATUS_NORMAL);
    }

    /**
     * 打卡状态名（与 hrm_attendance_clock_status 字典 label 一致）
     */
    private String clockStatusName(Integer status) {
        if (status == null) {
            return null;
        }
        switch (status) {
            case CLOCK_STATUS_NORMAL: return "正常";
            case CLOCK_STATUS_LATE: return "迟到";
            case CLOCK_STATUS_EARLY: return "早退";
            case CLOCK_STATUS_MISSCARD: return "缺卡";
            case CLOCK_STATUS_ABSENTEEISM: return "旷工";
            default: return String.valueOf(status);
        }
    }

    /**
     * 打卡变更后删除当月汇总缓存（下次查询重算）
     */
    private void invalidateMonthRecord(Long employeeId, LocalDateTime attendanceTime) {
        if (employeeId == null || attendanceTime == null) {
            return;
        }
        monthRecordMapper.delete(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<HrmAttendanceMonthRecordDO>()
                .eq(HrmAttendanceMonthRecordDO::getEmployeeId, employeeId)
                .eq(HrmAttendanceMonthRecordDO::getYear, attendanceTime.getYear())
                .eq(HrmAttendanceMonthRecordDO::getMonth, attendanceTime.getMonthValue()));
    }

    // ========== 私有：小工具 ==========

    private static LocalDate parseDate(String date) {
        if (StrUtil.isEmpty(date)) {
            throw exception(HRM_ATTENDANCE_DATE_INVALID);
        }
        try {
            return LocalDate.parse(date.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception ex) {
            throw exception(HRM_ATTENDANCE_DATE_INVALID);
        }
    }

    private static LocalDateTime atTime(LocalDate date, String time) {
        if (date == null || StrUtil.isEmpty(time)) {
            return null;
        }
        try {
            LocalTime localTime = LocalTime.parse(time.trim(), DateTimeFormatter.ofPattern("HH:mm"));
            return LocalDateTime.of(date, localTime);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String str(Object value) {
        return value == null ? null : value.toString();
    }

}
