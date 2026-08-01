package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stockserial;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 库存序列号分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpStockSerialPageReqVO extends PageParam {

    @Schema(description = "序列号", example = "SN2026070001")
    private String serialNo;

    @Schema(description = "产品编号", example = "1")
    private Long productId;

    @Schema(description = "仓库编号", example = "1")
    private Long warehouseId;

    @Schema(description = "批次编号", example = "1")
    private Long batchId;

    @Schema(description = "序列号状态", example = "10")
    private Integer status;

}
