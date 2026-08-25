package cn.zhicloud.module.mes.controller.admin.md.ecn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - MES ECN 工程变更明细 Request VO")
@Data
public class MesMdEcnOrderItemSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "ECN 单编号", example = "1")
    private Long ecnOrderId;

    @Schema(description = "变更项（10 物料 20 数量 30 工序 40 备注）", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "变更项不能为空")
    private Integer changeItem;

    @Schema(description = "原值", example = "M001")
    private String oldValue;

    @Schema(description = "新值", example = "M002")
    private String newValue;

    @Schema(description = "原 BOM 明细编号", example = "100")
    private Long bomDetailId;

    @Schema(description = "新 BOM 明细编号", example = "101")
    private Long newBomDetailId;

    @Schema(description = "备注", example = "备注")
    private String remark;

}
