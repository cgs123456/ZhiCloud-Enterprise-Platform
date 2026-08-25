package cn.zhicloud.module.wms.controller.admin.order.wave.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - WMS 波次单新增/修改 Request VO")
@Data
public class WmsWaveOrderSaveReqVO {

    @Schema(description = "主键编号（更新时必传）")
    private Long id;

    @Schema(description = "仓库编号", required = true)
    @NotNull(message = "仓库编号不能为空")
    private Long warehouseId;

    @Schema(description = "波次策略", required = true)
    @NotNull(message = "波次策略不能为空")
    private Integer strategy;

    @Schema(description = "单据日期")
    private LocalDateTime orderTime;

    @Schema(description = "拣货员")
    private String picker;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "出库单 ID 列表（生成波次时传入）", required = true)
    @NotEmpty(message = "出库单 ID 列表不能为空")
    private List<Long> shipmentOrderIds;

}
