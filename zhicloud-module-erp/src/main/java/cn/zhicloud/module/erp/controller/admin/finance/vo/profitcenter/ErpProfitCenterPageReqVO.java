package cn.zhicloud.module.erp.controller.admin.finance.vo.profitcenter;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 利润中心分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpProfitCenterPageReqVO extends PageParam {

    @Schema(description = "利润中心编码", example = "PC1001")
    private String code;

    @Schema(description = "利润中心名称", example = "华东事业部")
    private String name;

    @Schema(description = "状态", example = "0")
    private Integer status;

}
