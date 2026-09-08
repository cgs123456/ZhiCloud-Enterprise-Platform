package cn.zhicloud.module.aimultiagent.controller.admin.trace;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.module.aimultiagent.controller.admin.trace.vo.MultiAgentTraceRespVO;
import cn.zhicloud.module.aimultiagent.service.trace.MultiAgentSpanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 多 Agent 执行轨迹")
@RestController
@RequestMapping("/aimultiagent/trace")
@Validated
public class MultiAgentTraceController {

    @Resource
    private MultiAgentSpanService spanService;

    @GetMapping("/spans")
    @Operation(summary = "查询某次执行的完整轨迹（Span 列表 + 汇总）")
    @Parameter(name = "executionLogId", description = "执行日志编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('aimultiagent:execute:query')")
    public CommonResult<MultiAgentTraceRespVO> getTrace(
            @RequestParam("executionLogId") Long executionLogId) {
        return success(spanService.getTrace(executionLogId));
    }

}
