package cn.zhicloud.module.aimultiagent.controller.admin.execute.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 多 Agent 编排执行 Request VO")
@Data
public class MultiAgentExecuteReqVO {

    @Schema(description = "拓扑 ID", required = true, example = "1")
    @NotNull(message = "拓扑 ID 不能为空")
    private Long topologyId;

    @Schema(description = "用户输入", required = true, example = "帮我生成一份本月库存报告")
    @NotEmpty(message = "用户输入不能为空")
    private String userInput;

}
