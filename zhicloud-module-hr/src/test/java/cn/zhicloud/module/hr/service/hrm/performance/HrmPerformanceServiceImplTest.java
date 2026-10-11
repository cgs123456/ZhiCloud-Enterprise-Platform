package cn.zhicloud.module.hr.service.hrm.performance;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.hr.controller.admin.hrm.performance.vo.*;
import cn.zhicloud.module.hr.dal.dataobject.employee.HrEmployeeDO;
import cn.zhicloud.module.hr.dal.dataobject.performance.*;
import cn.zhicloud.module.hr.dal.mysql.employee.HrEmployeeMapper;
import cn.zhicloud.module.hr.dal.mysql.performance.*;
import cn.zhicloud.module.system.api.dept.DeptApi;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link HrmPerformanceServiceImpl} 单元测试
 *
 * @author zhicloud
 */
@Import(HrmPerformanceServiceImpl.class)
public class HrmPerformanceServiceImplTest extends BaseDbUnitTest {

    @Resource
    private HrmPerformanceServiceImpl performanceService;

    @Resource
    private HrmPerformanceAssessmentTemplateMapper templateMapper;
    @Resource
    private HrmPerformanceResultTemplateMapper resultTemplateMapper;
    @Resource
    private HrmPerformancePlanMapper planMapper;
    @Resource
    private HrmPerformanceAssessmentMapper assessmentMapper;
    @Resource
    private HrmPerformanceProcessRecordMapper recordMapper;
    @Resource
    private HrEmployeeMapper employeeMapper;

    @MockitoBean
    private DeptApi deptApi;

    private HrEmployeeDO insertEmployee(String name, Long deptId) {
        HrEmployeeDO employee = randomPojo(HrEmployeeDO.class, o -> {
            o.setName(name);
            o.setDeptId(deptId);
            o.setEntryStatus(1);
            o.setType(1);
        });
        employeeMapper.insert(employee);
        return employee;
    }

    @Test
    public void test_createAssessmentTemplate_countsDimensionsAndQuotas() {
        HrmPerformanceAssessmentTemplateVO reqVO = new HrmPerformanceAssessmentTemplateVO();
        reqVO.setName("技术岗考核模板");
        reqVO.setScoreCalculation(1);
        reqVO.setUpperLimitType(1);
        Map<String, Object> dim1 = new HashMap<>();
        dim1.put("name", "业绩");
        dim1.put("weight", 60);
        dim1.put("quotas", Arrays.asList(Map.of("name", "指标1"), Map.of("name", "指标2")));
        Map<String, Object> dim2 = new HashMap<>();
        dim2.put("name", "能力");
        dim2.put("weight", 40);
        dim2.put("quotas", Arrays.asList(Map.of("name", "指标3")));
        reqVO.setDimensions(Arrays.asList(dim1, dim2));

        Long id = performanceService.createAssessmentTemplate(reqVO);

        HrmPerformanceAssessmentTemplateVO read = performanceService.getAssessmentTemplate(id);
        assertEquals(2, read.getDimensionCount());
        assertEquals(3, read.getQuotaCount());
    }

    @Test
    public void test_createAssessmentTemplate_duplicateName() {
        HrmPerformanceAssessmentTemplateVO first = new HrmPerformanceAssessmentTemplateVO();
        first.setName("重名模板");
        first.setScoreCalculation(1);
        first.setUpperLimitType(1);
        first.setDimensions(Collections.emptyList());
        performanceService.createAssessmentTemplate(first);

        HrmPerformanceAssessmentTemplateVO dup = new HrmPerformanceAssessmentTemplateVO();
        dup.setName("重名模板");
        dup.setScoreCalculation(1);
        dup.setUpperLimitType(1);
        dup.setDimensions(Collections.emptyList());
        cn.zhicloud.framework.common.exception.ServiceException ex =
                assertThrows(cn.zhicloud.framework.common.exception.ServiceException.class,
                        () -> performanceService.createAssessmentTemplate(dup));
        assertEquals(1_050_019_001, ex.getCode());
    }

    @Test
    public void test_createResultTemplate_withLevels() {
        HrmPerformanceResultTemplateVO reqVO = new HrmPerformanceResultTemplateVO();
        reqVO.setName("五级结果模板");
        reqVO.setLevels(Arrays.asList(
                Map.of("name", "S", "minScore", 90, "maxScore", 100, "coefficient", 1.2),
                Map.of("name", "A", "minScore", 80, "maxScore", 89, "coefficient", 1.0)));
        Long id = performanceService.createResultTemplate(reqVO);

        HrmPerformanceResultTemplateVO read = performanceService.getResultTemplate(id);
        assertEquals(2, read.getLevels().size());
    }

    @Test
    public void test_startPlan_generatesAssessmentsByIdEmployeeScope() {
        HrEmployeeDO e1 = insertEmployee("绩效员工A", 100L);
        HrEmployeeDO e2 = insertEmployee("绩效员工B", 100L);
        HrmPerformancePlanVO planVO = new HrmPerformancePlanVO();
        planVO.setName("Q4 绩效计划");
        planVO.setCycleType(2);
        Map<String, Object> scope = new HashMap<>();
        scope.put("type", 1);
        scope.put("employeeIds", Arrays.asList(e1.getId(), e2.getId()));
        planVO.setScopes(Arrays.asList(scope));
        Long planId = performanceService.createPlan(planVO);

        performanceService.startPlan(planId);

        List<HrmPerformanceAssessmentDO> assessments = assessmentMapper.selectListByPlanId(planId);
        assertEquals(2, assessments.size());
        assertEquals(Integer.valueOf(3), planMapper.selectById(planId).getStatus()); // 进行中

        // 生命周期守卫：已进行中不可重复 start（一次性状态迁移）
        cn.zhicloud.framework.common.exception.ServiceException ex =
                assertThrows(cn.zhicloud.framework.common.exception.ServiceException.class,
                        () -> performanceService.startPlan(planId));
        assertEquals(1_050_019_005, ex.getCode());
    }

    @Test
    public void test_planLifecycle_archiveAndTerminate() {
        HrmPerformancePlanVO planVO = new HrmPerformancePlanVO();
        planVO.setName("生命周期计划");
        planVO.setScopes(Collections.emptyList());
        Long planId = performanceService.createPlan(planVO);

        // 草稿 → 开始（无范围也允许，生成 0 员工）→ 归档
        performanceService.startPlan(planId);
        performanceService.archivePlan(planId);
        assertEquals(Integer.valueOf(4), planMapper.selectById(planId).getStatus());

        // 归档后修改被拒
        HrmPerformancePlanVO updateVO = new HrmPerformancePlanVO();
        updateVO.setId(planId);
        updateVO.setName("改名");
        cn.zhicloud.framework.common.exception.ServiceException ex =
                assertThrows(cn.zhicloud.framework.common.exception.ServiceException.class,
                        () -> performanceService.updatePlan(updateVO));
        assertEquals(1_050_019_006, ex.getCode());
    }

    @Test
    public void test_deletePlan_runningRejected() {
        HrmPerformancePlanVO planVO = new HrmPerformancePlanVO();
        planVO.setName("进行中计划");
        planVO.setScopes(Collections.emptyList());
        Long planId = performanceService.createPlan(planVO);
        performanceService.startPlan(planId);

        cn.zhicloud.framework.common.exception.ServiceException ex =
                assertThrows(cn.zhicloud.framework.common.exception.ServiceException.class,
                        () -> performanceService.deletePlan(planId));
        assertEquals(1_050_019_007, ex.getCode());
    }

    @Test
    public void test_planStatusCount_fiveTabs() {
        HrmPerformancePlanVO planVO = new HrmPerformancePlanVO();
        planVO.setName("计数计划");
        planVO.setScopes(Collections.emptyList());
        performanceService.createPlan(planVO);

        List<HrmPerformancePlanStatusCountRespVO> counts = performanceService.getPlanStatusCount();
        assertEquals(5, counts.size());
        Map<Integer, Long> map = counts.stream()
                .collect(java.util.stream.Collectors.toMap(HrmPerformancePlanStatusCountRespVO::getStatus,
                        HrmPerformancePlanStatusCountRespVO::getCount));
        assertEquals(1L, map.get(1)); // 草稿 1 条
    }

    @Test
    public void test_createPlan_recordsProcess() {
        HrmPerformancePlanVO planVO = new HrmPerformancePlanVO();
        planVO.setName("记录计划");
        planVO.setScopes(Collections.emptyList());
        Long planId = performanceService.createPlan(planVO);

        List<HrmPerformanceProcessRecordDO> records = recordMapper.selectListByBiz(1, planId);
        assertEquals(1, records.size());
        assertEquals("创建绩效计划", records.get(0).getTitle());
    }

    @Test
    public void test_createAssessmentList_idempotent() {
        HrEmployeeDO e1 = insertEmployee("补录员工", 100L);
        HrmPerformancePlanVO planVO = new HrmPerformancePlanVO();
        planVO.setName("补录计划");
        planVO.setScopes(Collections.emptyList());
        Long planId = performanceService.createPlan(planVO);

        HrmPerformanceAssessmentCreateListReqVO reqVO = new HrmPerformanceAssessmentCreateListReqVO();
        reqVO.setPlanId(planId);
        reqVO.setEmployeeIds(Arrays.asList(e1.getId()));
        Long created = performanceService.createAssessmentList(reqVO);
        assertEquals(1L, created);

        // 再补录同员工 → 0 新增
        Long again = performanceService.createAssessmentList(reqVO);
        assertEquals(0L, again);
    }

}
