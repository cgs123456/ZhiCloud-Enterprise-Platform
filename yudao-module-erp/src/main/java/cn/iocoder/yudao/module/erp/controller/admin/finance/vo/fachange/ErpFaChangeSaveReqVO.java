package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.fachange;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - ERP 固定资产变动新增/修改 Request VO")
@Data
public class ErpFaChangeSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "固定资产编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "固定资产编号不能为空")
    private Long assetId;

    @Schema(description = "变动类型（10 部门转移 / 20 状态变动 / 30 原值调整 / 40 使用年限调整 / 50 残值调整 / 60 折旧方法变更）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "变动类型不能为空")
    private Integer changeType;

    @Schema(description = "变更前值", example = "100000.00")
    private String beforeValue;

    @Schema(description = "变更后值", example = "120000.00")
    private String afterValue;

    @Schema(description = "变更日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "变更日期不能为空")
    private LocalDate changeDate;

    @Schema(description = "变更原因", example = "资产评估增值")
    private String changeReason;

    @Schema(description = "操作员编号", example = "1")
    private Long operatorId;

    @Schema(description = "备注", example = "备注")
    private String remark;

}
