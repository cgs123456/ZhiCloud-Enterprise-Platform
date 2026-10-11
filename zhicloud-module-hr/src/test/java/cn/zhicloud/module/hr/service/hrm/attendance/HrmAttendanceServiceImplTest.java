package cn.zhicloud.module.hr.service.hrm.attendance;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.hr.controller.admin.hrm.attendance.vo.*;
import cn.zhicloud.module.hr.dal.dataobject.employee.HrEmployeeDO;
import cn.zhicloud.module.hr.dal.dataobject.hrm.*;
import cn.zhicloud.module.hr.dal.mysql.employee.HrEmployeeMapper;
import cn.zhicloud.module.hr.dal.mysql.hrm.*;
import cn.zhicloud.module.system.api.dept.DeptApi;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link HrmAttendanceServiceImpl} 的单元测试
 *
 * @author zhicloud
 */
@Import(HrmAttendanceServiceImpl.class)
public class HrmAttendanceServiceImplTest extends BaseDbUnitTest {

    @Resource
    private HrmAttendanceServiceImpl attendanceService;

    @Resource
    private HrEmployeeMapper employeeMapper;
    @Resource
    private HrmAttendanceClockMapper clockMapper;
    @Resource
    private HrmAttendanceGroupMapper groupMapper;
    @Resource
    private HrmAttendanceHolidayMapper holidayMapper;
    @Resource
    private HrmAttendanceLeaveMapper leaveMapper;
    @Resource
    private HrmAttendanceMonthRecordMapper monthRecordMapper;
    @Resource
    private HrmAttendanceGroupEmployeeMapper groupEmployeeMapper;

    @MockitoBean
    private DeptApi deptApi;

    private HrEmployeeDO insertEmployee() {
        HrEmployeeDO employee = randomPojo(HrEmployeeDO.class, o -> {
            o.setName("考勤张三");
            o.setEmpNo("ATT001");
            o.setEntryStatus(1);
            o.setDeptId(null);
        });
        employeeMapper.insert(employee);
        return employee;
    }

    @Test
    public void test_createClock_success() {
        HrEmployeeDO employee = insertEmployee();
        HrmAttendanceClockSaveReqVO reqVO = new HrmAttendanceClockSaveReqVO();
        reqVO.setEmployeeId(employee.getId());
        reqVO.setType(1);
        reqVO.setAttendanceTime(LocalDateTime.of(2026, 10, 9, 0, 0));
        reqVO.setClockTime(LocalDateTime.of(2026, 10, 9, 8, 55));
        reqVO.setRemark("正常打卡");

        Long id = attendanceService.createClock(reqVO);

        HrmAttendanceClockDO clock = clockMapper.selectById(id);
        assertNotNull(clock);
        assertEquals(employee.getId(), clock.getEmployeeId());
        assertEquals(Integer.valueOf(1), clock.getType());
        // 默认来源=手工录入、默认状态=正常
        assertEquals(Integer.valueOf(2), clock.getSourceType());
        assertEquals(Integer.valueOf(1), clock.getStatus());
    }

    @Test
    public void test_deleteClock_success() {
        HrEmployeeDO employee = insertEmployee();
        HrmAttendanceClockDO clock = randomPojo(HrmAttendanceClockDO.class, o -> {
            o.setEmployeeId(employee.getId());
            o.setType(1);
            o.setAttendanceTime(LocalDateTime.of(2026, 10, 9, 0, 0));
            o.setLongitude(new BigDecimal("114.057900"));
            o.setLatitude(new BigDecimal("22.543100"));
        });
        clockMapper.insert(clock);

        attendanceService.deleteClock(clock.getId());

        assertNull(clockMapper.selectById(clock.getId()));
    }

    @Test
    public void test_getClockShift_noGroup_returnsNull() {
        HrEmployeeDO employee = insertEmployee();
        HrmAttendanceClockShiftRespVO shift = attendanceService.getClockShift(employee.getId(), "2026-10-09");
        assertNull(shift);
    }

    @Test
    public void test_getClockShift_withGroup_matchesWeekday() {
        HrEmployeeDO employee = insertEmployee();
        // 考勤组：周一~周五（1-5），班次 09:00-18:00
        Map<String, Object> shiftConfig = new HashMap<>();
        shiftConfig.put("weeks", Arrays.asList(1, 2, 3, 4, 5));
        shiftConfig.put("startTime", "09:00");
        shiftConfig.put("endTime", "18:00");
        shiftConfig.put("clockInStartTime", "08:00");
        // 注：Map 值统一用可 JSON 序列化的标量/列表
        shiftConfig.put("clockInEndTime", "09:30");
        shiftConfig.put("clockOutStartTime", "17:30");
        shiftConfig.put("clockOutEndTime", "19:00");
        HrmAttendanceGroupDO group = randomPojo(HrmAttendanceGroupDO.class, o -> {
            o.setName("标准考勤组");
            o.setShifts(Arrays.asList(shiftConfig));
            o.setEmployeeIds(Arrays.asList(employee.getId()));
            o.setDefaultStatus(true);
            // json 列显式赋 null：randomPojo 的随机 Object 值 Jackson 无法序列化
            o.setSpecialDates(null);
            o.setDeptIds(null);
            o.setPoints(null);
            o.setWifis(null);
            o.setDeductRule(null);
        });
        groupMapper.insert(group);
        // 关联员工（考勤组-员工关联表）
        groupEmployeeMapper.insert(HrmAttendanceGroupEmployeeDO.builder()
                .groupId(group.getId()).employeeId(employee.getId()).build());

        // 2026-10-09 是周五（weekday=5）→ 命中班次
        HrmAttendanceClockShiftRespVO shift = attendanceService.getClockShift(employee.getId(), "2026-10-09");
        assertNotNull(shift);
        assertEquals(LocalDateTime.of(2026, 10, 9, 9, 0), shift.getStartTime());
        assertEquals(LocalDateTime.of(2026, 10, 9, 18, 0), shift.getEndTime());
        assertEquals(LocalDateTime.of(2026, 10, 9, 8, 0), shift.getClockInStartTime());

        // 2026-10-11 是周日（weekday=7）→ 不命中
        assertNull(attendanceService.getClockShift(employee.getId(), "2026-10-11"));
    }

    @Test
    public void test_getMonthRecord_aggregatesClocksAndDeduct() {
        HrEmployeeDO employee = insertEmployee();
        // 考勤组：周一~周五，09:00-18:00，扣款：迟到按次数 100/次，缺卡按次数 50/次
        Map<String, Object> shiftConfig = new HashMap<>();
        shiftConfig.put("weeks", Arrays.asList(1, 2, 3, 4, 5));
        shiftConfig.put("startTime", "09:00");
        shiftConfig.put("endTime", "18:00");
        Map<String, Object> deductRule = new HashMap<>();
        deductRule.put("lateMethod", 3);
        deductRule.put("lateDeductMoney", 100);
        deductRule.put("earlyMethod", 3);
        deductRule.put("earlyDeductMoney", 100);
        deductRule.put("misscardMethod", 3);
        deductRule.put("misscardDeductMoney", 50);
        deductRule.put("absenteeismMethod", 1);
        deductRule.put("absenteeismDeductMoney", 200);
        HrmAttendanceGroupDO group = randomPojo(HrmAttendanceGroupDO.class, o -> {
            o.setName("扣款考勤组");
            o.setShifts(Arrays.asList(shiftConfig));
            o.setDeductRule(deductRule);
            o.setDefaultStatus(true);
            o.setSpecialDates(null);
            o.setDeptIds(null);
            o.setEmployeeIds(null);
            o.setPoints(null);
            o.setWifis(null);
        });
        groupMapper.insert(group);

        // 3 次打卡：1 正常、1 迟到、1 早退（均在 2026-10 月）
        insertClock(employee.getId(), LocalDateTime.of(2026, 10, 1, 8, 55), 1);
        insertClock(employee.getId(), LocalDateTime.of(2026, 10, 2, 9, 30), 2); // 迟到 30 分钟
        insertClock(employee.getId(), LocalDateTime.of(2026, 10, 5, 17, 30), 3); // 早退 30 分钟

        HrmAttendanceMonthRecordPageReqVO reqVO = new HrmAttendanceMonthRecordPageReqVO();
        reqVO.setYear(2026);
        reqVO.setMonth(10);
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        var page = attendanceService.getMonthRecordPage(reqVO);
        assertEquals(1, page.getTotal());
        HrmAttendanceMonthRecordRespVO vo = page.getList().get(0);
        assertEquals(1, vo.getLateCount());
        assertEquals(30, vo.getLateMinute());
        assertEquals(1, vo.getEarlyCount());
        assertEquals(30, vo.getEarlyMinute());
        // 扣款：迟到按次数 1*100 + 早退 1*100 = 200
        assertEquals(0, new BigDecimal("200.00").compareTo(vo.getLateDeductAmount().add(vo.getEarlyDeductAmount())));
        // 应出勤天数 = 2026-10 月工作日（周一~周五）22 天
        assertEquals(0, new BigDecimal("22.00").compareTo(vo.getAttendDays()));
        // 汇总行落库（幂等缓存）
        assertNotNull(monthRecordMapper.selectByEmployeeAndMonth(employee.getId(), 2026, 10));
    }

    @Test
    public void test_createHoliday_duplicateDate() {
        HrmAttendanceHolidaySaveReqVO reqVO = new HrmAttendanceHolidaySaveReqVO();
        reqVO.setDate(LocalDate.of(2026, 10, 1));
        reqVO.setType(2);
        attendanceService.createHoliday(reqVO);

        HrmAttendanceHolidaySaveReqVO dup = new HrmAttendanceHolidaySaveReqVO();
        dup.setDate(LocalDate.of(2026, 10, 1));
        dup.setType(2);
        cn.zhicloud.framework.common.exception.ServiceException ex =
                assertThrows(cn.zhicloud.framework.common.exception.ServiceException.class,
                        () -> attendanceService.createHoliday(dup));
        assertEquals(1_050_016_004, ex.getCode());
    }

    @Test
    public void test_leavePage_enrichesEmployee() {
        HrEmployeeDO employee = insertEmployee();
        HrmAttendanceLeaveDO leave = randomPojo(HrmAttendanceLeaveDO.class, o -> {
            o.setEmployeeId(employee.getId());
            o.setType("ANNUAL");
            o.setStartTime(LocalDateTime.of(2026, 10, 6, 9, 0));
            o.setEndTime(LocalDateTime.of(2026, 10, 7, 18, 0));
            o.setDay(new BigDecimal("2.00"));
        });
        leaveMapper.insert(leave);

        HrmAttendanceLeavePageReqVO reqVO = new HrmAttendanceLeavePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        var page = attendanceService.getLeavePage(reqVO);
        assertEquals(1, page.getTotal());
        assertEquals("考勤张三", page.getList().get(0).getEmployeeName());
        assertEquals("ATT001", page.getList().get(0).getJobNumber());
    }

    private void insertClock(Long employeeId, LocalDateTime clockTime, int status) {
        HrmAttendanceClockDO clock = randomPojo(HrmAttendanceClockDO.class, o -> {
            o.setEmployeeId(employeeId);
            o.setType(1);
            o.setAttendanceTime(clockTime.toLocalDate().atStartOfDay());
            o.setClockTime(clockTime);
            o.setStatus(status);
            // 经纬度列 DECIMAL(10,6)：随机值会溢出，显式给合规小值
            o.setLongitude(new BigDecimal("114.057900"));
            o.setLatitude(new BigDecimal("22.543100"));
        });
        clockMapper.insert(clock);
    }

}
