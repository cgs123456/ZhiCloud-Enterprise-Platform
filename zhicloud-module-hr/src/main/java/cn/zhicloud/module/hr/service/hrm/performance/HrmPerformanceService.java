package cn.zhicloud.module.hr.service.hrm.performance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.hr.controller.admin.hrm.performance.vo.*;

import java.util.Collection;
import java.util.List;

/**
 * HRM 绩效域 Service 接口（考核模板/结果模板/绩效计划/员工考核/流程记录）
 *
 * @author zhicloud
 */
public interface HrmPerformanceService {

    // ========== 考核模板 ==========

    PageResult<HrmPerformanceAssessmentTemplateVO> getAssessmentTemplatePage(HrmPerformanceTemplatePageReqVO reqVO);

    HrmPerformanceAssessmentTemplateVO getAssessmentTemplate(Long id);

    List<HrmPerformanceAssessmentTemplateVO> getAssessmentTemplateSimpleList();

    Long createAssessmentTemplate(HrmPerformanceAssessmentTemplateVO reqVO);

    void updateAssessmentTemplate(HrmPerformanceAssessmentTemplateVO reqVO);

    void deleteAssessmentTemplate(Long id);

    void deleteAssessmentTemplateList(Collection<Long> ids);

    // ========== 结果模板 ==========

    PageResult<HrmPerformanceResultTemplateVO> getResultTemplatePage(HrmPerformanceTemplatePageReqVO reqVO);

    HrmPerformanceResultTemplateVO getResultTemplate(Long id);

    List<HrmPerformanceResultTemplateVO> getResultTemplateSimpleList();

    Long createResultTemplate(HrmPerformanceResultTemplateVO reqVO);

    void updateResultTemplate(HrmPerformanceResultTemplateVO reqVO);

    void deleteResultTemplate(Long id);

    void deleteResultTemplateList(Collection<Long> ids);

    // ========== 绩效计划 ==========

    PageResult<HrmPerformancePlanVO> getPlanPage(HrmPerformancePlanPageReqVO reqVO);

    HrmPerformancePlanVO getPlan(Long id);

    List<HrmPerformancePlanStatusCountRespVO> getPlanStatusCount();

    List<HrmPerformancePlanStageCountRespVO> getPlanStageCount(Long planId);

    List<HrmPerformancePlanLevelCountRespVO> getPlanLevelCount(Long planId);

    Long createPlan(HrmPerformancePlanVO reqVO);

    void updatePlan(HrmPerformancePlanVO reqVO);

    void deletePlan(Long id);

    /**
     * 开始绩效计划：草稿/未开始 → 进行中，按考评范围生成员工考核
     */
    void startPlan(Long id);

    /**
     * 开启评分：推进到自评阶段
     */
    void openScoring(Long id);

    /**
     * 发起面谈
     */
    void startInterview(Long id);

    /**
     * 归档计划：进行中 → 已归档
     */
    void archivePlan(Long id);

    /**
     * 终止计划：→ 已终止
     */
    void terminatePlan(Long id);

    // ========== 员工考核 ==========

    PageResult<HrmPerformanceAssessmentVO> getAssessmentPage(HrmPerformanceAssessmentPageReqVO reqVO);

    HrmPerformanceAssessmentVO getAssessment(Long id);

    List<Long> getUnassignedEmployeeIdList(Long planId);

    Long createAssessmentList(HrmPerformanceAssessmentCreateListReqVO reqVO);

    void deleteAssessmentList(Collection<Long> ids);

    List<HrmPerformanceProcessRecordVO> getProcessRecordList(Long id);

    // ========== 归档 ==========

    PageResult<HrmPerformanceAssessmentVO> getArchivePage(HrmPerformanceAssessmentPageReqVO reqVO);

    /**
     * 获得员工绩效档案分页（归档员工维度聚合：每员工一条，含最近考核 + 考核次数）
     */
    PageResult<HrmPerformanceArchiveEmployeeVO> getArchiveEmployeePage(HrmPerformanceAssessmentPageReqVO reqVO);

    HrmPerformanceAssessmentVO getArchive(Long id);

    List<HrmPerformancePlanVO> getArchivePlanSimpleList();

    void deleteArchive(Long id);

    void deleteArchiveEmployee(Long id);

    List<HrmPerformanceProcessRecordVO> getArchiveProcessRecordList(Long id);

}
