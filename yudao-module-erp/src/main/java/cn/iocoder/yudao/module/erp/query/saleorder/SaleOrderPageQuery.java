package cn.iocoder.yudao.module.erp.query.saleorder;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * 销售订单分页查询参数（CQRS 试点）
 *
 * <p>读侧独立的查询参数，与写侧的 {@code ErpSaleOrderPageReqVO} 分离，
 * 读侧可针对查询场景定制字段（如不需要出库状态、退货状态等写侧关注点）。
 *
 * @author DDD 试点
 */
@Schema(description = "管理后台 - ERP 销售订单分页查询（CQRS 试点）")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SaleOrderPageQuery extends PageParam {

    @Schema(description = "销售单编号", example = "XS001")
    private String no;

    @Schema(description = "客户编号", example = "1724")
    private Long customerId;

    @Schema(description = "销售状态", example = "20")
    private Integer status;

    @Schema(description = "下单时间区间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] orderTime;

    @Schema(description = "备注", example = "加急")
    private String remark;
}
