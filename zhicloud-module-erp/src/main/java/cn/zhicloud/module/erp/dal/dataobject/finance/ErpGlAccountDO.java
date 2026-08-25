package cn.zhicloud.module.erp.dal.dataobject.finance;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import cn.zhicloud.module.erp.enums.finance.ErpGlAccountBalanceDirectionEnum;
import cn.zhicloud.module.erp.enums.finance.ErpGlAccountTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 会计科目 DO（P0-7）
 *
 * <p>会计科目表（Chart of Accounts），采用树形结构（parent_id + code 全码）。
 * <ul>
 *   <li>顶级科目：parent_id = 0，作为分类汇总节点</li>
 *   <li>末级科目：is_leaf = true，可录入凭证分录</li>
 *   <li>非末级科目：仅用于汇总，不允许录入凭证</li>
 * </ul>
 *
 * <p>编码规则（参考中国会计准则）：
 * <ul>
 *   <li>1xxx 资产类</li>
 *   <li>2xxx 负债类</li>
 *   <li>3xxx 共同类</li>
 *   <li>4xxx 所有者权益类</li>
 *   <li>5xxx 成本类（并入费用）</li>
 *   <li>6xxx 损益类（收入/费用）</li>
 * </ul>
 *
 * @author 智云
 */
@TableName("erp_gl_account")
@KeySequence("erp_gl_account_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpGlAccountDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 父级编号（顶级为 0）
     */
    private Long parentId;
    /**
     * 科目编码（如 1001、100101）
     */
    private String code;
    /**
     * 科目名称（如 库存现金）
     */
    private String name;
    /**
     * 科目类型
     *
     * 枚举 {@link ErpGlAccountTypeEnum}
     */
    private Integer type;
    /**
     * 余额方向
     *
     * 枚举 {@link ErpGlAccountBalanceDirectionEnum}
     */
    private Integer balanceDirection;
    /**
     * 层级（顶级为 1，依次递增）
     */
    private Integer level;
    /**
     * 是否末级科目（true：可录入凭证分录）
     */
    private Boolean isLeaf;
    /**
     * 期初借方余额（仅末级科目有值）
     */
    private BigDecimal openingDebit;
    /**
     * 期初贷方余额（仅末级科目有值）
     */
    private BigDecimal openingCredit;
    /**
     * 当前借方累计发生额（动态维护，仅末级科目）
     */
    private BigDecimal currentDebit;
    /**
     * 当前贷方累计发生额（动态维护，仅末级科目）
     */
    private BigDecimal currentCredit;
    /**
     * 期末借方余额（动态计算/维护）
     */
    private BigDecimal closingDebit;
    /**
     * 期末贷方余额（动态计算/维护）
     */
    private BigDecimal closingCredit;
    /**
     * 状态（0 启用 / 1 禁用）
     *
     * 枚举 {@link cn.zhicloud.framework.common.enums.CommonStatusEnum}
     */
    private Integer status;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 备注
     */
    private String remark;

}
