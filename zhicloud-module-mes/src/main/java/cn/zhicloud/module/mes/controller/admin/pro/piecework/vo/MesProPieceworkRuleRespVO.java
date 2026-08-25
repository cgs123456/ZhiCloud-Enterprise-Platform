package cn.zhicloud.module.mes.controller.admin.pro.piecework.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 计件工资规则 Response VO")
@Data
public class MesProPieceworkRuleRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "规则名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "CNC 加工计件规则")
    private String ruleName;

    @Schema(description = "工序编号", example = "100")
    private Long processId;

    @Schema(description = "工序名称", example = "车削")
    private String processName;

    @Schema(description = "工艺路线编号", example = "200")
    private Long routeId;

    @Schema(description = "产品物料编号", example = "300")
    private Long itemId;

    @Schema(description = "产品物料名称", example = "壳体")
    private String itemName;

    @Schema(description = "工作站编号", example = "400")
    private Long workstationId;

    @Schema(description = "工作站名称", example = "工作站 A")
    private String workstationName;

    @Schema(description = "合格品单价（元/件）", example = "1.50")
    private BigDecimal qualifiedUnitPrice;

    @Schema(description = "废品单价（元/件）", example = "0.00")
    private BigDecimal scrapUnitPrice;

    @Schema(description = "阶梯单价配置（JSON）")
    private String stepConfig;

    @Schema(description = "生效日期")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期")
    private LocalDate expireDate;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "是否启用", example = "0")
    private Integer enabled;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
