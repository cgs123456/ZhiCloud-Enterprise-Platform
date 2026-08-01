package cn.iocoder.yudao.module.erp.controller.admin.stock.vmi.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP VMI 库存分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpVmiInventoryPageReqVO extends PageParam {

    @Schema(description = "供应商编号", example = "2048")
    private Long supplierId;

    @Schema(description = "仓库编号", example = "1024")
    private Long warehouseId;

    @Schema(description = "产品编号", example = "4096")
    private Long productId;

    @Schema(description = "产品名称", example = "键盘")
    private String productName;

}
