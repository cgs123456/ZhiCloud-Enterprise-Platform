package cn.zhicloud.module.qms.controller.admin.sqm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - QMS SCAR Response VO")
@Data
public class ScarRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "SCAR 单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "SCAR202401001")
    private String scarNo;

    @Schema(description = "供应商 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long supplierId;

    @Schema(description = "供应商名称", example = "XX 供应商")
    private String supplierName;

    @Schema(description = "产品 ID", example = "1024")
    private Long productId;

    @Schema(description = "产品名称", example = "XX 产品")
    private String productName;

    @Schema(description = "缺陷描述", requiredMode = Schema.RequiredMode.REQUIRED, example = "来料尺寸超差")
    private String defectDescription;

    @Schema(description = "根本原因", example = "供应商工艺参数设置不当")
    private String rootCause;

    @Schema(description = "纠正措施", example = "供应商调整工艺并增加首检")
    private String correctiveAction;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "关闭时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime closeTime;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}