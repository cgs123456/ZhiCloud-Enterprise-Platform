package cn.zhicloud.module.crm.controller.admin.salesorder.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - CRM 销售订单 Response VO")
@Data
@ExcelIgnoreUnannotated
public class CrmSaleOrderRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "10430")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "SO20230101000001")
    @ExcelProperty("订单编号")
    private String no;

    @Schema(description = "关联合同编号", example = "10864")
    @ExcelProperty("关联合同编号")
    private Long contractId;
    @Schema(description = "关联合同名称", example = "合同A")
    @ExcelProperty("关联合同名称")
    private String contractName;

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "18336")
    @ExcelProperty("客户编号")
    private Long customerId;
    @Schema(description = "客户名称", example = "18336")
    @ExcelProperty("客户名称")
    private String customerName;

    @Schema(description = "商机编号", example = "10864")
    @ExcelProperty("商机编号")
    private Long businessId;
    @Schema(description = "商机名称", example = "商机A")
    @ExcelProperty("商机名称")
    private String businessName;

    @Schema(description = "联系人编号", example = "18546")
    private Long contactId;
    @Schema(description = "联系人名称", example = "小豆")
    @ExcelProperty("联系人名称")
    private String contactName;

    @Schema(description = "下单日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("下单日期")
    private LocalDateTime orderDate;

    @Schema(description = "交货日期")
    @ExcelProperty("交货日期")
    private LocalDateTime deliveryDate;

    @Schema(description = "总金额", example = "19510")
    @ExcelProperty("总金额")
    private BigDecimal totalAmount;

    @Schema(description = "折扣金额", example = "100")
    @ExcelProperty("折扣金额")
    private BigDecimal discountAmount;

    @Schema(description = "最终金额", example = "19410")
    @ExcelProperty("最终金额")
    private BigDecimal finalAmount;

    @Schema(description = "付款状态", example = "10")
    @ExcelProperty("付款状态")
    private Integer paymentStatus;

    @Schema(description = "发货状态", example = "10")
    @ExcelProperty("发货状态")
    private Integer deliveryStatus;

    @Schema(description = "订单状态", example = "10")
    @ExcelProperty("订单状态")
    private Integer status;

    @Schema(description = "负责人的用户编号", example = "25682")
    @ExcelProperty("负责人的用户编号")
    private Long ownerUserId;
    @Schema(description = "负责人名字", example = "25682")
    @ExcelProperty("负责人名字")
    private String ownerUserName;
    @Schema(description = "负责人部门")
    @ExcelProperty("负责人部门")
    private String ownerUserDeptName;

    @Schema(description = "工作流编号", example = "1043")
    @ExcelProperty("工作流编号")
    private String processInstanceId;

    @Schema(description = "备注", example = "你猜")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建人", example = "25682")
    @ExcelProperty("创建人")
    private String creator;

    @Schema(description = "创建人名字", example = "test")
    @ExcelProperty("创建人名字")
    private String creatorName;

    @Schema(description = "订单明细列表")
    private List<Item> items;

    @Schema(description = "订单明细")
    @Data
    public static class Item {

        @Schema(description = "编号", example = "888")
        private Long id;

        @Schema(description = "订单编号", example = "10430")
        private Long orderId;

        @Schema(description = "产品编号", example = "20529")
        private Long productId;

        @Schema(description = "产品名称", example = "iPhone")
        private String productName;

        @Schema(description = "数量", example = "10")
        private BigDecimal quantity;

        @Schema(description = "单价", example = "999.00")
        private BigDecimal unitPrice;

        @Schema(description = "折扣", example = "50.00")
        private BigDecimal discount;

        @Schema(description = "金额", example = "9990.00")
        private BigDecimal amount;

        @Schema(description = "税率", example = "0.13")
        private BigDecimal taxRate;

        @Schema(description = "税额", example = "1298.70")
        private BigDecimal taxAmount;

        @Schema(description = "备注", example = "随便")
        private String remark;

    }

}
