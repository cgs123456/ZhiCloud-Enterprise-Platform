package cn.iocoder.yudao.module.wms.controller.admin.order.asn.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - WMS ASN 到货通知单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WmsAsnOrderPageReqVO extends PageParam {

    @Schema(description = "ASN 编号", example = "ASN202605110001")
    private String no;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "仓库编号", example = "1024")
    private Long warehouseId;

    @Schema(description = "供应商编号", example = "1024")
    private Long supplierId;

    @Schema(description = "月台编号", example = "1024")
    private Long dockId;

    @Schema(description = "运输方式", example = "10")
    private Integer transportMode;

    @Schema(description = "车牌号", example = "京A12345")
    private String vehicleNo;

    @Schema(description = "预计到货时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] expectedArrivalTime;

}
