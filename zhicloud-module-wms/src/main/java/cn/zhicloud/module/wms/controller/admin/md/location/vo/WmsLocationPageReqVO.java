package cn.zhicloud.module.wms.controller.admin.md.location.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - WMS 库位分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WmsLocationPageReqVO extends PageParam {

    @Schema(description = "仓库 ID", example = "1024")
    private Long warehouseId;

    @Schema(description = "库区 ID", example = "1024")
    private Long zoneId;

    @Schema(description = "库位编号", example = "L001")
    private String code;

    @Schema(description = "库位名称", example = "A-01-01")
    private String name;

    @Schema(description = "库位条码", example = "100001")
    private String barcode;

    @Schema(description = "库位类型", example = "10")
    private Integer type;

    @Schema(description = "状态", example = "10")
    private Integer status;

}
