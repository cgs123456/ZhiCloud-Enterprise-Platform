package cn.iocoder.yudao.module.hr.service.attendance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.hr.controller.admin.attendance.vo.HrAttendanceMonthlySummaryRespVO;
import cn.iocoder.yudao.module.hr.controller.admin.attendance.vo.HrAttendancePageReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.attendance.vo.HrAttendanceSaveReqVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.attendance.HrAttendanceDO;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

/**
 * HR 考勤记录 Service 接口
 *
 * @author yudao
 */
public interface HrAttendanceService {

    /**
     * 创建考勤记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createAttendance(@Valid HrAttendanceSaveReqVO createReqVO);

    /**
     * 更新考勤记录
     *
     * @param updateReqVO 更新信息
     */
    void updateAttendance(@Valid HrAttendanceSaveReqVO updateReqVO);

    /**
     * 删除考勤记录
     *
     * @param id 编号
     */
    void deleteAttendance(Long id);

    /**
     * 获得考勤记录
     *
     * @param id 编号
     * @return 考勤记录
     */
    HrAttendanceDO getAttendance(Long id);

    /**
     * 获得考勤记录分页
     *
     * @param pageReqVO 分页查询
     * @return 考勤记录分页
     */
    PageResult<HrAttendanceDO> getAttendancePage(HrAttendancePageReqVO pageReqVO);

    /**
     * 获得员工月度考勤汇总
     *
     * @param employeeId 员工编号
     * @param month 月份（yyyyMM）
     * @return 月度汇总
     */
    HrAttendanceMonthlySummaryRespVO getMonthlySummary(Long employeeId, String month);

    /**
     * 获得员工指定日期范围的考勤记录
     *
     * @param employeeId 员工编号
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 考勤记录列表
     */
    List<HrAttendanceDO> getAttendanceListByEmployeeAndDateRange(Long employeeId, LocalDate startDate, LocalDate endDate);

}