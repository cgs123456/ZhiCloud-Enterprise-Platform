package cn.iocoder.yudao.module.erp.controller.admin.purchase.inquiry.vo.compare;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 采购比价单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpPurchaseComparePageReqVO extends PageParam {

    @Schema(description = "比价单号", example = "BJB001")
    private String no;

    @Schema(description = "询价单编号", example = "1024")
    private Long inquiryId;

    @Schema(description = "状态", example = "20")
    private Integer status;

    @Schema(description = "推荐供应商编号", example = "1724")
    private Long recommendSupplierId;

}
