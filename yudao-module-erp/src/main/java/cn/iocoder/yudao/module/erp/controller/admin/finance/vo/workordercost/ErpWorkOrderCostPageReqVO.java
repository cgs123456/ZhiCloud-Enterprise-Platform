package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.workordercost;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 工单成本归集分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpWorkOrderCostPageReqVO extends PageParam {

    @Schema(description = "工单 ID", example = "1")
    private Long workOrderId;

    @Schema(description = "工单编码", example = "WO2026070001")
    private String workOrderCode;

    @Schema(description = "产品 ID", example = "1")
    private Long productId;

    @Schema(description = "成本期间", example = "202607")
    private String costPeriod;

}
