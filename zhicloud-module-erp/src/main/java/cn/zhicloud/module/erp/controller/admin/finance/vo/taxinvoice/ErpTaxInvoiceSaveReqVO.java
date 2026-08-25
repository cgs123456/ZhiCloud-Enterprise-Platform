package cn.zhicloud.module.erp.controller.admin.finance.vo.taxinvoice;

import cn.zhicloud.framework.common.validation.InEnum;
import cn.zhicloud.module.erp.controller.admin.finance.vo.taxinvoiceline.ErpTaxInvoiceLineSaveReqVO;
import cn.zhicloud.module.erp.enums.finance.tax.ErpInvoiceStatusEnum;
import cn.zhicloud.module.erp.enums.finance.tax.ErpInvoiceTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - ERP 发票新增/修改 Request VO")
@Data
public class ErpTaxInvoiceSaveReqVO {

    @Schema(description = "编号（更新时必填）", example = "1024")
    private Long id;

    @Schema(description = "发票号", requiredMode = Schema.RequiredMode.REQUIRED, example = "12345678")
    @NotBlank(message = "发票号不能为空")
    private String invoiceNo;

    @Schema(description = "发票代码", requiredMode = Schema.RequiredMode.REQUIRED, example = "110000000000")
    @NotBlank(message = "发票代码不能为空")
    private String invoiceCode;

    @Schema(description = "发票类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "发票类型不能为空")
    @InEnum(ErpInvoiceTypeEnum.class)
    private Integer invoiceType;

    @Schema(description = "购方名称", example = "杭州XX公司")
    private String buyerName;

    @Schema(description = "购方税号", example = "91330100000000000X")
    private String buyerTaxNo;

    @Schema(description = "销方名称", example = "上海YY公司")
    private String sellerName;

    @Schema(description = "销方税号", example = "91310000000000000Y")
    private String sellerTaxNo;

    @Schema(description = "开票日期", example = "2026-07-29")
    private LocalDate invoiceDate;

    @Schema(description = "不含税金额", example = "1000.00")
    private BigDecimal amountWithoutTax;

    @Schema(description = "税额", example = "130.00")
    private BigDecimal taxAmount;

    @Schema(description = "价税合计", example = "1130.00")
    private BigDecimal amountWithTax;

    @Schema(description = "状态", example = "10")
    @InEnum(ErpInvoiceStatusEnum.class)
    private Integer status;

    @Schema(description = "来源单据类型", example = "sale_out")
    private String sourceOrderType;

    @Schema(description = "来源单据 ID", example = "1")
    private Long sourceOrderId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "发票明细列表")
    private List<ErpTaxInvoiceLineSaveReqVO> lines;

}
