package cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - HRM 社保方案项目 VO")
@Data
public class InsuranceSchemeProjectVO {

    @Schema(description = "社保方案项目编号")
    private Long id;

    @Schema(description = "社保方案编号")
    private Long schemeId;

    @Schema(description = "项目类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "项目类型不能为空")
    private Integer type;

    @Schema(description = "项目名称")
    private String name;

    @Schema(description = "缴纳基数")
    private BigDecimal baseAmount;

    @Schema(description = "公司缴纳比例")
    private BigDecimal corporateRate;

    @Schema(description = "个人缴纳比例")
    private BigDecimal personalRate;

    @Schema(description = "公司缴纳金额")
    private BigDecimal corporateAmount;

    @Schema(description = "个人缴纳金额")
    private BigDecimal personalAmount;

}
