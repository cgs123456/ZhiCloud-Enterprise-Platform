package cn.iocoder.yudao.module.wms.controller.admin.inventory.batch.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * WMS 批次效期预警分页 Request VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - WMS 批次效期预警分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WmsBatchExpiryAlertPageReqVO extends PageParam {

    @Schema(description = "仓库编号", example = "1024")
    private Long warehouseId;

    @Schema(description = "商品 SKU 编号", example = "2048")
    private Long productId;

    @Schema(description = "批次号", example = "BATCH202605110001")
    private String batchNo;

    @Schema(description = "状态（0 未处理 1 已确认 2 已处理）", example = "0")
    private Integer status;

    @Schema(description = "预警时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] alertTime;

}
