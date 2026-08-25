package cn.zhicloud.module.erp.controller.admin.finance.vo.currency;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 币种分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpCurrencyPageReqVO extends PageParam {

    @Schema(description = "币种编码", example = "CNY")
    private String code;

    @Schema(description = "币种名称", example = "人民币")
    private String name;

    @Schema(description = "是否启用", example = "0")
    private Integer enabled;

    @Schema(description = "是否本位币", example = "true")
    private Boolean isBase;

}
