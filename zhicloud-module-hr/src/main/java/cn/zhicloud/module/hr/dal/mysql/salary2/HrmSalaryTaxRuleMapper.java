package cn.zhicloud.module.hr.dal.mysql.salary2;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.hr.dal.dataobject.salary2.HrmSalaryTaxRuleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HRM 计税规则 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface HrmSalaryTaxRuleMapper extends BaseMapperX<HrmSalaryTaxRuleDO> {

    default List<HrmSalaryTaxRuleDO> selectListAll() {
        return selectList(new LambdaQueryWrapperX<HrmSalaryTaxRuleDO>()
                .orderByDesc(HrmSalaryTaxRuleDO::getId));
    }

    default HrmSalaryTaxRuleDO selectByName(String name) {
        return selectOne(HrmSalaryTaxRuleDO::getName, name);
    }

}
