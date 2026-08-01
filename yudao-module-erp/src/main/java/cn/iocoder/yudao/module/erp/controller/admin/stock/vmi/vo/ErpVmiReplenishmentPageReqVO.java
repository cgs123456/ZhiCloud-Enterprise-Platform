package cn.iocoder.yudao.module.erp.controller.admin.stock.vmi.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP VMI 补货建议分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpVmiReplenishmentPageReqVO extends PageParam {

    @Schema(description = "补货建议单号", example = "VMI20240101001")
    private String no;

    @Schema(description = "供应商编号", example = "2048")
    private Long supplierId;

    @Schema(description = "仓库编号", example = "1024")
    private Long warehouseId;

    @Schema(description = "状态", example = "10")
    private Integer status;

}
