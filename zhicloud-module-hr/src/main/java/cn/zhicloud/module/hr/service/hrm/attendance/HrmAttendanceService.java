package cn.zhicloud.module.hr.service.hrm.attendance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.hr.controller.admin.hrm.attendance.vo.*;

import java.util.Collection;
import java.util.List;

/**
 * HRM 考勤 Service 接口（打卡/考勤组/节假日/请假/统计）
 *
 * @author zhicloud
 */
public interface HrmAttendanceService {

    // ========== 打卡 ==========

    PageResult<HrmAttendanceClockRespVO> getClockPage(HrmAttendanceClockPageReqVO reqVO);

    HrmAttendanceClockRespVO getClock(Long id);

    /**
     * 获得员工某日实际班次与允许打卡时间（无排班时返回 null）
     */
    HrmAttendanceClockShiftRespVO getClockShift(Long employeeId, String attendanceTime);

    Long createClock(HrmAttendanceClockSaveReqVO reqVO);

    void updateClock(HrmAttendanceClockSaveReqVO reqVO);

    void deleteClock(Long id);

    void deleteClockList(Collection<Long> ids);

    List<HrmAttendanceClockExportExcelVO> getClockExportList(HrmAttendanceClockPageReqVO reqVO);

    // ========== 考勤组 ==========

    PageResult<HrmAttendanceGroupRespVO> getGroupPage(Integer pageNo, Integer pageSize, String name);

    HrmAttendanceGroupRespVO getGroup(Long id);

    /**
     * 获得员工所在考勤组（优先员工关联，回退默认组）
     */
    HrmAttendanceGroupRespVO getMyGroup(Long employeeId);

    Long createGroup(HrmAttendanceGroupSaveReqVO reqVO);

    void updateGroup(HrmAttendanceGroupSaveReqVO reqVO);

    void deleteGroup(Long id);

    // ========== 节假日 ==========

    PageResult<HrmAttendanceHolidayRespVO> getHolidayPage(HrmAttendanceHolidayPageReqVO reqVO);

    HrmAttendanceHolidayRespVO getHoliday(Long id);

    Long createHoliday(HrmAttendanceHolidaySaveReqVO reqVO);

    void updateHoliday(HrmAttendanceHolidaySaveReqVO reqVO);

    void deleteHoliday(Long id);

    // ========== 请假（读侧；写侧在 portal 域） ==========

    PageResult<HrmAttendanceLeaveRespVO> getLeavePage(HrmAttendanceLeavePageReqVO reqVO);

    HrmAttendanceLeaveRespVO getLeave(Long id);

    List<HrmAttendanceLeaveExportExcelVO> getLeaveExportList(HrmAttendanceLeavePageReqVO reqVO);

    // ========== 统计 ==========

    /**
     * 月度考勤汇总分页（缺汇总行时按员工实时汇总并落库）
     */
    PageResult<HrmAttendanceMonthRecordRespVO> getMonthRecordPage(HrmAttendanceMonthRecordPageReqVO reqVO);

    /**
     * 月度每日打卡概况分页
     */
    PageResult<HrmAttendanceMonthDailyOverviewRespVO> getMonthDailyPage(HrmAttendanceMonthRecordPageReqVO reqVO);

    /**
     * 月度考勤详情（汇总 + 每日明细 + 请假）
     */
    HrmAttendanceMonthDetailRespVO getMonthDetail(Long employeeId, Integer year, Integer month);

    /**
     * 某员工某日考勤明细
     */
    HrmAttendanceDailyDetailRespVO getDailyDetail(Long employeeId, String date);

    List<HrmAttendanceMonthRecordExportExcelVO> getMonthRecordExportList(HrmAttendanceMonthRecordPageReqVO reqVO);

    List<HrmAttendanceMonthDailyExportExcelVO> getMonthDailyExportList(HrmAttendanceMonthRecordPageReqVO reqVO);

}
