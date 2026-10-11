package cn.zhicloud.module.hr.service.hrm.salary;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.hr.controller.admin.hrm.salary.vo.*;
import cn.zhicloud.module.hr.dal.dataobject.employee.HrEmployeeDO;
import cn.zhicloud.module.hr.dal.dataobject.salary2.*;
import cn.zhicloud.module.hr.dal.mysql.employee.HrEmployeeMapper;
import cn.zhicloud.module.hr.dal.mysql.salary2.*;
import cn.zhicloud.module.system.api.dept.DeptApi;
import cn.zhicloud.module.system.api.dept.dto.DeptRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.stream.Collectors;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.hr.enums.ErrorCodeConstants.*;

/**
 * HRM 薪资配置域 Service 实现类
 *
 * 口径（对齐前端 views/hrm/salary/config）：
 * * 薪资项树：parent_code=0 为分类根，其余为子项；system_flag=true 为系统默认项（不可删、不可改启用开关）。
 * * syncOption：首次初始化标准薪资项树（按前端 HrmSalaryOptionCode 枚举的预置项，幂等——已存在 code 跳过）。
 * * 计税规则被薪资组引用（tax_rule_id）时删除拒绝；薪资组 name 唯一。
 * * 薪资配置为单条（selectSingle）；create 已存在时返回已有 id。
 *
 * @author zhicloud
 */
@Service
@Validated
public class HrmSalaryConfigServiceImpl implements HrmSalaryConfigService {

    /** 标准薪资项分类（code → 名称），parentCode=0 */
    private static final Map<Integer, String> CATEGORIES = new LinkedHashMap<>() {{
        put(10, "基本工资");
        put(20, "津贴补贴");
        put(100, "社保公积金");
        put(110, "公司社保");
        put(120, "公司公积金");
        put(180, "加班工资");
        put(190, "考勤扣款");
        put(200, "考勤扣款合计");
        put(210, "应发工资");
        put(220, "应税工资");
        put(230, "个人所得税");
        put(240, "实发工资");
        put(250, "累计数据");
        put(260, "专项附加扣除");
        put(270, "累计计税");
    }};

    /** 标准薪资项（code → [名称, 父分类 code, 是否计税, 类型]），类型 0减项 1加项 2计算项 */
    private static final List<Object[]> STANDARD_OPTIONS = Arrays.asList(
            new Object[]{10101, "基本工资", 10, true, 1},
            new Object[]{10102, "岗位工资", 10, true, 1},
            new Object[]{20104, "餐补", 20, false, 1},
            new Object[]{180101, "加班工资", 180, true, 1},
            new Object[]{190101, "迟到扣款", 190, false, 0},
            new Object[]{190102, "早退扣款", 190, false, 0},
            new Object[]{190103, "旷工扣款", 190, false, 0},
            new Object[]{190104, "假期扣款", 190, false, 0},
            new Object[]{190105, "缺卡扣款", 190, false, 0},
            new Object[]{190106, "综合扣款", 190, false, 0},
            new Object[]{200101, "考勤扣款合计", 200, false, 2},
            new Object[]{100101, "个人社保", 100, false, 0},
            new Object[]{100102, "个人公积金", 100, false, 0},
            new Object[]{110101, "公司社保", 110, false, 1},
            new Object[]{120101, "公司公积金", 120, false, 1},
            new Object[]{210101, "应发工资", 210, false, 2},
            new Object[]{220101, "应税工资", 220, true, 2},
            new Object[]{230101, "个人所得税", 230, true, 0},
            new Object[]{240101, "实发工资", 240, false, 2},
            new Object[]{250101, "累计收入额（截至上月）", 250, false, 2},
            new Object[]{250102, "累计减除费用（截至上月）", 250, false, 2},
            new Object[]{250103, "累计专项扣除（截至上月）", 250, false, 2},
            new Object[]{250105, "累计已预缴税额", 250, false, 2},
            new Object[]{260101, "累计子女教育", 260, false, 2},
            new Object[]{260102, "累计住房租金", 260, false, 2},
            new Object[]{260103, "累计住房贷款利息", 260, false, 2},
            new Object[]{260104, "累计赡养老人", 260, false, 2},
            new Object[]{260105, "累计继续教育", 260, false, 2},
            new Object[]{270101, "累计收入额", 270, false, 2},
            new Object[]{270102, "累计减除费用", 270, false, 2},
            new Object[]{270103, "累计专项扣除", 270, false, 2},
            new Object[]{270104, "累计专项附加扣除", 270, false, 2},
            new Object[]{270105, "累计应纳税所得额", 270, false, 2},
            new Object[]{270106, "累计应纳税额", 270, false, 2});

    @Resource
    private HrmSalaryGroupMapper hrmSalaryGroupMapper;
    @Resource
    private HrmSalaryOptionMapper hrmSalaryOptionMapper;
    @Resource
    private HrmSalaryTaxRuleMapper hrmSalaryTaxRuleMapper;
    @Resource
    private HrmSalaryChangeTemplateMapper hrmSalaryChangeTemplateMapper;
    @Resource
    private HrmSalaryConfigMapper hrmSalaryConfigMapper;
    @Resource
    private HrEmployeeMapper hrmEmployeeMapper;
    @Resource
    private DeptApi deptApi;

    // ========== 薪资组 ==========

    @Override
    public PageResult<HrmSalaryGroupVO> getGroupPage(HrmSalaryGroupPageReqVO reqVO) {
        PageResult<HrmSalaryGroupDO> pageResult = hrmSalaryGroupMapper.selectPage(reqVO);
        return new PageResult<>(enrichGroups(pageResult.getList()), pageResult.getTotal());
    }

    @Override
    public HrmSalaryGroupVO getGroup(Long id) {
        return enrichGroups(Collections.singletonList(validateGroupExists(id))).get(0);
    }

    @Override
    public List<HrmSalaryGroupVO> getGroupSimpleList() {
        return enrichGroups(hrmSalaryGroupMapper.selectListAll());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGroup(HrmSalaryGroupVO reqVO) {
        validateGroupNameUnique(null, reqVO.getName());
        if (reqVO.getTaxRuleId() != null) {
            validateTaxRuleExists(reqVO.getTaxRuleId());
        }
        HrmSalaryGroupDO group = BeanUtils.toBean(reqVO, HrmSalaryGroupDO.class);
        hrmSalaryGroupMapper.insert(group);
        return group.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGroup(HrmSalaryGroupVO reqVO) {
        validateGroupExists(reqVO.getId());
        validateGroupNameUnique(reqVO.getId(), reqVO.getName());
        if (reqVO.getTaxRuleId() != null) {
            validateTaxRuleExists(reqVO.getTaxRuleId());
        }
        hrmSalaryGroupMapper.updateById(BeanUtils.toBean(reqVO, HrmSalaryGroupDO.class));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Long id) {
        validateGroupExists(id);
        hrmSalaryGroupMapper.deleteById(id);
    }

    // ========== 薪资项 ==========

    @Override
    public List<HrmSalaryOptionVO> getOptionList() {
        return buildOptionTree(hrmSalaryOptionMapper.selectListAll());
    }

    @Override
    public List<HrmSalaryOptionVO> getOptionSimpleList() {
        // 精简列表：仅启用且非分类的叶子项（下拉消费）
        return hrmSalaryOptionMapper.selectListAll().stream()
                .filter(o -> o.getParentCode() != null && o.getParentCode() != 0)
                .filter(o -> Boolean.TRUE.equals(o.getEnabled()))
                .map(o -> BeanUtils.toBean(o, HrmSalaryOptionVO.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOption(HrmSalaryOptionVO reqVO) {
        if (reqVO.getCode() != null && hrmSalaryOptionMapper.selectByCode(reqVO.getCode()) != null) {
            throw exception(HRM_SALARY_OPTION_CODE_DUPLICATE);
        }
        HrmSalaryOptionDO option = BeanUtils.toBean(reqVO, HrmSalaryOptionDO.class);
        option.setSystemFlag(false); // 新建非系统项
        applyOptionDefaults(option);
        hrmSalaryOptionMapper.insert(option);
        return option.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOption(HrmSalaryOptionVO reqVO) {
        validateOptionExists(reqVO.getId());
        HrmSalaryOptionDO updateObj = BeanUtils.toBean(reqVO, HrmSalaryOptionDO.class);
        updateObj.setSystemFlag(null); // 系统标志不可改
        hrmSalaryOptionMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOption(Long id) {
        HrmSalaryOptionDO option = validateOptionExists(id);
        if (Boolean.TRUE.equals(option.getSystemFlag())) {
            throw exception(HRM_SALARY_OPTION_SYSTEM_CANNOT_DELETE);
        }
        hrmSalaryOptionMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOptionEnabled(HrmSalaryOptionEnabledReqVO reqVO) {
        HrmSalaryOptionDO option = validateOptionExists(reqVO.getId());
        if (Boolean.TRUE.equals(option.getSystemFlag())) {
            throw exception(HRM_SALARY_OPTION_SYSTEM_CANNOT_MODIFY);
        }
        HrmSalaryOptionDO updateObj = new HrmSalaryOptionDO();
        updateObj.setId(reqVO.getId());
        updateObj.setEnabled(reqVO.getEnabled());
        hrmSalaryOptionMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOptionVisible(HrmSalaryOptionVisibleReqVO reqVO) {
        HrmSalaryOptionDO option = validateOptionExists(reqVO.getId());
        if (Boolean.TRUE.equals(option.getSystemFlag())) {
            throw exception(HRM_SALARY_OPTION_SYSTEM_CANNOT_MODIFY);
        }
        HrmSalaryOptionDO updateObj = new HrmSalaryOptionDO();
        updateObj.setId(reqVO.getId());
        updateObj.setVisible(reqVO.getVisible());
        hrmSalaryOptionMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncOption() {
        // 分类根
        for (Map.Entry<Integer, String> category : CATEGORIES.entrySet()) {
            if (hrmSalaryOptionMapper.selectByCode(category.getKey()) == null) {
                hrmSalaryOptionMapper.insert(HrmSalaryOptionDO.builder()
                        .code(category.getKey())
                        .parentCode(0)
                        .name(category.getValue())
                        .systemFlag(true)
                        .type(1)
                        .taxEnabled(false)
                        .visible(true)
                        .calculateEnabled(false)
                        .enabled(true)
                        .sort(category.getKey())
                        .build());
            }
        }
        // 叶子项
        for (Object[] spec : STANDARD_OPTIONS) {
            int code = (int) spec[0];
            if (hrmSalaryOptionMapper.selectByCode(code) != null) {
                continue; // 幂等
            }
            hrmSalaryOptionMapper.insert(HrmSalaryOptionDO.builder()
                    .code(code)
                    .parentCode((int) spec[2])
                    .name((String) spec[1])
                    .systemFlag(true)
                    .type((int) spec[4])
                    .taxEnabled((boolean) spec[3])
                    .visible(true)
                    .calculateEnabled(true)
                    .enabled(true)
                    .sort(code)
                    .build());
        }
    }

    // ========== 计税规则 ==========

    @Override
    public List<HrmSalaryTaxRuleVO> getTaxRuleList() {
        return enrichTaxRules(hrmSalaryTaxRuleMapper.selectListAll());
    }

    @Override
    public HrmSalaryTaxRuleVO getTaxRule(Long id) {
        return enrichTaxRules(Collections.singletonList(validateTaxRuleExists(id))).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTaxRule(HrmSalaryTaxRuleVO reqVO) {
        validateTaxRuleNameUnique(null, reqVO.getName());
        HrmSalaryTaxRuleDO rule = BeanUtils.toBean(reqVO, HrmSalaryTaxRuleDO.class);
        if (rule.getTaxEnabled() == null) {
            rule.setTaxEnabled(true);
        }
        hrmSalaryTaxRuleMapper.insert(rule);
        return rule.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTaxRule(HrmSalaryTaxRuleVO reqVO) {
        validateTaxRuleExists(reqVO.getId());
        validateTaxRuleNameUnique(reqVO.getId(), reqVO.getName());
        hrmSalaryTaxRuleMapper.updateById(BeanUtils.toBean(reqVO, HrmSalaryTaxRuleDO.class));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTaxRule(Long id) {
        validateTaxRuleExists(id);
        if (hrmSalaryGroupMapper.selectCountByTaxRuleId(id) > 0) {
            throw exception(HRM_SALARY_TAX_RULE_USED);
        }
        hrmSalaryTaxRuleMapper.deleteById(id);
    }

    // ========== 调薪模板 ==========

    @Override
    public List<HrmSalaryChangeTemplateVO> getChangeTemplateList() {
        return BeanUtils.toBean(hrmSalaryChangeTemplateMapper.selectListAll(), HrmSalaryChangeTemplateVO.class);
    }

    @Override
    public HrmSalaryChangeTemplateVO getChangeTemplate(Long id) {
        return BeanUtils.toBean(validateChangeTemplateExists(id), HrmSalaryChangeTemplateVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createChangeTemplate(HrmSalaryChangeTemplateVO reqVO) {
        HrmSalaryChangeTemplateDO template = BeanUtils.toBean(reqVO, HrmSalaryChangeTemplateDO.class);
        if (template.getDefaultStatus() == null) {
            template.setDefaultStatus(false);
        }
        hrmSalaryChangeTemplateMapper.insert(template);
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateChangeTemplate(HrmSalaryChangeTemplateVO reqVO) {
        validateChangeTemplateExists(reqVO.getId());
        hrmSalaryChangeTemplateMapper.updateById(BeanUtils.toBean(reqVO, HrmSalaryChangeTemplateDO.class));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChangeTemplate(Long id) {
        validateChangeTemplateExists(id);
        hrmSalaryChangeTemplateMapper.deleteById(id);
    }

    // ========== 薪资配置 ==========

    @Override
    public HrmSalaryConfigVO getConfig() {
        return BeanUtils.toBean(hrmSalaryConfigMapper.selectSingle(), HrmSalaryConfigVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createConfig(HrmSalaryConfigVO reqVO) {
        HrmSalaryConfigDO exist = hrmSalaryConfigMapper.selectSingle();
        if (exist != null) {
            return exist.getId(); // 单条：已存在返回既有
        }
        HrmSalaryConfigDO config = BeanUtils.toBean(reqVO, HrmSalaryConfigDO.class);
        hrmSalaryConfigMapper.insert(config);
        return config.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(HrmSalaryConfigVO reqVO) {
        HrmSalaryConfigDO exist = hrmSalaryConfigMapper.selectSingle();
        HrmSalaryConfigDO updateObj = BeanUtils.toBean(reqVO, HrmSalaryConfigDO.class);
        if (exist == null) {
            updateObj.setId(null);
            hrmSalaryConfigMapper.insert(updateObj);
        } else {
            updateObj.setId(exist.getId());
            hrmSalaryConfigMapper.updateById(updateObj);
        }
    }

    // ========== 私有：enrich ==========

    private List<HrmSalaryGroupVO> enrichGroups(List<HrmSalaryGroupDO> groups) {
        if (CollUtil.isEmpty(groups)) {
            return Collections.emptyList();
        }
        List<HrmSalaryGroupVO> voList = BeanUtils.toBean(groups, HrmSalaryGroupVO.class);
        Set<Long> taxRuleIds = groups.stream().map(HrmSalaryGroupDO::getTaxRuleId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> taxNameMap = taxRuleIds.isEmpty() ? Collections.emptyMap()
                : hrmSalaryTaxRuleMapper.selectByIds(taxRuleIds).stream()
                        .collect(Collectors.toMap(HrmSalaryTaxRuleDO::getId, HrmSalaryTaxRuleDO::getName, (a, b) -> a));
        Set<Long> deptIds = new HashSet<>();
        Set<Long> employeeIds = new HashSet<>();
        groups.forEach(g -> {
            if (g.getDeptIds() != null) {
                deptIds.addAll(g.getDeptIds());
            }
            if (g.getEmployeeIds() != null) {
                employeeIds.addAll(g.getEmployeeIds());
            }
        });
        Map<Long, DeptRespDTO> deptMap = deptIds.isEmpty() ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        Map<Long, String> employeeNameMap = employeeIds.isEmpty() ? Collections.emptyMap()
                : hrmEmployeeMapper.selectByIds(employeeIds).stream()
                        .collect(Collectors.toMap(HrEmployeeDO::getId, HrEmployeeDO::getName, (a, b) -> a));
        for (int i = 0; i < groups.size(); i++) {
            HrmSalaryGroupDO group = groups.get(i);
            HrmSalaryGroupVO vo = voList.get(i);
            vo.setTaxRuleName(taxNameMap.get(group.getTaxRuleId()));
            if (group.getDeptIds() != null) {
                vo.setDeptNames(group.getDeptIds().stream()
                        .map(id -> {
                            DeptRespDTO dept = deptMap.get(id);
                            return dept != null ? dept.getName() : String.valueOf(id);
                        }).collect(Collectors.toList()));
            }
            if (group.getEmployeeIds() != null) {
                vo.setEmployeeNames(group.getEmployeeIds().stream()
                        .map(id -> employeeNameMap.getOrDefault(id, String.valueOf(id)))
                        .collect(Collectors.toList()));
            }
        }
        return voList;
    }

    private List<HrmSalaryTaxRuleVO> enrichTaxRules(List<HrmSalaryTaxRuleDO> rules) {
        if (CollUtil.isEmpty(rules)) {
            return Collections.emptyList();
        }
        List<HrmSalaryTaxRuleVO> voList = BeanUtils.toBean(rules, HrmSalaryTaxRuleVO.class);
        for (int i = 0; i < rules.size(); i++) {
            voList.get(i).setUsedGroupCount(hrmSalaryGroupMapper.selectCountByTaxRuleId(rules.get(i).getId()));
        }
        return voList;
    }

    /**
     * 扁平薪资项 → 两层树（分类根 + 子项）
     */
    private List<HrmSalaryOptionVO> buildOptionTree(List<HrmSalaryOptionDO> options) {
        Map<Integer, List<HrmSalaryOptionDO>> childrenByParent = options.stream()
                .filter(o -> o.getParentCode() != null && o.getParentCode() != 0)
                .collect(Collectors.groupingBy(HrmSalaryOptionDO::getParentCode));
        List<HrmSalaryOptionVO> roots = new ArrayList<>();
        for (HrmSalaryOptionDO option : options) {
            if (option.getParentCode() != null && option.getParentCode() != 0) {
                continue; // 非根
            }
            HrmSalaryOptionVO vo = BeanUtils.toBean(option, HrmSalaryOptionVO.class);
            List<HrmSalaryOptionVO> children = childrenByParent.getOrDefault(option.getCode(), Collections.emptyList())
                    .stream().map(c -> BeanUtils.toBean(c, HrmSalaryOptionVO.class)).collect(Collectors.toList());
            vo.setChildren(children);
            roots.add(vo);
        }
        return roots;
    }

    private void applyOptionDefaults(HrmSalaryOptionDO option) {
        if (option.getParentCode() == null) {
            option.setParentCode(0);
        }
        if (option.getType() == null) {
            option.setType(1);
        }
        if (option.getTaxEnabled() == null) {
            option.setTaxEnabled(false);
        }
        if (option.getVisible() == null) {
            option.setVisible(true);
        }
        if (option.getCalculateEnabled() == null) {
            option.setCalculateEnabled(false);
        }
        if (option.getEnabled() == null) {
            option.setEnabled(true);
        }
        if (option.getSort() == null) {
            option.setSort(0);
        }
    }

    // ========== 私有：校验 ==========

    private HrmSalaryGroupDO validateGroupExists(Long id) {
        HrmSalaryGroupDO group = hrmSalaryGroupMapper.selectById(id);
        if (group == null) {
            throw exception(HRM_SALARY_GROUP_NOT_EXISTS);
        }
        return group;
    }

    private void validateGroupNameUnique(Long id, String name) {
        HrmSalaryGroupDO exist = hrmSalaryGroupMapper.selectByName(name);
        if (exist != null && (id == null || !exist.getId().equals(id))) {
            throw exception(HRM_SALARY_GROUP_NAME_DUPLICATE);
        }
    }

    private HrmSalaryOptionDO validateOptionExists(Long id) {
        HrmSalaryOptionDO option = hrmSalaryOptionMapper.selectById(id);
        if (option == null) {
            throw exception(HRM_SALARY_OPTION_NOT_EXISTS);
        }
        return option;
    }

    private HrmSalaryTaxRuleDO validateTaxRuleExists(Long id) {
        HrmSalaryTaxRuleDO rule = hrmSalaryTaxRuleMapper.selectById(id);
        if (rule == null) {
            throw exception(HRM_SALARY_TAX_RULE_NOT_EXISTS);
        }
        return rule;
    }

    private void validateTaxRuleNameUnique(Long id, String name) {
        HrmSalaryTaxRuleDO exist = hrmSalaryTaxRuleMapper.selectByName(name);
        if (exist != null && (id == null || !exist.getId().equals(id))) {
            throw exception(HRM_SALARY_TAX_RULE_NAME_DUPLICATE);
        }
    }

    private HrmSalaryChangeTemplateDO validateChangeTemplateExists(Long id) {
        HrmSalaryChangeTemplateDO template = hrmSalaryChangeTemplateMapper.selectById(id);
        if (template == null) {
            throw exception(HRM_SALARY_CHANGE_TEMPLATE_NOT_EXISTS);
        }
        return template;
    }

}
