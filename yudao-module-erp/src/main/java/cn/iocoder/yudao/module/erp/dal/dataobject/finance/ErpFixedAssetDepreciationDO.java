package cn.iocoder.yudao.module.erp.dal.dataobject.finance;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFixedAssetDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpPeriodDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ERP 固定资产折旧记录 DO
 *
 * <p>记录每次折旧计算结果，每月一条。用于审计追溯与 GL 凭证生成。
 *
 * @author 芋道源码
 */
@TableName("erp_fixed_asset_depreciation")
@KeySequence("erp_fixed_asset_depreciation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpFixedAssetDepreciationDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 固定资产编号
     *
     * 关联 {@link ErpFixedAssetDO#getId()}
     */
    private Long fixedAssetId;
    /**
     * 资产编码（冗余）
     */
    private String assetCode;
    /**
     * 资产名称（冗余）
     */
    private String assetName;
    /**
     * 会计期间编号
     *
     * 关联 {@link ErpPeriodDO#getId()}
     */
    private Long periodId;
    /**
     * 期间编码（如 202607）
     */
    private String periodCode;
    /**
     * 折旧日期
     */
    private LocalDate depreciationDate;
    /**
     * 本月折旧额
     */
    private BigDecimal depreciationAmount;
    /**
     * 累计折旧额（截至本月）
     */
    private BigDecimal accumulatedDepreciation;
    /**
     * 账面净值（本月末）
     */
    private BigDecimal netBookValue;
    /**
     * 已折旧月数（截至本月）
     */
    private Integer depreciatedMonths;
    /**
     * 折旧方法（10=直线法）
     */
    private Integer depreciationMethod;
    /**
     * 状态（10=待审核 DRAFT；20=已审核 APPROVED）
     */
    private Integer status;
    /**
     * 生成的凭证编号（关联 ErpGlVoucherDO.id，审核时生成）
     */
    private Long voucherId;
    /**
     * 凭证编号字符串（折旧审核时生成，占位实现）
     */
    private String voucherNo;
    /**
     * 备注
     */
    private String remark;

}
