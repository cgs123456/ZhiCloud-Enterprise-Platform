package cn.zhicloud.module.erp.dal.mysql.accounting;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.dal.dataobject.accounting.ErpPostingRuleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 过账规则头 Mapper。
 *
 * @author 智云
 */
@Mapper
public interface ErpPostingRuleMapper extends BaseMapperX<ErpPostingRuleDO> {

    default ErpPostingRuleDO selectByRuleCodeAndVersion(String ruleCode, String version) {
        return selectOne(new LambdaQueryWrapperX<ErpPostingRuleDO>()
                .eq(ErpPostingRuleDO::getRuleCode, ruleCode)
                .eq(ErpPostingRuleDO::getVersion, version));
    }

    default List<ErpPostingRuleDO> selectListByEventType(String eventType) {
        return selectList(new LambdaQueryWrapperX<ErpPostingRuleDO>()
                .eq(ErpPostingRuleDO::getEventType, eventType)
                .orderByDesc(ErpPostingRuleDO::getEffectiveFrom));
    }

}
