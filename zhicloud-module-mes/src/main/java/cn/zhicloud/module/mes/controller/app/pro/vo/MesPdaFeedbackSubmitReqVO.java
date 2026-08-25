package cn.zhicloud.module.mes.controller.app.pro.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "用户 APP - PDA 报工提交 Request VO")
@Data
public class MesPdaFeedbackSubmitReqVO {

    @Schema(description = "工单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "WO202503001")
    @NotEmpty(message = "工单编号不能为空")
    private String workOrderNo;

    @Schema(description = "工序编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "工序编号不能为空")
    private Long processId;

    @Schema(description = "本次报工数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "报工数量不能为空")
    private BigDecimal feedbackQuantity;

    @Schema(description = "合格品数量", example = "98")
    private BigDecimal qualifiedQuantity;

    @Schema(description = "不良品数量", example = "2")
    private BigDecimal unqualifiedQuantity;

    @Schema(description = "报工用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "报工用户编号不能为空")
    private Long feedbackUserId;

    @Schema(description = "备注", example = "PDA 报工")
    private String remark;

}
