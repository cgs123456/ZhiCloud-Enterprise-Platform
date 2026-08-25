package cn.zhicloud.module.qms.controller.admin.msa.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - QMS MSA 测量数据新增/修改 Request VO")
@Data
public class MsaMeasurementSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "研究 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "研究 ID 不能为空")
    private Long studyId;

    @Schema(description = "零件 ID", example = "1")
    private Long partId;

    @Schema(description = "评价人 ID", example = "1")
    private Long appraiserId;

    @Schema(description = "试验序号", example = "1")
    private Integer trialNo;

    @Schema(description = "测量值", requiredMode = Schema.RequiredMode.REQUIRED, example = "10.05")
    @NotNull(message = "测量值不能为空")
    private BigDecimal measurementValue;

    @Schema(description = "备注", example = "随便")
    private String remark;

}
