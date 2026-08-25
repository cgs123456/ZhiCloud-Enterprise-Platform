package cn.zhicloud.module.wms.controller.admin.order.dock.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - WMS 月台 Response VO")
@Data
public class WmsDockRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long warehouseId;

    @Schema(description = "仓库名称", example = "北京仓")
    private String warehouseName;

    @Schema(description = "月台编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "DOCK001")
    private String dockCode;

    @Schema(description = "月台名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "1号收货月台")
    private String dockName;

    @Schema(description = "月台类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer dockType;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer status;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
