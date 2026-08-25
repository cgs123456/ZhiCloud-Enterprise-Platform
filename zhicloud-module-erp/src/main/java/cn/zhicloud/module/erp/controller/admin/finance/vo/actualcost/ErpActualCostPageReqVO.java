package cn.zhicloud.module.erp.controller.admin.finance.vo.actualcost;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 实际成本分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpActualCostPageReqVO extends PageParam {

    @Schema(description = "产品编号", example = "1")
    private Long productId;

    @Schema(description = "成本期间", example = "202607")
    private String costPeriod;

    @Schema(description = "成本项目编号", example = "1")
    private Long costItemId;

    @Schema(description = "产品编码", example = "P001")
    private String productCode;

}
