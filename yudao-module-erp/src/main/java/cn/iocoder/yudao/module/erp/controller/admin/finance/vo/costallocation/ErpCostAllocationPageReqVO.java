package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costallocation;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 成本分摊分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpCostAllocationPageReqVO extends PageParam {

    @Schema(description = "源成本中心编号", example = "1")
    private Long costCenterId;

    @Schema(description = "目标成本中心编号", example = "2")
    private Long targetCostCenterId;

    @Schema(description = "分摊类型", example = "10")
    private Integer allocationType;

}
