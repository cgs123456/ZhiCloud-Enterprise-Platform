package cn.iocoder.yudao.module.erp.controller.admin.purchase.inquiry.vo.compare;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - ERP 采购比价单生成 Request VO")
@Data
public class ErpPurchaseCompareSaveReqVO {

    @Schema(description = "编号", example = "17386")
    private Long id;

    @Schema(description = "询价单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "询价单编号不能为空")
    private Long inquiryId;

    @Schema(description = "备注", example = "推荐最低价供应商")
    private String remark;

}
