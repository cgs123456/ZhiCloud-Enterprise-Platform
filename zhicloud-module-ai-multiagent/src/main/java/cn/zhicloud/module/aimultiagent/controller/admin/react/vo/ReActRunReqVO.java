package cn.zhicloud.module.aimultiagent.controller.admin.react.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Schema(description = "管理后台 - ReAct Agent 执行 Request VO")
@Data
public class ReActRunReqVO {

    @Schema(description = "用户输入", required = true, example = "查询所有用户的年龄信息")
    @NotEmpty(message = "用户输入不能为空")
    private String userInput;

    @Schema(description = "最大步数", example = "10")
    @Positive(message = "最大步数必须为正数")
    private Integer maxSteps = 10;

    @Schema(description = "Token 预算上限", example = "4000")
    @Positive(message = "Token 预算必须为正数")
    private Integer maxTokenBudget = 4000;

    @Schema(description = "超时秒数", example = "60")
    @Positive(message = "超时秒数必须为正数")
    private Integer timeoutSeconds = 60;

}
