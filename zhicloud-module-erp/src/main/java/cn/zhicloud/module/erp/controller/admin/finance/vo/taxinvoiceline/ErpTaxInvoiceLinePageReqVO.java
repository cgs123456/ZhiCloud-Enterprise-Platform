package cn.zhicloud.module.erp.controller.admin.finance.vo.taxinvoiceline;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 发票明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpTaxInvoiceLinePageReqVO extends PageParam {

    @Schema(description = "发票 ID", example = "1")
    private Long invoiceId;

    @Schema(description = "商品名称", example = "物料 A")
    private String productName;

}
