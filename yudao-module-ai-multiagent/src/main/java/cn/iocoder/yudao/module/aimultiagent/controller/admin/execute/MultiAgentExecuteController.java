package cn.iocoder.yudao.module.aimultiagent.controller.admin.execute;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.aimultiagent.controller.admin.execute.vo.MultiAgentExecuteLogRespVO;
import cn.iocoder.yudao.module.aimultiagent.controller.admin.execute.vo.MultiAgentExecuteReqVO;
import cn.iocoder.yudao.module.aimultiagent.dal.dataobject.MultiAgentExecutionLogDO;
import cn.iocoder.yudao.module.aimultiagent.service.execute.MultiAgentExecuteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 多 Agent 编排执行")
@RestController
@RequestMapping("/aimultiagent/execute")
@Validated
@Slf4j
public class MultiAgentExecuteController {

    @Resource
    private MultiAgentExecuteService executeService;

    @PostMapping("/run")
    @Operation(summary = "执行多 Agent 编排")
    @PreAuthorize("@ss.hasPermission('aimultiagent:execute:run')")
    public CommonResult<MultiAgentExecuteLogRespVO> execute(@Valid @RequestBody MultiAgentExecuteReqVO reqVO) {
        // 租户 ID 一律取自服务端登录上下文（TenantContextHolder），禁止信任请求体传入的 tenantId，防止越权（IDOR）
        MultiAgentExecutionLogDO logDO = executeService.execute(reqVO.getTopologyId(),
                reqVO.getUserInput(), TenantContextHolder.getRequiredTenantId());
        return success(BeanUtils.toBean(logDO, MultiAgentExecuteLogRespVO.class));
    }

    @GetMapping("/log")
    @Operation(summary = "查询执行日志")
    @Parameter(name = "id", description = "日志编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('aimultiagent:execute:query')")
    public CommonResult<MultiAgentExecuteLogRespVO> getExecutionLog(@RequestParam("id") Long id) {
        MultiAgentExecutionLogDO logDO = executeService.getExecutionLog(id);
        return success(BeanUtils.toBean(logDO, MultiAgentExecuteLogRespVO.class));
    }

    @GetMapping("/log/list")
    @Operation(summary = "按拓扑 ID 查询执行日志列表")
    @Parameter(name = "topologyId", description = "拓扑编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('aimultiagent:execute:query')")
    public CommonResult<List<MultiAgentExecuteLogRespVO>> getExecutionLogListByTopologyId(
            @RequestParam("topologyId") Long topologyId) {
        List<MultiAgentExecutionLogDO> list = executeService.getExecutionLogListByTopologyId(topologyId);
        return success(BeanUtils.toBean(list, MultiAgentExecuteLogRespVO.class));
    }

}
