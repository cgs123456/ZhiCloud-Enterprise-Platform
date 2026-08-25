package cn.zhicloud.module.erp.controller.admin.finance.vo.taxrate;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 税率分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpTaxRatePageReqVO extends PageParam {

    @Schema(description = "税率编码", example = "VAT13")
    private String code;

    @Schema(description = "税率名称", example = "增值税 13%")
    private String name;

    @Schema(description = "税率类型", example = "10")
    private Integer rateType;

    @Schema(description = "状态", example = "0")
    private Integer status;

}
