package cn.zhicloud.module.erp.dal.dataobject.finance;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import cn.zhicloud.module.erp.enums.finance.ErpGlVoucherStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ERP 会计凭证 DO（P0-7）
 *
 * <p>会计凭证是记录经济业务的载体，每张凭证包含多条分录（{@link ErpGlVoucherEntryDO}），
 * 遵循"有借必有贷，借贷必相等"原则。
 *
 * <p>状态流转：
 * <ul>
 *   <li>{@link ErpGlVoucherStatusEnum#DRAFT} 草稿：可修改/删除</li>
 *   <li>{@link ErpGlVoucherStatusEnum#APPROVED} 已审核：已过账，更新科目余额，不可修改</li>
 *   <li>反审核：APPROVED → DRAFT，回滚科目余额</li>
 * </ul>
 *
 * @author 智云
 */
@TableName("erp_gl_voucher")
@KeySequence("erp_gl_voucher_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpGlVoucherDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 凭证字号（如 记-001、收-001、付-001、转-001）
     */
    private String voucherNo;
    /**
     * 凭证日期
     */
    private LocalDate voucherDate;
    /**
     * 会计期间编号（关联 {@link ErpPeriodDO#getId()}）
     */
    private Long periodId;
    /**
     * 会计期间编码（冗余，便于查询）
     */
    private String periodCode;
    /**
     * 凭证类型（10 收款 / 20 付款 / 30 转账 / 40 记账）
     *
     * <p>对应中国会计准则传统分类：现金/银行收款凭证、现金/银行付款凭证、转账凭证
     */
    private Integer voucherType;
    /**
     * 附件张数
     */
    private Integer attachmentCount;
    /**
     * 凭证摘要（凭证级别，分录也可单独有摘要）
     */
    private String summary;
    /**
     * 借方合计（必须等于贷方合计，校验借贷平衡）
     */
    private BigDecimal debitTotal;
    /**
     * 贷方合计
     */
    private BigDecimal creditTotal;
    /**
     * 状态
     *
     * 枚举 {@link ErpGlVoucherStatusEnum}
     */
    private Integer status;
    /**
     * 制单人
     */
    private String preparedBy;
    /**
     * 审核人
     */
    private String approvedBy;
    /**
     * 审核时间
     */
    private java.time.LocalDateTime approvedTime;
    /**
     * 备注
     */
    private String remark;
    /**
     * 账簿 ID（多账簿支持，空表示默认主账簿）
     */
    private Long accountBookId;

}
