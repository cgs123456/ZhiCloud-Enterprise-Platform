package cn.iocoder.yudao.module.erp.controller.admin.production.mps.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 主生产计划 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpMpsPlanRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "计划编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "MPS-001")
    @ExcelProperty("计划编号")
    private String planNo;

    @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long productId;

    @Schema(description = "产品编码", example = "P001")
    @ExcelProperty("产品编码")
    private String productCode;

    @Schema(description = "产品名称", example = "巧克力")
    @ExcelProperty("产品名称")
    private String productName;

    @Schema(description = "计划周期", requiredMode = Schema.RequiredMode.REQUIRED, example = "202607")
    @ExcelProperty("计划周期")
    private String planPeriod;

    @Schema(description = "计划类型", example = "10")
    @ExcelProperty("计划类型")
    private Integer planType;

    @Schema(description = "需求日期")
    @ExcelProperty("需求日期")
    private LocalDate demandDate;

    @Schema(description = "计划数量", example = "100.00")
    @ExcelProperty("计划数量")
    private BigDecimal plannedQuantity;

    @Schema(description = "计划完工日期")
    @ExcelProperty("计划完工日期")
    private LocalDate plannedFinishDate;

    @Schema(description = "来源", example = "10")
    @ExcelProperty("来源")
    private Integer source;

    @Schema(description = "来源订单编号", example = "1")
    private Long sourceOrderId;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "备注", example = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "创建人", example = "芋道")
    private String creator;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "计划明细列表")
    private List<ErpMpsPlanDetailRespVO> details;

}