package cn.zhicloud.module.wms.controller.admin.order.crossdock.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - WMS 越库单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WmsCrossDockOrderPageReqVO extends PageParam {

    @Schema(description = "越库单号", example = "CD202605110001")
    private String no;

    @Schema(description = "越库状态", example = "10")
    private Integer status;

    @Schema(description = "源头供应商编号", example = "1024")
    private Long sourceSupplierId;

    @Schema(description = "目标客户编号", example = "2048")
    private Long targetCustomerId;

    @Schema(description = "关联入库单号", example = "RK202605110001")
    private String receiptOrderNo;

    @Schema(description = "关联出库单号", example = "CK202605110001")
    private String shipmentOrderNo;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
