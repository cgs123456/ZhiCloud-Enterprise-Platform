package cn.zhicloud.module.erp.dal.mysql.accounting;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.dal.dataobject.accounting.ErpPostingRuleLineDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 过账规则分录行 Mapper。
 *
 * @author 智云
 */
@Mapper
public interface ErpPostingRuleLineMapper extends BaseMapperX<ErpPostingRuleLineDO> {

    default List<ErpPostingRuleLineDO> selectListByRuleId(Long ruleId) {
        return selectList(new LambdaQueryWrapperX<ErpPostingRuleLineDO>()
                .eq(ErpPostingRuleLineDO::getRuleId, ruleId)
                .orderByAsc(ErpPostingRuleLineDO::getLineNo));
    }

}
