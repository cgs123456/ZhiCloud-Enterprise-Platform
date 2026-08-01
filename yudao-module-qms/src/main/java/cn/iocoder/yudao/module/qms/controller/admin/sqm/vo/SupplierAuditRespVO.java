package cn.iocoder.yudao.module.qms.controller.admin.sqm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - QMS 供应商审核 Response VO")
@Data
public class SupplierAuditRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "审核编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "SA202401001")
    private String auditNo;

    @Schema(description = "审核名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "XX 供应商 2024 年度审核")
    private String auditName;

    @Schema(description = "供应商 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long supplierId;

    @Schema(description = "供应商名称", example = "XX 供应商")
    private String supplierName;

    @Schema(description = "审核类型", example = "20")
    private Integer auditType;

    @Schema(description = "计划日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate plannedDate;

    @Schema(description = "实际日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualDate;

    @Schema(description = "审核员", example = "李审核")
    private String auditor;

    @Schema(description = "审核结论", example = "10")
    private Integer conclusion;

    @Schema(description = "审核报告", example = "审核通过，详见报告附件")
    private String auditReport;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}