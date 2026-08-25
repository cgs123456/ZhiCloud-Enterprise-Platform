package cn.zhicloud.module.erp.controller.admin.finance.vo.standardcost;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 标准成本分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpStandardCostPageReqVO extends PageParam {

    @Schema(description = "产品 ID", example = "1")
    private Long productId;

    @Schema(description = "产品编码", example = "P001")
    private String productCode;

    @Schema(description = "成本项目 ID", example = "1")
    private Long costItemId;

    @Schema(description = "状态", example = "20")
    private Integer status;

}
