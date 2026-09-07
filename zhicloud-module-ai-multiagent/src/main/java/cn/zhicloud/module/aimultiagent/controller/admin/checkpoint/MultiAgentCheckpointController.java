package cn.zhicloud.module.aimultiagent.controller.admin.checkpoint;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.aimultiagent.controller.admin.execute.vo.MultiAgentCheckpointRespVO;
import cn.zhicloud.module.aimultiagent.dal.dataobject.MultiAgentCheckpointDO;
import cn.zhicloud.module.aimultiagent.service.execute.MultiAgentExecuteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 多 Agent 编排检查点")
@RestController
@RequestMapping("/aimultiagent/checkpoint")
@Validated
public class MultiAgentCheckpointController {

    @Resource
    private MultiAgentExecuteService executeService;

    @GetMapping("/list")
    @Operation(summary = "查询某次执行的检查点列表（按写入顺序）")
    @Parameter(name = "executionLogId", description = "执行日志编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('aimultiagent:execute:query')")
    public CommonResult<List<MultiAgentCheckpointRespVO>> getCheckpointList(
            @RequestParam("executionLogId") Long executionLogId) {
        List<MultiAgentCheckpointDO> list = executeService.getCheckpoints(executionLogId);
        return success(BeanUtils.toBean(list, MultiAgentCheckpointRespVO.class));
    }

}
