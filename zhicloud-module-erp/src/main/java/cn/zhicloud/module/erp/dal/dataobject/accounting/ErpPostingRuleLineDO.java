package cn.zhicloud.module.erp.dal.dataobject.accounting;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * ERP 过账规则分录行 DO。
 *
 * @author 智云
 */
@TableName("erp_posting_rule_line")
@KeySequence("erp_posting_rule_line_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPostingRuleLineDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long ruleId;
    private Integer lineNo;
    private Integer entrySide;
    private String accountCode;
    private String accountResolver;
    private String amountExpression;
    private String conditionJson;
    private String remark;

}
