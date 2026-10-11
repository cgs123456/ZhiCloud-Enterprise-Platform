package cn.zhicloud.module.hr.service.hrm.performance;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.hr.controller.admin.hrm.performance.vo.*;
import cn.zhicloud.module.hr.dal.dataobject.employee.HrEmployeeDO;
import cn.zhicloud.module.hr.dal.dataobject.performance.*;
import cn.zhicloud.module.hr.dal.mysql.employee.HrEmployeeMapper;
import cn.zhicloud.module.hr.dal.mysql.performance.*;
import cn.zhicloud.module.system.api.dept.DeptApi;
import cn.zhicloud.module.system.api.dept.dto.DeptRespDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.hr.enums.ErrorCodeConstants.*;

/**
 * HRM 绩效域 Service 实现类
 *
 * 口径（对齐前端 views/hrm/performance）：
 * * 模板/结果模板：name 唯一（服务层校验）；模板 dimensions/levels 以 json 整体存取。
 * * 计划生命周期：草稿(1)/未开始(2) → 开始 startPlan → 进行中(3) → 归档 archivePlan → 已归档(4)；terminate → 已终止(5)。
 *   开始计划时按 scopes（考评范围）解析员工并生成员工考核（一计划一员工一条，幂等）。
 * * 状态计数：计划按 status 五档；阶段计数/等级计数按计划下员工考核的 stage_type/result_level 聚合。
 * * 归档：仅已归档计划（status=4）的考核可查；archive-employee-delete 单条删除归档考核。
 * * 流程记录：biz_type 1计划 2员工考核，按 biz 记录操作（模板/计划动作落记录）。
 *
 * @author zhicloud
 */
@Slf4j
@Service
@Validated
public class HrmPerformanceServiceImpl implements HrmPerformanceService {

    private static final int PLAN_STATUS_DRAFT = 1;
    private static final int PLAN_STATUS_RUNNING = 3;
    private static final int PLAN_STATUS_ARCHIVED = 4;
    private static final int PLAN_STATUS_TERMINATED = 5;
    private static final int ASSESSMENT_STATUS_DRAFT = 1;
    private static final int BIZ_TYPE_PLAN = 1;
    private static final int BIZ_TYPE_ASSESSMENT = 2;

    @Resource
    private HrmPerformanceAssessmentTemplateMapper hrmAssessmentTemplateMapper;
    @Resource
    private HrmPerformanceResultTemplateMapper hrmResultTemplateMapper;
    @Resource
    private HrmPerformancePlanMapper hrmPlanMapper;
    @Resource
    private HrmPerformanceAssessmentMapper hrmAssessmentMapper;
    @Resource
    private HrmPerformanceAssessmentDimensionMapper hrmDimensionMapper;
    @Resource
    private HrmPerformanceAssessmentQuotaMapper hrmQuotaMapper;
    @Resource
    private HrmPerformanceAssessmentStageMapper hrmStageMapper;
    @Resource
    private HrmPerformanceProcessRecordMapper hrmRecordMapper;
    @Resource
    private HrEmployeeMapper hrmEmployeeMapper;
    @Resource
    private DeptApi deptApi;

    // ========== 考核模板 ==========

    @Override
    public PageResult<HrmPerformanceAssessmentTemplateVO> getAssessmentTemplatePage(HrmPerformanceTemplatePageReqVO reqVO) {
        PageResult<HrmPerformanceAssessmentTemplateDO> pageResult = hrmAssessmentTemplateMapper.selectPage(reqVO);
        return new PageResult<>(enrichTemplates(pageResult.getList()), pageResult.getTotal());
    }

    @Override
    public HrmPerformanceAssessmentTemplateVO getAssessmentTemplate(Long id) {
        return enrichTemplates(Collections.singletonList(validateAssessmentTemplateExists(id))).get(0);
    }

    @Override
    public List<HrmPerformanceAssessmentTemplateVO> getAssessmentTemplateSimpleList() {
        return enrichTemplates(hrmAssessmentTemplateMapper.selectListAll());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAssessmentTemplate(HrmPerformanceAssessmentTemplateVO reqVO) {
        validateTemplateNameUnique(hrmAssessmentTemplateMapper.selectByName(reqVO.getName()), null, reqVO.getName(), true);
        HrmPerformanceAssessmentTemplateDO template = BeanUtils.toBean(reqVO, HrmPerformanceAssessmentTemplateDO.class);
        hrmAssessmentTemplateMapper.insert(template);
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAssessmentTemplate(HrmPerformanceAssessmentTemplateVO reqVO) {
        validateAssessmentTemplateExists(reqVO.getId());
        HrmPerformanceAssessmentTemplateDO exist = hrmAssessmentTemplateMapper.selectByName(reqVO.getName());
        validateTemplateNameUnique(exist, reqVO.getId(), reqVO.getName(), true);
        hrmAssessmentTemplateMapper.updateById(BeanUtils.toBean(reqVO, HrmPerformanceAssessmentTemplateDO.class));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAssessmentTemplate(Long id) {
        validateAssessmentTemplateExists(id);
        hrmAssessmentTemplateMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAssessmentTemplateList(Collection<Long> ids) {
        if (CollUtil.isNotEmpty(ids)) {
            hrmAssessmentTemplateMapper.deleteByIds(ids);
        }
    }

    // ========== 结果模板 ==========

    @Override
    public PageResult<HrmPerformanceResultTemplateVO> getResultTemplatePage(HrmPerformanceTemplatePageReqVO reqVO) {
        PageResult<HrmPerformanceResultTemplateDO> pageResult = hrmResultTemplateMapper.selectPage(reqVO);
        return new PageResult<>(BeanUtils.toBean(pageResult.getList(), HrmPerformanceResultTemplateVO.class), pageResult.getTotal());
    }

    @Override
    public HrmPerformanceResultTemplateVO getResultTemplate(Long id) {
        return BeanUtils.toBean(validateResultTemplateExists(id), HrmPerformanceResultTemplateVO.class);
    }

    @Override
    public List<HrmPerformanceResultTemplateVO> getResultTemplateSimpleList() {
        return BeanUtils.toBean(hrmResultTemplateMapper.selectListAll(), HrmPerformanceResultTemplateVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createResultTemplate(HrmPerformanceResultTemplateVO reqVO) {
        validateTemplateNameUnique(hrmResultTemplateMapper.selectByName(reqVO.getName()), null, reqVO.getName(), false);
        HrmPerformanceResultTemplateDO template = BeanUtils.toBean(reqVO, HrmPerformanceResultTemplateDO.class);
        if (template.getStatus() == null) {
            template.setStatus(0);
        }
        hrmResultTemplateMapper.insert(template);
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateResultTemplate(HrmPerformanceResultTemplateVO reqVO) {
        validateResultTemplateExists(reqVO.getId());
        HrmPerformanceResultTemplateDO exist = hrmResultTemplateMapper.selectByName(reqVO.getName());
        validateTemplateNameUnique(exist, reqVO.getId(), reqVO.getName(), false);
        hrmResultTemplateMapper.updateById(BeanUtils.toBean(reqVO, HrmPerformanceResultTemplateDO.class));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteResultTemplate(Long id) {
        validateResultTemplateExists(id);
        hrmResultTemplateMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteResultTemplateList(Collection<Long> ids) {
        if (CollUtil.isNotEmpty(ids)) {
            hrmResultTemplateMapper.deleteByIds(ids);
        }
    }

    // ========== 绩效计划 ==========

    @Override
    public PageResult<HrmPerformancePlanVO> getPlanPage(HrmPerformancePlanPageReqVO reqVO) {
        PageResult<HrmPerformancePlanDO> pageResult = hrmPlanMapper.selectPage(reqVO);
        return new PageResult<>(enrichPlans(pageResult.getList()), pageResult.getTotal());
    }

    @Override
    public HrmPerformancePlanVO getPlan(Long id) {
        return enrichPlans(Collections.singletonList(validatePlanExists(id))).get(0);
    }

    @Override
    public List<HrmPerformancePlanStatusCountRespVO> getPlanStatusCount() {
        Map<Integer, Long> map = hrmPlanMapper.selectList(new LambdaQueryWrapper<>()).stream()
                .map(HrmPerformancePlanDO::getStatus).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        List<HrmPerformancePlanStatusCountRespVO> result = new ArrayList<>(5);
        for (int status = 1; status <= 5; status++) {
            HrmPerformancePlanStatusCountRespVO vo = new HrmPerformancePlanStatusCountRespVO();
            vo.setStatus(status);
            vo.setCount(map.getOrDefault(status, 0L));
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<HrmPerformancePlanStageCountRespVO> getPlanStageCount(Long planId) {
        validatePlanExists(planId);
        Map<Integer, Long> map = hrmAssessmentMapper.selectListByPlanId(planId).stream()
                .map(HrmPerformanceAssessmentDO::getStageType).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        List<HrmPerformancePlanStageCountRespVO> result = new ArrayList<>();
        map.forEach((stageType, count) -> {
            HrmPerformancePlanStageCountRespVO vo = new HrmPerformancePlanStageCountRespVO();
            vo.setStageType(stageType);
            vo.setCount(count);
            result.add(vo);
        });
        result.sort(Comparator.comparing(HrmPerformancePlanStageCountRespVO::getStageType));
        return result;
    }

    @Override
    public List<HrmPerformancePlanLevelCountRespVO> getPlanLevelCount(Long planId) {
        validatePlanExists(planId);
        Map<String, Long> map = hrmAssessmentMapper.selectListByPlanId(planId).stream()
                .map(HrmPerformanceAssessmentDO::getResultLevel).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(l -> l, Collectors.counting()));
        List<HrmPerformancePlanLevelCountRespVO> result = new ArrayList<>();
        map.forEach((level, count) -> {
            HrmPerformancePlanLevelCountRespVO vo = new HrmPerformancePlanLevelCountRespVO();
            vo.setLevel(level);
            vo.setCount(count);
            result.add(vo);
        });
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPlan(HrmPerformancePlanVO reqVO) {
        HrmPerformancePlanDO plan = BeanUtils.toBean(reqVO, HrmPerformancePlanDO.class);
        if (plan.getStatus() == null) {
            plan.setStatus(PLAN_STATUS_DRAFT);
        }
        hrmPlanMapper.insert(plan);
        recordProcess(BIZ_TYPE_PLAN, plan.getId(), "创建绩效计划", "创建计划：" + plan.getName(), "ACTION");
        return plan.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePlan(HrmPerformancePlanVO reqVO) {
        HrmPerformancePlanDO exist = validatePlanExists(reqVO.getId());
        if (Integer.valueOf(PLAN_STATUS_ARCHIVED).equals(exist.getStatus())) {
            throw exception(HRM_PERFORMANCE_PLAN_ARCHIVED);
        }
        hrmPlanMapper.updateById(BeanUtils.toBean(reqVO, HrmPerformancePlanDO.class));
        recordProcess(BIZ_TYPE_PLAN, reqVO.getId(), "修改绩效计划", "修改计划：" + reqVO.getName(), "ACTION");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlan(Long id) {
        HrmPerformancePlanDO plan = validatePlanExists(id);
        if (Integer.valueOf(PLAN_STATUS_RUNNING).equals(plan.getStatus())) {
            throw exception(HRM_PERFORMANCE_PLAN_RUNNING);
        }
        // 连带删除该计划下的员工考核及明细
        List<HrmPerformanceAssessmentDO> assessments = hrmAssessmentMapper.selectListByPlanId(id);
        for (HrmPerformanceAssessmentDO assessment : assessments) {
            deleteAssessmentCascade(assessment.getId());
        }
        hrmRecordMapper.deleteByBiz(BIZ_TYPE_PLAN, id);
        hrmPlanMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startPlan(Long id) {
        HrmPerformancePlanDO plan = validatePlanExists(id);
        if (!Integer.valueOf(PLAN_STATUS_DRAFT).equals(plan.getStatus())
                && !Integer.valueOf(2).equals(plan.getStatus())) {
            throw exception(HRM_PERFORMANCE_PLAN_STATUS_INVALID);
        }
        // 按考评范围生成员工考核
        List<Long> employeeIds = resolveScopeEmployeeIds(plan);
        for (Long employeeId : employeeIds) {
            if (hrmAssessmentMapper.selectByPlanAndEmployee(id, employeeId) != null) {
                continue; // 幂等：已生成跳过
            }
            HrmPerformanceAssessmentDO assessment = HrmPerformanceAssessmentDO.builder()
                    .planId(id)
                    .employeeId(employeeId)
                    .status(ASSESSMENT_STATUS_DRAFT)
                    .stageType(1) // 员工填写
                    .stageSort(1)
                    .appealStatus(0)
                    .build();
            hrmAssessmentMapper.insert(assessment);
        }
        hrmPlanMapper.update(null, new LambdaUpdateWrapper<HrmPerformancePlanDO>()
                .eq(HrmPerformancePlanDO::getId, id)
                .set(HrmPerformancePlanDO::getStatus, PLAN_STATUS_RUNNING)
                .set(HrmPerformancePlanDO::getStageType, 1));
        recordProcess(BIZ_TYPE_PLAN, id, "开始绩效计划", "生成员工考核 " + employeeIds.size() + " 人", "ACTION");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void openScoring(Long id) {
        HrmPerformancePlanDO plan = validatePlanExists(id);
        if (!Integer.valueOf(PLAN_STATUS_RUNNING).equals(plan.getStatus())) {
            throw exception(HRM_PERFORMANCE_PLAN_STATUS_INVALID);
        }
        hrmPlanMapper.update(null, new LambdaUpdateWrapper<HrmPerformancePlanDO>()
                .eq(HrmPerformancePlanDO::getId, id)
                .set(HrmPerformancePlanDO::getStageType, 3)); // 自评
        hrmAssessmentMapper.update(null, new LambdaUpdateWrapper<HrmPerformanceAssessmentDO>()
                .eq(HrmPerformanceAssessmentDO::getPlanId, id)
                .set(HrmPerformanceAssessmentDO::getStageType, 3));
        recordProcess(BIZ_TYPE_PLAN, id, "开启评分", "计划进入自评阶段", "ACTION");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startInterview(Long id) {
        validatePlanExists(id);
        recordProcess(BIZ_TYPE_PLAN, id, "发起面谈", "计划进入面谈阶段", "ACTION");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archivePlan(Long id) {
        HrmPerformancePlanDO plan = validatePlanExists(id);
        if (!Integer.valueOf(PLAN_STATUS_RUNNING).equals(plan.getStatus())) {
            throw exception(HRM_PERFORMANCE_PLAN_STATUS_INVALID);
        }
        hrmPlanMapper.update(null, new LambdaUpdateWrapper<HrmPerformancePlanDO>()
                .eq(HrmPerformancePlanDO::getId, id)
                .set(HrmPerformancePlanDO::getStatus, PLAN_STATUS_ARCHIVED));
        // 员工考核同步归档
        hrmAssessmentMapper.update(null, new LambdaUpdateWrapper<HrmPerformanceAssessmentDO>()
                .eq(HrmPerformanceAssessmentDO::getPlanId, id)
                .set(HrmPerformanceAssessmentDO::getArchiveTime, LocalDateTime.now()));
        recordProcess(BIZ_TYPE_PLAN, id, "归档绩效计划", "计划归档", "ACTION");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void terminatePlan(Long id) {
        validatePlanExists(id);
        hrmPlanMapper.update(null, new LambdaUpdateWrapper<HrmPerformancePlanDO>()
                .eq(HrmPerformancePlanDO::getId, id)
                .set(HrmPerformancePlanDO::getStatus, PLAN_STATUS_TERMINATED)
                .set(HrmPerformancePlanDO::getTerminateTime, LocalDateTime.now()));
        recordProcess(BIZ_TYPE_PLAN, id, "终止绩效计划", "计划终止", "ACTION");
    }

    // ========== 员工考核 ==========

    @Override
    public PageResult<HrmPerformanceAssessmentVO> getAssessmentPage(HrmPerformanceAssessmentPageReqVO reqVO) {
        // 非归档分页：不含 archive_time 的考核
        Collection<Long> employeeIds = resolveEmployeeIds(reqVO.getDeptIds(), reqVO.getSearch());
        if (employeeIds != null && employeeIds.isEmpty()) {
            return PageResult.empty();
        }
        PageResult<HrmPerformanceAssessmentDO> pageResult = hrmAssessmentMapper.selectPage(reqVO, employeeIds);
        return new PageResult<>(enrichAssessments(pageResult.getList()), pageResult.getTotal());
    }

    @Override
    public HrmPerformanceAssessmentVO getAssessment(Long id) {
        HrmPerformanceAssessmentDO assessment = validateAssessmentExists(id);
        HrmPerformanceAssessmentVO vo = enrichAssessments(Collections.singletonList(assessment)).get(0);
        // 明细：维度（含指标）+ 阶段
        vo.setDimensionList(buildDimensionList(id));
        vo.setStageList(buildStageList(id));
        return vo;
    }

    @Override
    public List<Long> getUnassignedEmployeeIdList(Long planId) {
        validatePlanExists(planId);
        Set<Long> assigned = hrmAssessmentMapper.selectListByPlanId(planId).stream()
                .map(HrmPerformanceAssessmentDO::getEmployeeId).collect(Collectors.toSet());
        List<HrEmployeeDO> all = hrmEmployeeMapper.selectList(new LambdaQueryWrapper<HrEmployeeDO>()
                .in(HrEmployeeDO::getEntryStatus, Arrays.asList(1, 3)));
        return all.stream().map(HrEmployeeDO::getId).filter(id -> !assigned.contains(id)).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAssessmentList(HrmPerformanceAssessmentCreateListReqVO reqVO) {
        HrmPerformancePlanDO plan = validatePlanExists(reqVO.getPlanId());
        int created = 0;
        for (Long employeeId : reqVO.getEmployeeIds()) {
            if (hrmAssessmentMapper.selectByPlanAndEmployee(reqVO.getPlanId(), employeeId) != null) {
                continue;
            }
            HrmPerformanceAssessmentDO assessment = HrmPerformanceAssessmentDO.builder()
                    .planId(reqVO.getPlanId())
                    .employeeId(employeeId)
                    .status(ASSESSMENT_STATUS_DRAFT)
                    .stageType(plan.getStageType() != null ? plan.getStageType() : 1)
                    .stageSort(1)
                    .appealStatus(0)
                    .build();
            hrmAssessmentMapper.insert(assessment);
            created++;
        }
        return (long) created;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAssessmentList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        for (Long id : ids) {
            deleteAssessmentCascade(id);
        }
    }

    @Override
    public List<HrmPerformanceProcessRecordVO> getProcessRecordList(Long id) {
        return enrichRecords(hrmRecordMapper.selectListByBiz(BIZ_TYPE_ASSESSMENT, id));
    }

    // ========== 归档 ==========

    @Override
    public PageResult<HrmPerformanceAssessmentVO> getArchivePage(HrmPerformanceAssessmentPageReqVO reqVO) {
        Collection<Long> employeeIds = resolveEmployeeIds(reqVO.getDeptIds(), reqVO.getSearch());
        if (employeeIds != null && employeeIds.isEmpty()) {
            return PageResult.empty();
        }
        PageResult<HrmPerformanceAssessmentDO> pageResult = hrmAssessmentMapper.selectArchivePage(reqVO, employeeIds);
        return new PageResult<>(enrichAssessments(pageResult.getList()), pageResult.getTotal());
    }

    @Override
    public HrmPerformanceAssessmentVO getArchive(Long id) {
        HrmPerformanceAssessmentDO assessment = validateAssessmentExists(id);
        if (assessment.getArchiveTime() == null) {
            throw exception(HRM_PERFORMANCE_ASSESSMENT_NOT_ARCHIVED);
        }
        HrmPerformanceAssessmentVO vo = enrichAssessments(Collections.singletonList(assessment)).get(0);
        vo.setDimensionList(buildDimensionList(id));
        vo.setStageList(buildStageList(id));
        return vo;
    }

    @Override
    public List<HrmPerformancePlanVO> getArchivePlanSimpleList() {
        return enrichPlans(hrmPlanMapper.selectListByArchived());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArchive(Long id) {
        deleteAssessmentCascade(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArchiveEmployee(Long id) {
        deleteAssessmentCascade(id);
    }

    @Override
    public List<HrmPerformanceProcessRecordVO> getArchiveProcessRecordList(Long id) {
        return enrichRecords(hrmRecordMapper.selectListByBiz(BIZ_TYPE_ASSESSMENT, id));
    }

    // ========== 私有：级联删除 ==========

    private void deleteAssessmentCascade(Long assessmentId) {
        hrmDimensionMapper.delete(new LambdaQueryWrapper<HrmPerformanceAssessmentDimensionDO>()
                .eq(HrmPerformanceAssessmentDimensionDO::getAssessmentId, assessmentId));
        hrmQuotaMapper.delete(new LambdaQueryWrapper<HrmPerformanceAssessmentQuotaDO>()
                .eq(HrmPerformanceAssessmentQuotaDO::getAssessmentId, assessmentId));
        hrmStageMapper.delete(new LambdaQueryWrapper<HrmPerformanceAssessmentStageDO>()
                .eq(HrmPerformanceAssessmentStageDO::getAssessmentId, assessmentId));
        hrmRecordMapper.deleteByBiz(BIZ_TYPE_ASSESSMENT, assessmentId);
        hrmAssessmentMapper.deleteById(assessmentId);
    }

    // ========== 私有：考评范围解析 ==========

    /**
     * 按计划 scopes 解析员工编号（type=1 员工部门：deptIds 子树 + employeeIds；type=2 聘用形式：employeeType）
     */
    private List<Long> resolveScopeEmployeeIds(HrmPerformancePlanDO plan) {
        if (CollUtil.isEmpty(plan.getScopes())) {
            return Collections.emptyList();
        }
        Set<Long> result = new LinkedHashSet<>();
        for (Map<String, Object> scope : plan.getScopes()) {
            Integer type = toInt(scope.get("type"));
            // 员工部门范围
            List<Long> deptIds = toLongList(scope.get("deptIds"));
            if (CollUtil.isNotEmpty(deptIds)) {
                Set<Long> expanded = new HashSet<>();
                for (Long deptId : deptIds) {
                    expanded.add(deptId);
                    deptApi.getChildDeptList(deptId).forEach(d -> expanded.add(d.getId()));
                }
                hrmEmployeeMapper.selectList(new LambdaQueryWrapper<HrEmployeeDO>()
                                .in(HrEmployeeDO::getDeptId, expanded)
                                .in(HrEmployeeDO::getEntryStatus, Arrays.asList(1, 3)))
                        .forEach(e -> result.add(e.getId()));
            }
            // 指定员工
            List<Long> employeeIds = toLongList(scope.get("employeeIds"));
            if (CollUtil.isNotEmpty(employeeIds)) {
                result.addAll(employeeIds);
            }
            // 聘用形式
            Integer employeeType = toInt(scope.get("employeeType"));
            if (employeeType != null) {
                hrmEmployeeMapper.selectList(new LambdaQueryWrapper<HrEmployeeDO>()
                                .eq(HrEmployeeDO::getType, employeeType)
                                .in(HrEmployeeDO::getEntryStatus, Arrays.asList(1, 3)))
                        .forEach(e -> result.add(e.getId()));
            }
        }
        return new ArrayList<>(result);
    }

    private Collection<Long> resolveEmployeeIds(Long[] deptIds, String search) {
        if ((deptIds == null || deptIds.length == 0) && (search == null || search.isEmpty())) {
            return null;
        }
        Set<Long> expanded = new HashSet<>();
        if (deptIds != null) {
            for (Long deptId : deptIds) {
                if (deptId == null) {
                    continue;
                }
                expanded.add(deptId);
                deptApi.getChildDeptList(deptId).forEach(d -> expanded.add(d.getId()));
            }
        }
        List<HrEmployeeDO> employees = hrmEmployeeMapper.selectList(
                new cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX<HrEmployeeDO>()
                        .inIfPresent(HrEmployeeDO::getDeptId, expanded.isEmpty() ? null : expanded));
        if (search == null || search.isEmpty()) {
            return employees.stream().map(HrEmployeeDO::getId).collect(Collectors.toSet());
        }
        String kw = search.trim();
        return employees.stream()
                .filter(e -> (e.getName() != null && e.getName().contains(kw))
                        || (e.getEmpNo() != null && e.getEmpNo().contains(kw)))
                .map(HrEmployeeDO::getId).collect(Collectors.toSet());
    }

    // ========== 私有：enrich ==========

    private List<HrmPerformanceAssessmentTemplateVO> enrichTemplates(List<HrmPerformanceAssessmentTemplateDO> templates) {
        if (CollUtil.isEmpty(templates)) {
            return Collections.emptyList();
        }
        List<HrmPerformanceAssessmentTemplateVO> voList = BeanUtils.toBean(templates, HrmPerformanceAssessmentTemplateVO.class);
        for (int i = 0; i < templates.size(); i++) {
            HrmPerformanceAssessmentTemplateVO vo = voList.get(i);
            List<Map<String, Object>> dimensions = templates.get(i).getDimensions();
            if (CollUtil.isNotEmpty(dimensions)) {
                vo.setDimensionCount(dimensions.size());
                int quotaCount = 0;
                for (Map<String, Object> dimension : dimensions) {
                    Object quotas = dimension.get("quotas");
                    if (quotas instanceof List) {
                        quotaCount += ((List<?>) quotas).size();
                    }
                }
                vo.setQuotaCount(quotaCount);
            } else {
                vo.setDimensionCount(0);
                vo.setQuotaCount(0);
            }
        }
        return voList;
    }

    private List<HrmPerformancePlanVO> enrichPlans(List<HrmPerformancePlanDO> plans) {
        if (CollUtil.isEmpty(plans)) {
            return Collections.emptyList();
        }
        List<HrmPerformancePlanVO> voList = BeanUtils.toBean(plans, HrmPerformancePlanVO.class);
        // 模板名
        Set<Long> assessTemplateIds = plans.stream().map(HrmPerformancePlanDO::getAssessmentTemplateId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> assessNameMap = assessTemplateIds.isEmpty() ? Collections.emptyMap()
                : hrmAssessmentTemplateMapper.selectByIds(assessTemplateIds).stream()
                        .collect(Collectors.toMap(HrmPerformanceAssessmentTemplateDO::getId,
                                HrmPerformanceAssessmentTemplateDO::getName, (a, b) -> a));
        Set<Long> resultTemplateIds = plans.stream().map(HrmPerformancePlanDO::getResultTemplateId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> resultNameMap = resultTemplateIds.isEmpty() ? Collections.emptyMap()
                : hrmResultTemplateMapper.selectByIds(resultTemplateIds).stream()
                        .collect(Collectors.toMap(HrmPerformanceResultTemplateDO::getId,
                                HrmPerformanceResultTemplateDO::getName, (a, b) -> a));
        for (int i = 0; i < plans.size(); i++) {
            HrmPerformancePlanDO plan = plans.get(i);
            HrmPerformancePlanVO vo = voList.get(i);
            vo.setAssessmentTemplateName(assessNameMap.get(plan.getAssessmentTemplateId()));
            vo.setResultTemplateName(resultNameMap.get(plan.getResultTemplateId()));
            // 员工数 / 已完成数
            List<HrmPerformanceAssessmentDO> assessments = hrmAssessmentMapper.selectListByPlanId(plan.getId());
            vo.setEmployeeCount((long) assessments.size());
            long finished = assessments.stream()
                    .filter(a -> a.getScore() != null || Integer.valueOf(8).equals(a.getStageType())).count();
            vo.setFinishedCount(finished);
            // 可操作判断
            vo.setScoringReady(Integer.valueOf(PLAN_STATUS_RUNNING).equals(plan.getStatus()));
            vo.setInterviewReady(Integer.valueOf(PLAN_STATUS_RUNNING).equals(plan.getStatus()));
            vo.setArchiveReady(Integer.valueOf(PLAN_STATUS_RUNNING).equals(plan.getStatus()) && !assessments.isEmpty());
            vo.setOperationType(plan.getStageType());
        }
        return voList;
    }

    private List<HrmPerformanceAssessmentVO> enrichAssessments(List<HrmPerformanceAssessmentDO> assessments) {
        if (CollUtil.isEmpty(assessments)) {
            return Collections.emptyList();
        }
        List<HrmPerformanceAssessmentVO> voList = BeanUtils.toBean(assessments, HrmPerformanceAssessmentVO.class);
        // 计划信息
        Set<Long> planIds = assessments.stream().map(HrmPerformanceAssessmentDO::getPlanId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, HrmPerformancePlanDO> planMap = planIds.isEmpty() ? Collections.emptyMap()
                : hrmPlanMapper.selectListByIds(planIds).stream()
                        .collect(Collectors.toMap(HrmPerformancePlanDO::getId, p -> p, (a, b) -> a));
        // 员工信息
        Set<Long> employeeIds = assessments.stream().map(HrmPerformanceAssessmentDO::getEmployeeId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, HrEmployeeDO> employeeMap = employeeIds.isEmpty() ? Collections.emptyMap()
                : hrmEmployeeMapper.selectByIds(employeeIds).stream()
                        .collect(Collectors.toMap(HrEmployeeDO::getId, e -> e, (a, b) -> a));
        Set<Long> deptIds = employeeMap.values().stream().map(HrEmployeeDO::getDeptId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, DeptRespDTO> deptMap = deptIds.isEmpty() ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        // 处理人名
        Set<Long> handlerIds = new HashSet<>();
        assessments.forEach(a -> {
            if (a.getTargetConfirmationEmployeeId() != null) {
                handlerIds.add(a.getTargetConfirmationEmployeeId());
            }
        });
        Map<Long, String> handlerNameMap = handlerIds.isEmpty() ? Collections.emptyMap()
                : hrmEmployeeMapper.selectByIds(handlerIds).stream()
                        .collect(Collectors.toMap(HrEmployeeDO::getId, HrEmployeeDO::getName, (a, b) -> a));
        for (int i = 0; i < assessments.size(); i++) {
            HrmPerformanceAssessmentDO assessment = assessments.get(i);
            HrmPerformanceAssessmentVO vo = voList.get(i);
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
            HrEmployeeDO employee = employeeMap.get(assessment.getEmployeeId());
            if (employee != null) {
                vo.setEmployeeName(employee.getName());
                vo.setJobNumber(employee.getEmpNo());
                vo.setMobile(employee.getPhone());
                vo.setDeptId(employee.getDeptId());
                vo.setPostName(employee.getPostName());
                vo.setEmployeeType(employee.getType());
                vo.setEmployeeStatus(employee.getEmployeeStatus());
                DeptRespDTO dept = deptMap.get(employee.getDeptId());
                if (dept != null) {
                    vo.setDeptName(dept.getName());
                }
            }
            vo.setTargetConfirmationEmployeeName(handlerNameMap.get(assessment.getTargetConfirmationEmployeeId()));
            vo.setCurrentHandlerName(handlerNameMap.get(assessment.getTargetConfirmationEmployeeId()));
            // 可确认目标：待目标确认阶段（stageType=2）且有目标确认人
            vo.setCanConfirmTarget(Integer.valueOf(2).equals(assessment.getStageType()));
        }
        return voList;
    }

    private List<HrmPerformanceAssessmentDimensionVO> buildDimensionList(Long assessmentId) {
        List<HrmPerformanceAssessmentDimensionDO> dimensions = hrmDimensionMapper.selectListByAssessmentId(assessmentId);
        List<HrmPerformanceAssessmentDimensionVO> voList = BeanUtils.toBean(dimensions, HrmPerformanceAssessmentDimensionVO.class);
        List<HrmPerformanceAssessmentQuotaDO> quotas = hrmQuotaMapper.selectListByAssessmentId(assessmentId);
        Map<Long, List<HrmPerformanceAssessmentQuotaVO>> quotaMap = quotas.stream()
                .collect(Collectors.groupingBy(HrmPerformanceAssessmentQuotaDO::getDimensionId,
                        Collectors.mapping(q -> BeanUtils.toBean(q, HrmPerformanceAssessmentQuotaVO.class),
                                Collectors.toList())));
        for (HrmPerformanceAssessmentDimensionVO vo : voList) {
            vo.setQuotaList(quotaMap.getOrDefault(vo.getId(), Collections.emptyList()));
        }
        return voList;
    }

    private List<HrmPerformanceAssessmentStageVO> buildStageList(Long assessmentId) {
        List<HrmPerformanceAssessmentStageDO> stages = hrmStageMapper.selectListByAssessmentId(assessmentId);
        List<HrmPerformanceAssessmentStageVO> voList = BeanUtils.toBean(stages, HrmPerformanceAssessmentStageVO.class);
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

    private List<HrmPerformanceProcessRecordVO> enrichRecords(List<HrmPerformanceProcessRecordDO> records) {
        if (CollUtil.isEmpty(records)) {
            return Collections.emptyList();
        }
        List<HrmPerformanceProcessRecordVO> voList = BeanUtils.toBean(records, HrmPerformanceProcessRecordVO.class);
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

    private void recordProcess(Integer bizType, Long bizId, String title, String content, String source) {
        HrmPerformanceProcessRecordDO record = HrmPerformanceProcessRecordDO.builder()
                .bizType(bizType)
                .bizId(bizId)
                .title(title)
                .content(content)
                .source(source)
                .operateTime(LocalDateTime.now())
                .build();
        hrmRecordMapper.insert(record);
    }

    // ========== 私有：校验 ==========

    private HrmPerformanceAssessmentTemplateDO validateAssessmentTemplateExists(Long id) {
        HrmPerformanceAssessmentTemplateDO template = hrmAssessmentTemplateMapper.selectById(id);
        if (template == null) {
            throw exception(HRM_PERFORMANCE_TEMPLATE_NOT_EXISTS);
        }
        return template;
    }

    private HrmPerformanceResultTemplateDO validateResultTemplateExists(Long id) {
        HrmPerformanceResultTemplateDO template = hrmResultTemplateMapper.selectById(id);
        if (template == null) {
            throw exception(HRM_PERFORMANCE_RESULT_TEMPLATE_NOT_EXISTS);
        }
        return template;
    }

    private HrmPerformancePlanDO validatePlanExists(Long id) {
        HrmPerformancePlanDO plan = hrmPlanMapper.selectById(id);
        if (plan == null) {
            throw exception(HRM_PERFORMANCE_PLAN_NOT_EXISTS);
        }
        return plan;
    }

    private HrmPerformanceAssessmentDO validateAssessmentExists(Long id) {
        HrmPerformanceAssessmentDO assessment = hrmAssessmentMapper.selectById(id);
        if (assessment == null) {
            throw exception(HRM_PERFORMANCE_ASSESSMENT_NOT_EXISTS);
        }
        return assessment;
    }

    private void validateTemplateNameUnique(Object exist, Long id, String name, boolean assessment) {
        if (exist == null) {
            return;
        }
        Long existId;
        if (exist instanceof HrmPerformanceAssessmentTemplateDO) {
            existId = ((HrmPerformanceAssessmentTemplateDO) exist).getId();
        } else {
            existId = ((HrmPerformanceResultTemplateDO) exist).getId();
        }
        if (id == null || !existId.equals(id)) {
            throw exception(assessment ? HRM_PERFORMANCE_TEMPLATE_NAME_DUPLICATE
                    : HRM_PERFORMANCE_RESULT_TEMPLATE_NAME_DUPLICATE);
        }
    }

    // ========== 私有：小工具 ==========

    private static Integer toInt(Object value) {
        if (value == null) {
            return null;
        }
        return (int) Double.parseDouble(value.toString());
    }

    private static List<Long> toLongList(Object value) {
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            if (item != null) {
                result.add((long) Double.parseDouble(item.toString()));
            }
        }
        return result;
    }

}
