package cn.zhicloud.module.qms.controller.admin.complaint.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - QMS 客户投诉 Response VO")
@Data
public class CustomerComplaintRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "投诉编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "CP20240101001")
    private String complaintNo;

    @Schema(description = "客户 ID", example = "2048")
    private Long customerId;

    @Schema(description = "客户名称", example = "XX 公司")
    private String customerName;

    @Schema(description = "产品 ID", example = "1024")
    private Long productId;

    @Schema(description = "产品名称", example = "XX 产品")
    private String productName;

    @Schema(description = "投诉内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "外观不良")
    private String complaintContent;

    @Schema(description = "投诉日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate complaintDate;

    @Schema(description = "调查根因", example = "工装松动")
    private String rootCause;

    @Schema(description = "影响范围", example = "2024 年 1 月批次")
    private String impactScope;

    @Schema(description = "处理方式", example = "10")
    private Integer handleType;

    @Schema(description = "处理措施描述", example = "退货并整改")
    private String handleAction;

    @Schema(description = "关联 8D 报告 ID", example = "3072")
    private Long eightDId;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer status;

    @Schema(description = "关闭时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime closeTime;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}