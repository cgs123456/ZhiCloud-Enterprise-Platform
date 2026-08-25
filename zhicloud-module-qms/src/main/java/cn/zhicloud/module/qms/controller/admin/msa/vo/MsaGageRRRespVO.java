package cn.zhicloud.module.qms.controller.admin.msa.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * QMS MSA GR&R 分析结果 VO
 *
 * <p>基于均值极差法（Xbar-R）计算：%GR&R = (EV + AV) / TV * 100%。
 * <ul>
 *   <li>%GR&R < 10%：可接受</li>
 *   <li>10% <= %GR&R <= 30%：有条件接受</li>
 *   <li>%GR&R > 30%：不可接受</li>
 * </ul>
 *
 * @author 智云
 */
@Schema(description = "管理后台 - QMS MSA GR&R 分析结果 Response VO")
@Data
public class MsaGageRRRespVO {

    @Schema(description = "研究 ID")
    private Long studyId;

    @Schema(description = "重复性 EV（Equipment Variation）")
    private BigDecimal ev;

    @Schema(description = "再现性 AV（Appraiser Variation）")
    private BigDecimal av;

    @Schema(description = "重复性和再现性 Gage R&R")
    private BigDecimal gageRR;

    @Schema(description = "零件变异 PV（Part Variation）")
    private BigDecimal pv;

    @Schema(description = "总变异 TV（Total Variation）")
    private BigDecimal tv;

    @Schema(description = "%EV = EV / TV * 100%")
    private BigDecimal percentEV;

    @Schema(description = "%AV = AV / TV * 100%")
    private BigDecimal percentAV;

    @Schema(description = "%GR&R = (EV + AV) / TV * 100%")
    private BigDecimal percentGageRR;

    @Schema(description = "%PV = PV / TV * 100%")
    private BigDecimal percentPV;

    @Schema(description = "评价结论", example = "可接受")
    private String conclusion;

    @Schema(description = "样本数据条数")
    private Integer sampleCount;

}
