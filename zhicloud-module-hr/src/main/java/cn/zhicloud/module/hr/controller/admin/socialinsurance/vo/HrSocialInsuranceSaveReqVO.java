package cn.zhicloud.module.hr.controller.admin.socialinsurance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - HR 社保基数新增/修改 Request VO")
@Data
public class HrSocialInsuranceSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "员工 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "员工不能为空")
    private Long employeeId;

    @Schema(description = "年份", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024")
    @NotNull(message = "年份不能为空")
    private Integer year;

    @Schema(description = "养老基数", example = "10000.00")
    private BigDecimal pensionBase;

    @Schema(description = "医疗基数", example = "10000.00")
    private BigDecimal medicalBase;

    @Schema(description = "失业基数", example = "10000.00")
    private BigDecimal unemploymentBase;

    @Schema(description = "工伤基数", example = "10000.00")
    private BigDecimal workInjuryBase;

    @Schema(description = "生育基数", example = "10000.00")
    private BigDecimal maternityBase;

    @Schema(description = "公积金基数", example = "10000.00")
    private BigDecimal housingFundBase;

    @Schema(description = "个人养老比例", example = "0.0800")
    private BigDecimal personalPensionRate;

    @Schema(description = "公司养老比例", example = "0.1600")
    private BigDecimal companyPensionRate;

    @Schema(description = "备注", example = "随便")
    private String remark;

}