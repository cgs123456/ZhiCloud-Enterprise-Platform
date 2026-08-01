package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costitem;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 成本项目分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpCostItemPageReqVO extends PageParam {

    @Schema(description = "成本项目编码", example = "CI001")
    private String code;

    @Schema(description = "成本项目名称", example = "直接材料")
    private String name;

    @Schema(description = "类型", example = "10")
    private Integer type;

    @Schema(description = "状态", example = "0")
    private Integer status;

}
