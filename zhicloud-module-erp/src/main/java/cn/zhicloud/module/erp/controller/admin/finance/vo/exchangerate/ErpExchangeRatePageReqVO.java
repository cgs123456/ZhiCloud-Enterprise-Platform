package cn.zhicloud.module.erp.controller.admin.finance.vo.exchangerate;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 汇率分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpExchangeRatePageReqVO extends PageParam {

    @Schema(description = "源币种编号", example = "1")
    private Long fromCurrencyId;

    @Schema(description = "目标币种编号", example = "2")
    private Long toCurrencyId;

}
