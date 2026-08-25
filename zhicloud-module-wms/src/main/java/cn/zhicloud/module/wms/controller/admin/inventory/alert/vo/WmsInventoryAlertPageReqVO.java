package cn.zhicloud.module.wms.controller.admin.inventory.alert.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - WMS 库存预警分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WmsInventoryAlertPageReqVO extends PageParam {

    @Schema(description = "预警类型", example = "LOW_STOCK")
    private String alertType;

    @Schema(description = "仓库编号", example = "1024")
    private Long warehouseId;

    @Schema(description = "商品 SKU 编号", example = "2048")
    private Long productId;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "预警时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] alertTime;

}