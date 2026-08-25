package cn.zhicloud.module.aimultiagent.controller.admin.react;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.module.aimultiagent.config.MultiAgentProperties;
import cn.zhicloud.module.aimultiagent.controller.admin.react.vo.ReActRunReqVO;
import cn.zhicloud.module.aimultiagent.service.react.ReActAgent;
import cn.zhicloud.module.aimultiagent.service.react.ReActResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * ReAct Agent 控制器
 *
 * 提供 REST API 触发 ReAct 循环执行。
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - ReAct Agent")
@RestController
@RequestMapping("/aimultiagent/react") // 自动加 /admin-api 前缀（需在 controller.admin 包下）
@Validated
@Slf4j
public class ReActAgentController {

    @Resource
    private ReActAgent reActAgent;

    @Autowired(required = false)
    private MultiAgentProperties properties;

    @PostMapping("/run")
    @Operation(summary = "执行 ReAct Agent", description = "基于 Thought → Action → Observation 循环执行任务")
    @PreAuthorize("@ss.hasPermission('aimultiagent:react:run')")
    public CommonResult<ReActResult> run(@Valid @RequestBody ReActRunReqVO reqVO) {
        int maxSteps = reqVO.getMaxSteps() != null ? reqVO.getMaxSteps()
                : (properties != null ? properties.getReact().getMaxSteps() : 10);
        int maxTokenBudget = reqVO.getMaxTokenBudget() != null ? reqVO.getMaxTokenBudget()
                : (properties != null ? properties.getReact().getMaxTokenBudget() : 4000);
        int timeoutSeconds = reqVO.getTimeoutSeconds() != null ? reqVO.getTimeoutSeconds()
                : (properties != null ? properties.getReact().getTimeoutSeconds() : 60);
        ReActResult result = reActAgent.run(reqVO.getUserInput(), maxSteps, maxTokenBudget, timeoutSeconds);
        return success(result);
    }

}
