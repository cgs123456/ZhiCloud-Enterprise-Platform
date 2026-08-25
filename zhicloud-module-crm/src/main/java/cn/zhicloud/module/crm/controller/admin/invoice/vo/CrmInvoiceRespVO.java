package cn.zhicloud.module.crm.controller.admin.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - CRM 开票 Response VO")
@Data
public class CrmInvoiceRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "25787")
    private Long id;

    @Schema(description = "开票单号", example = "FP001")
    private String no;

    @Schema(description = "合同编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Long contractId;
    @Schema(description = "合同名称", example = "智云合同")
    private String contractName;

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Long customerId;
    @Schema(description = "客户名称", example = "智云客户")
    private String customerName;

    @Schema(description = "联系人编号", example = "2")
    private Long contactId;
    @Schema(description = "联系人名称", example = "智云联系人")
    private String contactName;

    @Schema(description = "发票类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer invoiceType;

    @Schema(description = "发票号码", example = "FP001")
    private String invoiceNo;

    @Schema(description = "购方名称", example = "智云有限公司")
    private String buyerName;

    @Schema(description = "购方税号", example = "91330106MA12345678")
    private String buyerTaxNo;

    @Schema(description = "不含税金额", example = "1000.00")
    private BigDecimal amountWithoutTax;

    @Schema(description = "税额", example = "130.00")
    private BigDecimal taxAmount;

    @Schema(description = "含税金额", example = "1130.00")
    private BigDecimal amountWithTax;

    @Schema(description = "开票日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-02-02")
    private LocalDate invoiceDate;

    @Schema(description = "审批状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer auditStatus;

    @Schema(description = "工作流编号", example = "1043")
    private String processInstanceId;

    @Schema(description = "负责人的用户编号", example = "25682")
    private Long ownerUserId;
    @Schema(description = "负责人名字", example = "智云")
    private String ownerUserName;
    @Schema(description = "负责人部门", example = "研发部")
    private String ownerUserDeptName;

    @Schema(description = "发票附件 URL 列表", example = "[\"https://www.zhicloud.cn/1.pdf\"]")
    private List<String> fileUrls;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "开票明细列表")
    private List<CrmInvoiceSaveReqVO.Line> lines;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

    @Schema(description = "创建人", example = "25682")
    private String creator;
    @Schema(description = "创建人名字", example = "智云")
    private String creatorName;

}
