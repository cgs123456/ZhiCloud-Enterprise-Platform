package cn.iocoder.yudao.module.erp.dal.dataobject.finance;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.erp.enums.finance.ErpConsolidationMethodEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpConsolidationScopeStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ERP 合并范围 DO（P1-合并报表引擎）
 *
 * <p>记录母子公司持股关系及合并方法，是自动抵消引擎的核心配置。
 * <ul>
 *   <li>同一母子公司组合唯一（uk_parent_subsidiary）</li>
 *   <li>持股比例（holding_ratio）支持小数（如 0.65 表示 65%）</li>
 *   <li>合并方法（{@link ErpConsolidationMethodEnum}）决定抵消算法</li>
 * </ul>
 *
 * @author 芋道源码
 */
@TableName("erp_consolidation_scope")
@KeySequence("erp_consolidation_scope_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpConsolidationScopeDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 母公司编号
     */
    private Long parentCompanyId;
    /**
     * 子公司编号
     */
    private Long subsidiaryCompanyId;
    /**
     * 持股比例（0~1，例如 0.65 表示 65%）
     */
    private BigDecimal holdingRatio;
    /**
     * 合并方法
     *
     * 枚举 {@link ErpConsolidationMethodEnum}
     */
    private Integer consolidationMethod;
    /**
     * 生效日期
     */
    private LocalDate effectiveDate;
    /**
     * 失效日期
     */
    private LocalDate expiryDate;
    /**
     * 状态
     *
     * 枚举 {@link ErpConsolidationScopeStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
