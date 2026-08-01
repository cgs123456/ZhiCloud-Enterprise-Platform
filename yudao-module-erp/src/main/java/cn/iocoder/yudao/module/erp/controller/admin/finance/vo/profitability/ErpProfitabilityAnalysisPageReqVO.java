package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.profitability;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 获利能力分析分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpProfitabilityAnalysisPageReqVO extends PageParam {

    @Schema(description = "利润中心编号", example = "1")
    private Long profitCenterId;

    @Schema(description = "会计期间编号", example = "1")
    private Long periodId;

}
