package cn.zhicloud.module.fms.dal.dataobject.config;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * FMS 账套参数 DO
 *
 * @author 智云
 */
@TableName("fms_finance_parameter")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FmsFinanceParameterDO extends TenantBaseDO {

    @TableId
    private Long id;
    /** 账套编号 */
    private Long accountSetId;
    /** 会计制度 */
    private Integer standard;
    /** 科目层级 */
    private Integer level;
    /** 科目编码规则，如 4-2-2-2 */
    private String subjectCodeRule;
    /** 账簿余额方向模式 */
    private Integer ledgerBalanceMode;
    /** 结账前是否要求凭证审核 */
    private Boolean voucherReviewRequired;

}
