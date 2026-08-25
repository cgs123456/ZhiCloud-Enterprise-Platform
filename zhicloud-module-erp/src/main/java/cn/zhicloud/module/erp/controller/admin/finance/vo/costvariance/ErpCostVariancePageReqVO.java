package cn.zhicloud.module.erp.controller.admin.finance.vo.costvariance;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 成本差异分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpCostVariancePageReqVO extends PageParam {

    @Schema(description = "产品 ID", example = "1")
    private Long productId;

    @Schema(description = "成本期间", example = "202607")
    private String costPeriod;

    @Schema(description = "成本项目 ID", example = "1")
    private Long costItemId;

    @Schema(description = "差异类型", example = "10")
    private Integer varianceType;

}
