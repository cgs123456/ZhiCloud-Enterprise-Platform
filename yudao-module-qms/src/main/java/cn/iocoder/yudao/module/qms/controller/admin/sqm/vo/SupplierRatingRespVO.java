package cn.iocoder.yudao.module.qms.controller.admin.sqm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - QMS 供应商评级 Response VO")
@Data
public class SupplierRatingRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "评级编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "SR2024Q1001")
    private String ratingNo;

    @Schema(description = "供应商 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long supplierId;

    @Schema(description = "供应商名称", example = "XX 供应商")
    private String supplierName;

    @Schema(description = "评级周期", example = "2024-Q1")
    private String ratingPeriod;

    @Schema(description = "PPM 缺陷率", example = "80")
    private Integer ppm;

    @Schema(description = "交期达成率", example = "98.50")
    private BigDecimal onTimeRate;

    @Schema(description = "质量合格率", example = "99.50")
    private BigDecimal qualityRate;

    @Schema(description = "供应商等级", example = "A")
    private String grade;

    @Schema(description = "评级日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate ratingDate;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}