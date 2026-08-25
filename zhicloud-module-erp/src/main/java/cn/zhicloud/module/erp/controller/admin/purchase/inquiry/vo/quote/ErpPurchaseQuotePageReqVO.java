package cn.zhicloud.module.erp.controller.admin.purchase.inquiry.vo.quote;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 采购报价单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpPurchaseQuotePageReqVO extends PageParam {

    @Schema(description = "报价单号", example = "BJD001")
    private String no;

    @Schema(description = "询价单编号", example = "1024")
    private Long inquiryId;

    @Schema(description = "供应商编号", example = "1724")
    private Long supplierId;

    @Schema(description = "状态", example = "20")
    private Integer status;

    @Schema(description = "报价时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] quoteDate;

    @Schema(description = "备注", example = "你猜")
    private String remark;

}
