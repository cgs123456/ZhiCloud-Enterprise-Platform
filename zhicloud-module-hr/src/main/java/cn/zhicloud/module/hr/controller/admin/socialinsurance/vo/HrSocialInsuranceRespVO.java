package cn.zhicloud.module.hr.controller.admin.socialinsurance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HR 社保基数 Response VO")
@Data
public class HrSocialInsuranceRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "员工 ID", example = "2048")
    private Long employeeId;

    @Schema(description = "年份", example = "2024")
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

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}