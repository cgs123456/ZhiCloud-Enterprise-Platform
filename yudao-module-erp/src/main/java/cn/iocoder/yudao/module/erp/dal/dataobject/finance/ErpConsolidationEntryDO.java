package cn.iocoder.yudao.module.erp.dal.dataobject.finance;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpGlAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpPeriodDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 合并报表抵消分录 DO
 *
 * <p>用于集团内关联交易抵消，支持合并资产负债表/合并利润表生成。
 *
 * <p>典型抵消场景：
 * <ul>
 *   <li>母公司对子公司投资 ↔ 子公司权益</li>
 *   <li>集团内部应收/应付</li>
 *   <li>集团内部销售/采购收入成本抵消</li>
 *   <li>集团内部固定资产交易未实现利润</li>
 * </ul>
 *
 * @author 芋道源码
 */
@TableName("erp_consolidation_entry")
@KeySequence("erp_consolidation_entry_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpConsolidationEntryDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 合并任务编号（如 CONS-202607）
     */
    private String consolidationNo;
    /**
     * 会计期间编号
     *
     * 关联 {@link ErpPeriodDO#getId()}
     */
    private Long periodId;
    /**
     * 期间编码（冗余）
     */
    private String periodCode;
    /**
     * 抵消类型（10=投资权益抵消 INVESTMENT_EQUITY；20=内部应收应付 INTERCOMPANY_AR_AP；
     *           30=内部销售成本 INTERCOMPANY_SALE_COGS；40=内部固定资产 INTERCOMPANY_FA）
     */
    private Integer eliminationType;
    /**
     * 借方科目编号
     *
     * 关联 {@link ErpGlAccountDO#getId()}
     */
    private Long debitAccountId;
    /**
     * 借方科目编码（冗余）
     */
    private String debitAccountCode;
    /**
     * 借方科目名称（冗余）
     */
    private String debitAccountName;
    /**
     * 贷方科目编号
     *
     * 关联 {@link ErpGlAccountDO#getId()}
     */
    private Long creditAccountId;
    /**
     * 贷方科目编码（冗余）
     */
    private String creditAccountCode;
    /**
     * 贷方科目名称（冗余）
     */
    private String creditAccountName;
    /**
     * 抵消金额
     */
    private BigDecimal eliminationAmount;
    /**
     * 状态（10=草稿 DRAFT；20=已审核 APPROVED）
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
