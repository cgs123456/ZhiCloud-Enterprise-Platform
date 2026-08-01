package cn.iocoder.yudao.module.wms.controller.admin.inventory.alert.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - WMS 安全库存配置分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WmsSafetyStockConfigPageReqVO extends PageParam {

    @Schema(description = "仓库编号", example = "1024")
    private Long warehouseId;

    @Schema(description = "商品 SKU 编号", example = "2048")
    private Long productId;

}