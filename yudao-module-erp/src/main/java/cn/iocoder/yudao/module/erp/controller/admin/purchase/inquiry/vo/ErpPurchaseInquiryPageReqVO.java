package cn.iocoder.yudao.module.erp.controller.admin.purchase.inquiry.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 采购询价单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpPurchaseInquiryPageReqVO extends PageParam {

    @Schema(description = "询价单号", example = "XJD001")
    private String no;

    @Schema(description = "询价主题", example = "原料询价")
    private String inquiryName;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "期望交货日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDate[] expectedDeliveryDate;

    @Schema(description = "备注", example = "你猜")
    private String remark;

    @Schema(description = "创建者")
    private String creator;

}
