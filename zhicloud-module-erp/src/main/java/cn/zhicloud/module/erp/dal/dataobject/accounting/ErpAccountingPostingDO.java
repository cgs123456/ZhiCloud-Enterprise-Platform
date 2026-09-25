package cn.zhicloud.module.erp.dal.dataobject.accounting;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ERP 会计过账结果 DO。
 *
 * @author 智云
 */
@TableName("erp_accounting_posting")
@KeySequence("erp_accounting_posting_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAccountingPostingDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String eventId;
    private Long voucherId;
    private Long postingRuleId;
    private String ruleVersion;
    private Integer status;
    private Integer attemptNo;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime postedAt;

}
