package cn.zhicloud.module.wms.controller.admin.order.pickstrategy.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - WMS 拣货任务分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WmsPickTaskPageReqVO extends PageParam {

    @Schema(description = "任务编号", example = "PT202605110001")
    private String taskNo;

    @Schema(description = "出库单编号", example = "1024")
    private Long shipmentOrderId;

    @Schema(description = "波次单编号", example = "1024")
    private Long waveOrderId;

    @Schema(description = "SKU 编号", example = "2048")
    private Long skuId;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "拣货员用户编号", example = "1")
    private Long pickerUserId;

}
