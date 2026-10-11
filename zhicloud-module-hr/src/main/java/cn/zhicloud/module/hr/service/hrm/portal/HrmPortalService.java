package cn.zhicloud.module.hr.service.hrm.portal;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.hr.controller.admin.hrm.portal.vo.*;
import cn.zhicloud.module.hr.controller.admin.hrm.vo.*;

import java.util.List;
import java.util.Map;

/**
 * HRM 员工自助端（portal）Service 接口
 *
 * 所有接口以「当前登录用户绑定的员工」为数据边界；未绑定员工时抛
 * {@code HRM_PORTAL_EMPLOYEE_NOT_BOUND}。
 *
 * @author zhicloud
 */
public interface HrmPortalService {

    // ========== 员工档案 ==========

    /**
     * 当前登录用户是否已绑定员工档案
     */
    boolean getBindStatus();

    /**
     * 获得当前员工档案
     */
    HrmPortalEmployeeVO getEmployee();

    /**
     * 修改当前员工档案（仅可自助编辑字段）
     */
    void updateEmployee(HrmPortalEmployeeUpdateReqVO reqVO);

    // ========== 考勤 ==========

    /**
     * 获得当前员工打卡记录列表
     */
    List<HrmPortalAttendanceClockVO> getClockList(String startDate, String endDate);

    /**
     * 获得当前员工请假申请列表
     */
    List<HrmPortalAttendanceLeaveVO> getLeaveList(Integer year, Integer month);

    /**
     * 创建当前员工请假申请
     */
    Long createLeave(HrmPortalAttendanceLeaveCreateReqVO reqVO);

    /**
     * 取消当前员工请假申请
     */
    void cancelLeave(HrmPortalAttendanceLeaveCancelReqVO reqVO);

    /**
     * 回写请假单的 BPM 审批结果（供流程状态监听器调用；幂等，仅从「审批中」落一次）
     */
    void updateLeaveBpmResult(Long id, Integer bpmStatus, String processInstanceId);

    /**
     * 获得当前员工月度考勤详情（summary/dailyDetails/leaves）
     */
    Map<String, Object> getAttendanceMonthDetail(Integer year, Integer month);

    /**
     * 获得当前员工月度考勤导出列表
     */
    List<HrmPortalAttendanceMonthExportExcelVO> getAttendanceMonthExportList(Integer year, Integer month);

    // ========== 社保 ==========

    /**
     * 获得当前员工社保参保记录列表
     */
    List<HrmPortalInsuranceRecordVO> getInsuranceRecordList(Integer year);

    /**
     * 获得当前员工社保参保记录详情
     */
    HrmPortalInsuranceRecordVO getInsuranceRecord(Integer year, Integer month);

    // ========== 绩效 ==========

    /**
     * 获得当前员工绩效考核分页
     */
    PageResult<HrmPortalPerformanceAssessmentVO> getAssessmentPage(HrmPortalPerformanceAssessmentPageReqVO reqVO);

    /**
     * 获得当前员工绩效任务数量
     */
    HrmPortalPerformanceTaskCountVO getAssessmentTaskCount(String search);

    /**
     * 获得当前员工绩效考核详情
     */
    HrmPortalPerformanceAssessmentVO getAssessment(Long id, Long stageId);

    /**
     * 获得当前员工绩效流程记录列表
     */
    List<HrmPortalPerformanceProcessRecordVO> getProcessRecordList(Long id, Long stageId);

    /**
     * 获得指标填写任务分页
     */
    PageResult<HrmPortalPerformanceAssessmentVO> getFillQuotaTaskPage(HrmPortalPerformanceAssessmentPageReqVO reqVO);

    /**
     * 获得目标确认任务分页
     */
    PageResult<HrmPortalPerformanceAssessmentVO> getTargetConfirmationTaskPage(HrmPortalPerformanceAssessmentPageReqVO reqVO);

    /**
     * 获得评分任务分页
     */
    PageResult<HrmPortalPerformanceAssessmentVO> getReviewTaskPage(HrmPortalPerformanceAssessmentPageReqVO reqVO);

    /**
     * 获得结果审核任务分页
     */
    PageResult<HrmPortalPerformanceAssessmentVO> getResultAuditTaskPage(HrmPortalPerformanceAssessmentPageReqVO reqVO);

    /**
     * 获得结果确认任务分页
     */
    PageResult<HrmPortalPerformanceAssessmentVO> getResultConfirmationTaskPage(HrmPortalPerformanceAssessmentPageReqVO reqVO);

    /**
     * 获得申诉处理任务分页
     */
    PageResult<HrmPortalPerformanceAssessmentVO> getAppealTaskPage(HrmPortalPerformanceAssessmentPageReqVO reqVO);

    /**
     * 填写绩效指标
     */
    void fillQuota(HrmPortalPerformanceQuotaFillReqVO reqVO);

    /**
     * 确认绩效目标
     */
    void confirmTarget(HrmPortalPerformanceConfirmReqVO reqVO);

    /**
     * 预览绩效评分
     */
    Object previewScore(HrmPortalPerformanceScoreReqVO reqVO);

    /**
     * 提交绩效评分
     */
    void score(HrmPortalPerformanceScoreReqVO reqVO);

    /**
     * 驳回评分阶段
     */
    void rejectReviewStage(HrmPortalPerformanceRejectReqVO reqVO);

    /**
     * 处理绩效结果审核
     */
    void handleResultAudit(HrmPortalPerformanceHandleStageReqVO reqVO);

    /**
     * 确认绩效结果
     */
    void confirmResult(HrmPortalPerformanceConfirmReqVO reqVO);

    /**
     * 提交绩效申诉
     */
    void submitAppeal(HrmPortalPerformanceAppealReqVO reqVO);

    /**
     * 处理绩效申诉
     */
    void handleAppeal(HrmPortalPerformanceHandleStageReqVO reqVO);

    // ========== 工资条 ==========

    /**
     * 获得当前员工工资条分页
     */
    PageResult<HrmPortalSalarySlipVO> getSalarySlipPage(Integer pageNo, Integer pageSize, String startMonth, String endMonth);

    /**
     * 标记当前员工工资条已读
     */
    void readSalarySlip(Long id);

    /**
     * 获得当前员工未读工资条概况
     */
    HrmPortalSalarySlipUnreadSummaryVO getSalarySlipUnreadSummary();

    // ========== 首页日历 ==========

    /**
     * 获得当前员工首页日历
     */
    List<HrmPortalCalendarItemVO> getHomeCalendar(String startDate, String endDate);

    // ========== 员工子档案 ==========

    /**
     * 获得当前员工证书列表
     */
    List<HrmEmployeeCertificateVO> getCertificateList();

    /**
     * 获得当前员工紧急联系人列表
     */
    List<HrmEmployeeContactVO> getContactList();

    /**
     * 获得当前员工教育经历列表
     */
    List<HrmEmployeeEducationExperienceVO> getEducationExperienceList();

    /**
     * 获得当前员工工作经历列表
     */
    List<HrmEmployeeWorkExperienceVO> getWorkExperienceList();

    /**
     * 获得当前员工培训经历列表
     */
    List<HrmEmployeeTrainingExperienceVO> getTrainingExperienceList();

    /**
     * 获得当前员工离职信息
     */
    HrmEmployeeQuitInfoRespVO getQuitInfo();

    /**
     * 获得当前员工档案字段配置
     */
    List<HrmEmployeeFieldConfigRespVO> getFieldConfigList();

}
