package cn.zhicloud.module.erp.dal.dataobject.finance;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 会计凭证分录 DO（P0-7）
 *
 * <p>每张凭证包含多条分录，遵循"有借必有贷，借贷必相等"。
 * 每条分录对应一个末级会计科目 + 借方或贷方金额。
 *
 * <p>规则：
 * <ul>
 *   <li>借方金额与贷方金额不能同时为 0，也不能同时大于 0（同一条分录）</li>
 *   <li>所有分录的借方合计必须等于贷方合计（在 {@link ErpGlVoucherDO#debitTotal} 校验）</li>
 *   <li>科目必须是末级科目（{@link ErpGlAccountDO#getIsLeaf()} = true）</li>
 * </ul>
 *
 * @author 智云
 */
@TableName("erp_gl_voucher_entry")
@KeySequence("erp_gl_voucher_entry_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpGlVoucherEntryDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 凭证编号（关联 {@link ErpGlVoucherDO#getId()}）
     */
    private Long voucherId;
    /**
     * 科目编号（关联 {@link ErpGlAccountDO#getId()}）
     */
    private Long accountId;
    /**
     * 科目编码（冗余，便于查询）
     */
    private String accountCode;
    /**
     * 科目名称（冗余，便于查询）
     */
    private String accountName;
    /**
     * 摘要（分录级别，可单独描述本笔分录）
     */
    private String summary;
    /**
     * 借方金额（与贷方金额互斥）
     */
    private BigDecimal debitAmount;
    /**
     * 贷方金额（与借方金额互斥）
     */
    private BigDecimal creditAmount;
    /**
     * 排序号（凭证内顺序）
     */
    private Integer sort;

}
