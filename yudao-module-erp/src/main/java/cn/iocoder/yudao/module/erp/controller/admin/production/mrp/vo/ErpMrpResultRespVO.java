package cn.iocoder.yudao.module.erp.controller.admin.production.mrp.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 物料需求计划结果 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpMrpResultRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "MRP 计划编号", example = "1")
    private Long planId;

    @Schema(description = "产品编号", example = "1")
    private Long productId;

    @Schema(description = "产品名称", example = "巧克力")
    @ExcelProperty("产品名称")
    private String productName;

    @Schema(description = "需求类型 10独立需求/20相关需求", example = "10")
    @ExcelProperty("需求类型")
    private Integer demandType;

    @Schema(description = "需求量", example = "100.00")
    @ExcelProperty("需求量")
    private BigDecimal demandQuantity;

    @Schema(description = "库存可用量", example = "20.00")
    @ExcelProperty("库存可用量")
    private BigDecimal stockQuantity;

    @Schema(description = "净需求", example = "80.00")
    @ExcelProperty("净需求")
    private BigDecimal netDemand;

    @Schema(description = "计划订单类型 10采购/20生产", example = "10")
    @ExcelProperty("计划订单类型")
    private Integer plannedOrderType;

    @Schema(description = "计划订单量", example = "80.00")
    @ExcelProperty("计划订单量")
    private BigDecimal plannedOrderQuantity;

    @Schema(description = "计划交付日")
    @ExcelProperty("计划交付日")
    private LocalDate plannedDeliveryDate;

    @Schema(description = "供应商编号", example = "1")
    private Long supplierId;

    @Schema(description = "生产车间编号", example = "1")
    private Long workshopId;

    @Schema(description = "上层产品编号", example = "1")
    private Long sourceProductId;

    @Schema(description = "上层需求量", example = "100.00")
    private BigDecimal sourceQuantity;

    @Schema(description = "备注", example = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
