package cn.zhicloud.module.hr.service.hrm.salary;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.hr.controller.admin.hrm.salary.vo.*;
import cn.zhicloud.module.hr.dal.dataobject.salary2.*;
import cn.zhicloud.module.hr.dal.mysql.salary2.*;
import cn.zhicloud.module.system.api.dept.DeptApi;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link HrmSalaryConfigServiceImpl} 单元测试
 *
 * @author zhicloud
 */
@Import(HrmSalaryConfigServiceImpl.class)
public class HrmSalaryConfigServiceImplTest extends BaseDbUnitTest {

    @Resource
    private HrmSalaryConfigServiceImpl salaryConfigService;

    @Resource
    private HrmSalaryGroupMapper groupMapper;
    @Resource
    private HrmSalaryOptionMapper optionMapper;
    @Resource
    private HrmSalaryTaxRuleMapper taxRuleMapper;
    @Resource
    private HrmSalaryChangeTemplateMapper changeTemplateMapper;
    @Resource
    private HrmSalaryConfigMapper hrmSalaryConfigMapper;

    @MockitoBean
    private DeptApi deptApi;

    @Test
    public void test_createGroup_success() {
        HrmSalaryGroupVO reqVO = new HrmSalaryGroupVO();
        reqVO.setName("默认薪资组");
        reqVO.setSalaryStandard(1);
        reqVO.setDeptIds(Arrays.asList(100L, 101L));
        reqVO.setEmployeeIds(Arrays.asList(1L));
        Long id = salaryConfigService.createGroup(reqVO);

        HrmSalaryGroupDO group = groupMapper.selectById(id);
        assertNotNull(group);
        assertEquals("默认薪资组", group.getName());
        assertEquals(2, group.getDeptIds().size());
    }

    @Test
    public void test_createGroup_duplicateName() {
        HrmSalaryGroupVO first = new HrmSalaryGroupVO();
        first.setName("重名薪资组");
        salaryConfigService.createGroup(first);
        HrmSalaryGroupVO dup = new HrmSalaryGroupVO();
        dup.setName("重名薪资组");
        cn.zhicloud.framework.common.exception.ServiceException ex =
                assertThrows(cn.zhicloud.framework.common.exception.ServiceException.class,
                        () -> salaryConfigService.createGroup(dup));
        assertEquals(1_050_020_001, ex.getCode());
    }

    @Test
    public void test_syncOption_buildsTreeAndIdempotent() {
        salaryConfigService.syncOption();
        List<HrmSalaryOptionVO> tree = salaryConfigService.getOptionList();
        // 分类根（15 个）
        assertEquals(15, tree.size());
        // 每个分类根都有子项
        assertTrue(tree.stream().anyMatch(r -> r.getChildren() != null && !r.getChildren().isEmpty()));
        // 基本工资分类下有 2 个子项
        HrmSalaryOptionVO basicCategory = tree.stream()
                .filter(r -> r.getCode() == 10).findFirst().orElse(null);
        assertNotNull(basicCategory);
        assertEquals(2, basicCategory.getChildren().size());

        int before = optionMapper.selectListAll().size();
        salaryConfigService.syncOption(); // 幂等
        assertEquals(before, optionMapper.selectListAll().size());
    }

    @Test
    public void test_deleteOption_systemRejected() {
        salaryConfigService.syncOption();
        HrmSalaryOptionDO systemOption = optionMapper.selectByCode(10101);
        assertNotNull(systemOption);
        cn.zhicloud.framework.common.exception.ServiceException ex =
                assertThrows(cn.zhicloud.framework.common.exception.ServiceException.class,
                        () -> salaryConfigService.deleteOption(systemOption.getId()));
        assertEquals(1_050_020_004, ex.getCode());
    }

    @Test
    public void test_createOption_customThenToggle() {
        HrmSalaryOptionVO reqVO = new HrmSalaryOptionVO();
        reqVO.setCode(999999);
        reqVO.setParentCode(10);
        reqVO.setName("企业自定义补贴");
        Long id = salaryConfigService.createOption(reqVO);

        HrmSalaryOptionEnabledReqVO enabledReq = new HrmSalaryOptionEnabledReqVO();
        enabledReq.setId(id);
        enabledReq.setEnabled(false);
        salaryConfigService.updateOptionEnabled(enabledReq);
        assertEquals(false, optionMapper.selectById(id).getEnabled());

        HrmSalaryOptionVisibleReqVO visibleReq = new HrmSalaryOptionVisibleReqVO();
        visibleReq.setId(id);
        visibleReq.setVisible(false);
        salaryConfigService.updateOptionVisible(visibleReq);
        assertEquals(false, optionMapper.selectById(id).getVisible());
    }

    @Test
    public void test_deleteTaxRule_usedByGroupRejected() {
        HrmSalaryTaxRuleVO ruleVO = new HrmSalaryTaxRuleVO();
        ruleVO.setName("标准计税规则");
        ruleVO.setType(1);
        ruleVO.setThreshold(new BigDecimal("5000"));
        Long ruleId = salaryConfigService.createTaxRule(ruleVO);

        HrmSalaryGroupVO groupVO = new HrmSalaryGroupVO();
        groupVO.setName("引用计税组的薪资组");
        groupVO.setTaxRuleId(ruleId);
        salaryConfigService.createGroup(groupVO);

        cn.zhicloud.framework.common.exception.ServiceException ex =
                assertThrows(cn.zhicloud.framework.common.exception.ServiceException.class,
                        () -> salaryConfigService.deleteTaxRule(ruleId));
        assertEquals(1_050_020_008, ex.getCode());
    }

    @Test
    public void test_getTaxRuleList_enrichesUsedGroupCount() {
        HrmSalaryTaxRuleVO ruleVO = new HrmSalaryTaxRuleVO();
        ruleVO.setName("计数计税规则");
        ruleVO.setType(1);
        Long ruleId = salaryConfigService.createTaxRule(ruleVO);
        HrmSalaryGroupVO groupVO = new HrmSalaryGroupVO();
        groupVO.setName("计数组");
        groupVO.setTaxRuleId(ruleId);
        salaryConfigService.createGroup(groupVO);

        List<HrmSalaryTaxRuleVO> rules = salaryConfigService.getTaxRuleList();
        HrmSalaryTaxRuleVO target = rules.stream().filter(r -> r.getId().equals(ruleId)).findFirst().orElse(null);
        assertNotNull(target);
        assertEquals(1L, target.getUsedGroupCount());
    }

    @Test
    public void test_changeTemplate_crud() {
        HrmSalaryChangeTemplateVO reqVO = new HrmSalaryChangeTemplateVO();
        reqVO.setName("默认调薪模板");
        reqVO.setDefaultStatus(true);
        HrmSalaryChangeOptionVO option = new HrmSalaryChangeOptionVO();
        option.setName("基本工资");
        option.setCode(10101);
        reqVO.setOptions(Arrays.asList(option));
        Long id = salaryConfigService.createChangeTemplate(reqVO);

        HrmSalaryChangeTemplateVO read = salaryConfigService.getChangeTemplate(id);
        assertEquals("默认调薪模板", read.getName());
        assertEquals(1, read.getOptions().size());
    }

    @Test
    public void test_config_singleRowUpsert() {
        // 首次 get 为 null
        assertNull(salaryConfigService.getConfig());

        HrmSalaryConfigVO configVO = new HrmSalaryConfigVO();
        configVO.setCycleStartDay(1);
        configVO.setCycleEndDay(31);
        configVO.setSocialSecurityMonthType(1);
        configVO.setStartYear(2026);
        configVO.setStartMonth(1);
        Long id = salaryConfigService.createConfig(configVO);
        assertNotNull(id);

        // 二次 create 返回既有
        Long again = salaryConfigService.createConfig(configVO);
        assertEquals(id, again);

        // update 不改 id
        HrmSalaryConfigVO updateVO = new HrmSalaryConfigVO();
        updateVO.setCycleStartDay(5);
        salaryConfigService.updateConfig(updateVO);
        assertEquals(1, hrmSalaryConfigMapper.selectList(null).size());
    }

}
