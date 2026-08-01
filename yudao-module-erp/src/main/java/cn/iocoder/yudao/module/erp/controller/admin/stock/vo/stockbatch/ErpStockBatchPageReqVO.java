package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stockbatch;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 库存批次分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpStockBatchPageReqVO extends PageParam {

    @Schema(description = "批次号", example = "B2026070001")
    private String batchNo;

    @Schema(description = "产品编号", example = "1024")
    private Long productId;

    @Schema(description = "仓库编号", example = "1")
    private Long warehouseId;

    @Schema(description = "批次状态", example = "10")
    private Integer status;

}
