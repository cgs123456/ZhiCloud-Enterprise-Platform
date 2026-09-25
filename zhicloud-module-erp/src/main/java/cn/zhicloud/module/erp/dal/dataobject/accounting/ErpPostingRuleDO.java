package cn.zhicloud.module.erp.dal.dataobject.accounting;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ERP 过账规则头 DO。
 *
 * <p>规则行见 {@link ErpPostingRuleLineDO}。规则按编码和版本不可变发布，
 * 修改已发布规则必须生成新版本，避免历史凭证无法复算。
 *
 * @author 智云
 */
@TableName("erp_posting_rule")
@KeySequence("erp_posting_rule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPostingRuleDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String ruleCode;
    private String version;
    private String eventType;
    private String conditionJson;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private Boolean enabled;
    private LocalDateTime publishedAt;
    private String remark;

}
