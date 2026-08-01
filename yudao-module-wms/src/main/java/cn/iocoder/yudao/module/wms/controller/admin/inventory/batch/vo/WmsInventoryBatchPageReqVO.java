package cn.iocoder.yudao.module.wms.controller.admin.inventory.batch.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * WMS 库存批次分页 Request VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - WMS 库存批次分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WmsInventoryBatchPageReqVO extends PageParam {

    @Schema(description = "库存编号", example = "1024")
    private Long inventoryId;

    @Schema(description = "批次号", example = "BATCH202605110001")
    private String batchNo;

    @Schema(description = "批次状态", example = "AVAILABLE")
    private String status;

    @Schema(description = "生产日期-开始")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDate productionDateStart;

    @Schema(description = "生产日期-结束")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDate productionDateEnd;

    @Schema(description = "过期日期-开始")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDate expiryDateStart;

    @Schema(description = "过期日期-结束")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDate expiryDateEnd;

}
