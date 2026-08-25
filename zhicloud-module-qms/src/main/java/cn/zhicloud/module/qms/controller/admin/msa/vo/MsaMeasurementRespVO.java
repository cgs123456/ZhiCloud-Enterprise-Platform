package cn.zhicloud.module.qms.controller.admin.msa.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * QMS MSA 测量数据 Response VO
 *
 * @author 智云
 */
@Schema(description = "管理后台 - QMS MSA 测量数据 Response VO")
@Data
public class MsaMeasurementRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "研究 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long studyId;

    @Schema(description = "零件 ID", example = "1")
    private Long partId;

    @Schema(description = "评价人 ID", example = "1")
    private Long appraiserId;

    @Schema(description = "试验序号", example = "1")
    private Integer trialNo;

    @Schema(description = "测量值", requiredMode = Schema.RequiredMode.REQUIRED, example = "10.05")
    private BigDecimal measurementValue;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
