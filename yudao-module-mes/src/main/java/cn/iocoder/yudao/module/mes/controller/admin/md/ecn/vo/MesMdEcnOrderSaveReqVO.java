package cn.iocoder.yudao.module.mes.controller.admin.md.ecn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - MES ECN 工程变更单创建/更新 Request VO")
@Data
public class MesMdEcnOrderSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "ECN 单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ECN202503001")
    @NotBlank(message = "ECN 单号不能为空")
    @Size(max = 64, message = "ECN 单号长度不能超过 64 个字符")
    private String no;

    @Schema(description = "变更名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "X 产品 BOM 替换物料")
    @NotBlank(message = "变更名称不能为空")
    @Size(max = 200, message = "变更名称长度不能超过 200 个字符")
    private String ecnName;

    @Schema(description = "变更类型（10 新增 BOM 20 修改 BOM 30 删除 BOM 40 替换物料）", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    @NotNull(message = "变更类型不能为空")
    private Integer changeType;

    @Schema(description = "原 BOM 编号", example = "1")
    private Long bomId;

    @Schema(description = "新 BOM 编号", example = "2")
    private Long newBomId;

    @Schema(description = "变更原因", example = "客户要求升级材料")
    @Size(max = 500, message = "变更原因长度不能超过 500 个字符")
    private String changeReason;

    @Schema(description = "变更说明", example = "替换 BOM 中子件 A 为子件 B")
    private String changeDescription;

    @Schema(description = "申请人", example = "1")
    private Long applicantUserId;

    @Schema(description = "生效日期", example = "2025-04-01")
    private LocalDate effectiveDate;

    @Schema(description = "备注", example = "备注")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

    @Schema(description = "变更明细列表")
    private List<MesMdEcnOrderItemSaveReqVO> items;

}
