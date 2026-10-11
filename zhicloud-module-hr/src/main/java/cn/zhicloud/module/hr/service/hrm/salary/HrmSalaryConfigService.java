package cn.zhicloud.module.hr.service.hrm.salary;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.hr.controller.admin.hrm.salary.vo.*;

import java.util.List;

/**
 * HRM 薪资配置域 Service 接口（薪资组/薪资项/计税规则/调薪模板/薪资配置）
 *
 * @author zhicloud
 */
public interface HrmSalaryConfigService {

    // ========== 薪资组 ==========

    PageResult<HrmSalaryGroupVO> getGroupPage(HrmSalaryGroupPageReqVO reqVO);

    HrmSalaryGroupVO getGroup(Long id);

    List<HrmSalaryGroupVO> getGroupSimpleList();

    Long createGroup(HrmSalaryGroupVO reqVO);

    void updateGroup(HrmSalaryGroupVO reqVO);

    void deleteGroup(Long id);

    // ========== 薪资项 ==========

    List<HrmSalaryOptionVO> getOptionList();

    List<HrmSalaryOptionVO> getOptionSimpleList();

    Long createOption(HrmSalaryOptionVO reqVO);

    void updateOption(HrmSalaryOptionVO reqVO);

    void deleteOption(Long id);

    void updateOptionEnabled(HrmSalaryOptionEnabledReqVO reqVO);

    void updateOptionVisible(HrmSalaryOptionVisibleReqVO reqVO);

    /**
     * 同步标准薪资项（首次初始化内置薪资项树）
     */
    void syncOption();

    // ========== 计税规则 ==========

    List<HrmSalaryTaxRuleVO> getTaxRuleList();

    HrmSalaryTaxRuleVO getTaxRule(Long id);

    Long createTaxRule(HrmSalaryTaxRuleVO reqVO);

    void updateTaxRule(HrmSalaryTaxRuleVO reqVO);

    void deleteTaxRule(Long id);

    // ========== 调薪模板 ==========

    List<HrmSalaryChangeTemplateVO> getChangeTemplateList();

    HrmSalaryChangeTemplateVO getChangeTemplate(Long id);

    Long createChangeTemplate(HrmSalaryChangeTemplateVO reqVO);

    void updateChangeTemplate(HrmSalaryChangeTemplateVO reqVO);

    void deleteChangeTemplate(Long id);

    // ========== 薪资配置 ==========

    HrmSalaryConfigVO getConfig();

    Long createConfig(HrmSalaryConfigVO reqVO);

    void updateConfig(HrmSalaryConfigVO reqVO);

}
