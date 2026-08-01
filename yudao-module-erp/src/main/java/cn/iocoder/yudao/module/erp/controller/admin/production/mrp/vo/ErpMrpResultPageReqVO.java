package cn.iocoder.yudao.module.erp.controller.admin.production.mrp.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 物料需求计划结果分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpMrpResultPageReqVO extends PageParam {

    @Schema(description = "MRP 计划编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long planId;

    @Schema(description = "产品编号", example = "1")
    private Long productId;

    @Schema(description = "需求类型 10独立需求/20相关需求", example = "10")
    private Integer demandType;

    @Schema(description = "计划订单类型 10采购/20生产", example = "10")
    private Integer plannedOrderType;

}
