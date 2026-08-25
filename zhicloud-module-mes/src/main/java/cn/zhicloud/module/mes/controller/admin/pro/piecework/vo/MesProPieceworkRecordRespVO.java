package cn.zhicloud.module.mes.controller.admin.pro.piecework.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 计件工资明细 Response VO")
@Data
public class MesProPieceworkRecordRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "报工单编号", example = "2048")
    private Long feedbackId;

    @Schema(description = "报工用户编号", example = "100")
    private Long feedbackUserId;

    @Schema(description = "报工用户姓名", example = "张三")
    private String feedbackUserName;

    @Schema(description = "生产工单编号", example = "200")
    private Long workOrderId;

    @Schema(description = "工序编号", example = "300")
    private Long processId;

    @Schema(description = "产品物料编号", example = "400")
    private Long itemId;

    @Schema(description = "工作站编号", example = "500")
    private Long workstationId;

    @Schema(description = "合格品数量", example = "100")
    private BigDecimal qualifiedQty;

    @Schema(description = "废品数量", example = "5")
    private BigDecimal scrapQty;

    @Schema(description = "工废数量", example = "2")
    private BigDecimal laborScrapQty;

    @Schema(description = "合格品单价（元/件）", example = "1.50")
    private BigDecimal unitPrice;

    @Schema(description = "废品单价（元/件）", example = "0.00")
    private BigDecimal scrapUnitPrice;

    @Schema(description = "工资金额合计", example = "150.00")
    private BigDecimal totalAmount;

    @Schema(description = "所属月份（yyyyMM）", example = "202607")
    private String periodMonth;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
