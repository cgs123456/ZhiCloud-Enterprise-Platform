package cn.zhicloud.module.mes.controller.admin.pro.piecework.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - MES 计件工资规则分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProPieceworkRulePageReqVO extends PageParam {

    @Schema(description = "规则名称", example = "CNC 加工")
    private String ruleName;

    @Schema(description = "工序编号", example = "100")
    private Long processId;

    @Schema(description = "工艺路线编号", example = "200")
    private Long routeId;

    @Schema(description = "产品物料编号", example = "300")
    private Long itemId;

    @Schema(description = "工作站编号", example = "400")
    private Long workstationId;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "是否启用", example = "0")
    private Integer enabled;

}
