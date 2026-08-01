package cn.iocoder.yudao.module.wms.controller.admin.order.wave.vo.order;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - WMS 波次单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WmsWaveOrderPageReqVO extends PageParam {

    @Schema(description = "波次单号")
    private String no;

    @Schema(description = "仓库编号")
    private Long warehouseId;

    @Schema(description = "波次策略")
    private Integer strategy;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "拣货员")
    private String picker;

    @Schema(description = "下单时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] orderTime;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
