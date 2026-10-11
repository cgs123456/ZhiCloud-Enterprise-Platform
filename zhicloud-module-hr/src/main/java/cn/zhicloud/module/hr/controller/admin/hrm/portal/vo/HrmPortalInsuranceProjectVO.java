package cn.zhicloud.module.hr.controller.admin.hrm.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 员工自助端 - 社保参保项目 VO
 *
 * 对齐前端 InsuranceSchemeProjectVO 契约。
 *
 * @author zhicloud
 */
@Schema(description = "员工自助端 - 社保参保项目 VO")
@Data
public class HrmPortalInsuranceProjectVO {

    @Schema(description = "社保项目编号")
    private Long schemeProjectId;

    @Schema(description = "社保项目类型（1-11）")
    private Integer type;

    @Schema(description = "社保项目名称")
    private String name;

    @Schema(description = "默认缴纳基数")
    private BigDecimal baseAmount;

    @Schema(description = "企业缴纳比例")
    private BigDecimal corporateRate;

    @Schema(description = "个人缴纳比例")
    private BigDecimal personalRate;

    @Schema(description = "企业缴纳金额")
    private BigDecimal corporateAmount;

    @Schema(description = "个人缴纳金额")
    private BigDecimal personalAmount;

}
