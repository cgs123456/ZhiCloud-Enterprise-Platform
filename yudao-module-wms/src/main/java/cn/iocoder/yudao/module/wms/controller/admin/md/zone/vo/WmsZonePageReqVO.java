package cn.iocoder.yudao.module.wms.controller.admin.md.zone.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - WMS 库区分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WmsZonePageReqVO extends PageParam {

    @Schema(description = "仓库 ID", example = "1024")
    private Long warehouseId;

    @Schema(description = "库区编号", example = "Z001")
    private String code;

    @Schema(description = "库区名称", example = "存储区A")
    private String name;

    @Schema(description = "库区类型", example = "10")
    private Integer type;

}
