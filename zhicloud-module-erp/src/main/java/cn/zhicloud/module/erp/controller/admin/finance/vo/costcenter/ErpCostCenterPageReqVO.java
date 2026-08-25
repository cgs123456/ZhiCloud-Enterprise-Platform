package cn.zhicloud.module.erp.controller.admin.finance.vo.costcenter;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 成本中心分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpCostCenterPageReqVO extends PageParam {

    @Schema(description = "成本中心编码", example = "CC1001")
    private String code;

    @Schema(description = "成本中心名称", example = "总部")
    private String name;

    @Schema(description = "状态", example = "0")
    private Integer status;

}
