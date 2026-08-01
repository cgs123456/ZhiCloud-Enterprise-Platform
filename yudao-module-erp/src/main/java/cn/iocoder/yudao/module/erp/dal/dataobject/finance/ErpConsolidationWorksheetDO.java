package cn.iocoder.yudao.module.erp.dal.dataobject.finance;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.erp.enums.finance.ErpConsolidationEliminationTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpWorksheetStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 合并工作底稿 DO（P1-合并报表引擎）
 *
 * <p>记录自动抵消引擎生成的抵消分录结果，按合并周期 + 母子公司 + 抵消类型维度存储。
 *
 * <p>典型抵消类型：
 * <ul>
 *   <li>{@link ErpConsolidationEliminationTypeEnum#INVESTMENT_EQUITY} 投资权益抵消</li>
 *   <li>{@link ErpConsolidationEliminationTypeEnum#INTERCOMPANY_AR_AP} 内部应收应付抵消</li>
 *   <li>{@link ErpConsolidationEliminationTypeEnum#INTERCOMPANY_SALE_COGS} 内部销售成本抵消</li>
 *   <li>{@link ErpConsolidationEliminationTypeEnum#INTERCOMPANY_FA} 内部固定资产抵消</li>
 * </ul>
 *
 * @author 芋道源码
 */
@TableName("erp_consolidation_worksheet")
@KeySequence("erp_consolidation_worksheet_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpConsolidationWorksheetDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 合并周期（yyyyMM，如 202607）
     */
    private String consolidationPeriod;
    /**
     * 母公司编号
     */
    private Long parentCompanyId;
    /**
     * 子公司编号
     */
    private Long subsidiaryCompanyId;
    /**
     * 抵消类型
     *
     * 枚举 {@link ErpConsolidationEliminationTypeEnum}
     */
    private Integer eliminationType;
    /**
     * 抵消金额
     */
    private BigDecimal eliminationAmount;
    /**
     * 抵消描述
     */
    private String description;
    /**
     * 状态
     *
     * 枚举 {@link ErpWorksheetStatusEnum}
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
