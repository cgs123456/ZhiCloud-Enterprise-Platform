package cn.zhicloud.module.wms.controller.admin.md.sn.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - WMS 序列号分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WmsSnPageReqVO extends PageParam {

    @Schema(description = "序列号", example = "SN20260730001")
    private String sn;

    @Schema(description = "商品编号", example = "1")
    private Long productId;

    @Schema(description = "库存批次编号", example = "10")
    private Long batchId;

    @Schema(description = "仓库编号", example = "1")
    private Long warehouseId;

    @Schema(description = "状态", example = "IN_STOCK")
    private String status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}