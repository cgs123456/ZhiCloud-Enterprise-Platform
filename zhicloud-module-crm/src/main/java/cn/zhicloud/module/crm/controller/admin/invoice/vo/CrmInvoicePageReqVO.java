package cn.zhicloud.module.crm.controller.admin.invoice.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import cn.zhicloud.framework.common.validation.InEnum;
import cn.zhicloud.module.crm.enums.common.CrmAuditStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - CRM 开票分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmInvoicePageReqVO extends PageParam {

    @Schema(description = "开票单号", example = "FP001")
    private String no;

    @Schema(description = "客户编号", example = "2")
    private Long customerId;

    @Schema(description = "合同编号", example = "2")
    private Long contractId;

    @Schema(description = "发票类型", example = "1")
    private Integer invoiceType;

    @Schema(description = "购方名称", example = "智云有限公司")
    private String buyerName;

    @Schema(description = "审批状态", example = "20")
    @InEnum(CrmAuditStatusEnum.class)
    private Integer auditStatus;

}
