package cn.iocoder.yudao.module.crm.controller.admin.servicecloud.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - CRM 售后工单 Response VO")
@Data
@ExcelIgnoreUnannotated
public class CrmWorkOrderRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "工单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "WO20230101000001")
    @ExcelProperty("工单编号")
    private String no;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "设备故障维修")
    @ExcelProperty("标题")
    private String title;

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "18336")
    @ExcelProperty("客户编号")
    private Long customerId;
    @Schema(description = "客户名称", example = "腾讯科技")
    @ExcelProperty("客户名称")
    private String customerName;

    @Schema(description = "联系人编号", example = "18546")
    private Long contactId;
    @Schema(description = "联系人名称", example = "小豆")
    @ExcelProperty("联系人名称")
    private String contactName;

    @Schema(description = "产品编号", example = "20529")
    @ExcelProperty("产品编号")
    private Long productId;
    @Schema(description = "产品名称", example = "iPhone")
    @ExcelProperty("产品名称")
    private String productName;

    @Schema(description = "工单类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    @ExcelProperty("工单类型")
    private Integer workOrderType;

    @Schema(description = "优先级", requiredMode = Schema.RequiredMode.REQUIRED, example = "30")
    @ExcelProperty("优先级")
    private Integer priority;

    @Schema(description = "问题描述", example = "设备无法开机")
    @ExcelProperty("问题描述")
    private String description;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "处理人", example = "1024")
    @ExcelProperty("处理人")
    private Long assigneeUserId;
    @Schema(description = "处理人名称", example = "小明")
    @ExcelProperty("处理人名称")
    private String assigneeUserName;

    @Schema(description = "解决方案", example = "更换电源模块")
    @ExcelProperty("解决方案")
    private String resolution;

    @Schema(description = "响应时间")
    @ExcelProperty("响应时间")
    private LocalDateTime respondTime;

    @Schema(description = "解决时间")
    @ExcelProperty("解决时间")
    private LocalDateTime resolveTime;

    @Schema(description = "SLA 截止时间")
    @ExcelProperty("SLA 截止时间")
    private LocalDateTime slaDeadline;

    @Schema(description = "备注", example = "随便")
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

}
