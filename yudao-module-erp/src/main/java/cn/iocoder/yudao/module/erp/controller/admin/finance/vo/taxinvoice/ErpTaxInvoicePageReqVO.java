package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.taxinvoice;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 发票分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpTaxInvoicePageReqVO extends PageParam {

    @Schema(description = "发票号", example = "12345678")
    private String invoiceNo;

    @Schema(description = "发票类型", example = "10")
    private Integer invoiceType;

    @Schema(description = "状态", example = "20")
    private Integer status;

    @Schema(description = "购方名称", example = "杭州XX公司")
    private String buyerName;

    @Schema(description = "销方名称", example = "上海YY公司")
    private String sellerName;

}
