package cn.zhicloud.module.qms.controller.admin.sqm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import cn.zhicloud.module.qms.enums.qms.SupplierAuditStatusEnum;
import cn.zhicloud.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - QMS 供应商审核新增/修改 Request VO")
@Data
public class SupplierAuditSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "审核编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "SA202401001")
    @NotEmpty(message = "审核编号不能为空")
    private String auditNo;

    @Schema(description = "审核名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "XX 供应商 2024 年度审核")
    @NotEmpty(message = "审核名称不能为空")
    private String auditName;

    @Schema(description = "供应商 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "供应商 ID 不能为空")
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

    /**
     * 状态。**服务端只读，不参与写入**：新增由服务层强制置为初始值（§12.235.5）。
     * @InEnum 用于在边界处拒绝垃圾值（此前连枚举约束都没有）。
     */
    @Schema(description = "状态（服务端只读，不参与写入）", example = "10")
    @InEnum(SupplierAuditStatusEnum.class)
    private Integer status;

    @Schema(description = "备注", example = "随便")
    private String remark;

}