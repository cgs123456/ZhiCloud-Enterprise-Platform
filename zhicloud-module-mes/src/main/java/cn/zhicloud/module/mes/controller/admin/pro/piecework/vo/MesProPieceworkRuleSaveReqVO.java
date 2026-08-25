package cn.zhicloud.module.mes.controller.admin.pro.piecework.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - MES 计件工资规则新增/修改 Request VO")
@Data
public class MesProPieceworkRuleSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "规则名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "CNC 加工计件规则")
    @NotEmpty(message = "规则名称不能为空")
    private String ruleName;

    @Schema(description = "工序编号", example = "100")
    private Long processId;

    @Schema(description = "工艺路线编号", example = "200")
    private Long routeId;

    @Schema(description = "产品物料编号", example = "300")
    private Long itemId;

    @Schema(description = "工作站编号", example = "400")
    private Long workstationId;

    @Schema(description = "合格品单价（元/件）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1.50")
    @NotNull(message = "合格品单价不能为空")
    private BigDecimal qualifiedUnitPrice;

    @Schema(description = "废品单价（元/件）", example = "0.00")
    private BigDecimal scrapUnitPrice;

    @Schema(description = "阶梯单价配置（JSON）")
    private String stepConfig;

    @Schema(description = "生效日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "生效日期不能为空")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "失效日期不能为空")
    private LocalDate expireDate;

    @Schema(description = "状态（0 开启 1 关闭）", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "是否启用（0 启用 1 停用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "是否启用不能为空")
    private Integer enabled;

    @Schema(description = "备注", example = "备注")
    private String remark;

}
