package cn.iocoder.yudao.module.crm.controller.admin.salesorder.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - CRM 销售订单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmSaleOrderPageReqVO extends PageParam {

    @Schema(description = "订单编号", example = "SO20230101")
    private String no;

    @Schema(description = "客户编号", example = "18336")
    private Long customerId;

    @Schema(description = "关联合同编号", example = "10864")
    private Long contractId;

    @Schema(description = "商机编号", example = "10864")
    private Long businessId;

    @Schema(description = "订单状态", example = "10")
    private Integer status;

    @Schema(description = "付款状态", example = "10")
    private Integer paymentStatus;

    @Schema(description = "创建时间", example = "[2023-01-01 00:00:00, 2023-01-31 23:59:59]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
