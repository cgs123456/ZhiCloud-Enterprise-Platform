package cn.zhicloud.module.hr.service.attendance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.hr.controller.admin.attendance.vo.HrAttendanceMonthlySummaryRespVO;
import cn.zhicloud.module.hr.controller.admin.attendance.vo.HrAttendancePageReqVO;
import cn.zhicloud.module.hr.controller.admin.attendance.vo.HrAttendanceSaveReqVO;
import cn.zhicloud.module.hr.dal.dataobject.attendance.HrAttendanceDO;
import cn.zhicloud.module.hr.dal.mysql.attendance.HrAttendanceMapper;
import cn.zhicloud.module.hr.enums.attendance.HrAttendanceStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.hr.enums.ErrorCodeConstants.HR_ATTENDANCE_NOT_EXISTS;

/**
 * HR 考勤记录 Service 实现类
 *
 * @author zhicloud
 */
@Service
@Validated
public class HrAttendanceServiceImpl implements HrAttendanceService {

    @Resource
    private HrAttendanceMapper attendanceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAttendance(HrAttendanceSaveReqVO createReqVO) {
        // 幂等：同一员工同一日期只允许一条考勤记录，已存在则更新
        HrAttendanceDO existing = attendanceMapper.selectByEmployeeAndDate(
                createReqVO.getEmployeeId(), createReqVO.getAttendanceDate());
        if (existing != null) {
            HrAttendanceDO updateObj = BeanUtils.toBean(createReqVO, HrAttendanceDO.class);
            updateObj.setId(existing.getId());
            attendanceMapper.updateById(updateObj);
            return existing.getId();
        }
        HrAttendanceDO attendance = BeanUtils.toBean(createReqVO, HrAttendanceDO.class);
        attendanceMapper.insert(attendance);
        return attendance.getId();
    }

    @Override
    public void updateAttendance(HrAttendanceSaveReqVO updateReqVO) {
        // 校验存在
        validateAttendanceExists(updateReqVO.getId());
        // 更新
        HrAttendanceDO updateObj = BeanUtils.toBean(updateReqVO, HrAttendanceDO.class);
        attendanceMapper.updateById(updateObj);
    }

    @Override
    public void deleteAttendance(Long id) {
        // 校验存在
        validateAttendanceExists(id);
        // 删除
        attendanceMapper.deleteById(id);
    }

    private void validateAttendanceExists(Long id) {
        if (attendanceMapper.selectById(id) == null) {
            throw exception(HR_ATTENDANCE_NOT_EXISTS);
        }
    }

    @Override
    public HrAttendanceDO getAttendance(Long id) {
        return attendanceMapper.selectById(id);
    }

    @Override
    public PageResult<HrAttendanceDO> getAttendancePage(HrAttendancePageReqVO pageReqVO) {
        return attendanceMapper.selectPage(pageReqVO);
    }

    @Override
    public HrAttendanceMonthlySummaryRespVO getMonthlySummary(Long employeeId, String month) {
        // 解析月份，计算该月起止日期
        YearMonth yearMonth = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyyMM"));
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        // 查询该月所有考勤记录
        List<HrAttendanceDO> list = attendanceMapper.selectListByEmployeeAndDateRange(employeeId, startDate, endDate);
        // 汇总
        HrAttendanceMonthlySummaryRespVO summary = new HrAttendanceMonthlySummaryRespVO();
        summary.setEmployeeId(employeeId);
        summary.setMonth(month);
        summary.setTotalDays(list.size());
        summary.setNormalDays((int) list.stream().filter(a -> HrAttendanceStatusEnum.NORMAL.getStatus().equals(a.getStatus())).count());
        summary.setLateDays((int) list.stream().filter(a -> HrAttendanceStatusEnum.LATE.getStatus().equals(a.getStatus())).count());
        summary.setEarlyLeaveDays((int) list.stream().filter(a -> HrAttendanceStatusEnum.EARLY_LEAVE.getStatus().equals(a.getStatus())).count());
        summary.setAbsentDays((int) list.stream().filter(a -> HrAttendanceStatusEnum.ABSENT.getStatus().equals(a.getStatus())).count());
        summary.setOvertimeDays((int) list.stream().filter(a -> HrAttendanceStatusEnum.OVERTIME.getStatus().equals(a.getStatus())).count());
        summary.setTotalOvertimeHours(list.stream()
                .map(HrAttendanceDO::getOvertimeHours)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return summary;
    }

    @Override
    public List<HrAttendanceDO> getAttendanceListByEmployeeAndDateRange(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return attendanceMapper.selectListByEmployeeAndDateRange(employeeId, startDate, endDate);
    }

}