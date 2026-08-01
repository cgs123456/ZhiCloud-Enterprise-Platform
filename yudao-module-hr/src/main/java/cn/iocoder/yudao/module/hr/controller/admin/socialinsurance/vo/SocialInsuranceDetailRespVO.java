package cn.iocoder.yudao.module.hr.controller.admin.socialinsurance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - HR 月度社保明细 Response VO")
@Data
public class SocialInsuranceDetailRespVO {

    @Schema(description = "员工 ID", example = "2048")
    private Long employeeId;

    @Schema(description = "养老基数", example = "10000.00")
    private BigDecimal pensionBase;

    @Schema(description = "个人养老", example = "800.00")
    private BigDecimal personalPension;

    @Schema(description = "公司养老", example = "1600.00")
    private BigDecimal companyPension;

    @Schema(description = "个人医疗", example = "200.00")
    private BigDecimal personalMedical;

    @Schema(description = "公司医疗", example = "800.00")
    private BigDecimal companyMedical;

    @Schema(description = "个人失业", example = "50.00")
    private BigDecimal personalUnemployment;

    @Schema(description = "公司失业", example = "50.00")
    private BigDecimal companyUnemployment;

    @Schema(description = "公司工伤", example = "20.00")
    private BigDecimal companyWorkInjury;

    @Schema(description = "公司生育", example = "80.00")
    private BigDecimal companyMaternity;

    @Schema(description = "个人公积金", example = "700.00")
    private BigDecimal personalHousingFund;

    @Schema(description = "公司公积金", example = "700.00")
    private BigDecimal companyHousingFund;

    @Schema(description = "个人部分合计", example = "1750.00")
    private BigDecimal personalTotal;

    @Schema(description = "公司部分合计", example = "3250.00")
    private BigDecimal companyTotal;

}