package cn.iocoder.yudao.module.aimultiagent.controller.admin.topology;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aimultiagent.controller.admin.topology.vo.MultiAgentTopologyPageReqVO;
import cn.iocoder.yudao.module.aimultiagent.controller.admin.topology.vo.MultiAgentTopologyRespVO;
import cn.iocoder.yudao.module.aimultiagent.controller.admin.topology.vo.MultiAgentTopologySaveReqVO;
import cn.iocoder.yudao.module.aimultiagent.dal.dataobject.MultiAgentTopologyDO;
import cn.iocoder.yudao.module.aimultiagent.service.topology.MultiAgentTopologyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 多 Agent 拓扑配置")
@RestController
@RequestMapping("/aimultiagent/topology")
@Validated
@Slf4j
public class MultiAgentTopologyController {

    @Resource
    private MultiAgentTopologyService topologyService;

    @PostMapping("/create")
    @Operation(summary = "创建多 Agent 拓扑配置")
    @PreAuthorize("@ss.hasPermission('aimultiagent:topology:create')")
    public CommonResult<Long> createTopology(@Valid @RequestBody MultiAgentTopologySaveReqVO createReqVO) {
        return success(topologyService.createTopology(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新多 Agent 拓扑配置")
    @PreAuthorize("@ss.hasPermission('aimultiagent:topology:update')")
    public CommonResult<Boolean> updateTopology(@Valid @RequestBody MultiAgentTopologySaveReqVO updateReqVO) {
        topologyService.updateTopology(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除多 Agent 拓扑配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('aimultiagent:topology:delete')")
    public CommonResult<Boolean> deleteTopology(@RequestParam("id") Long id) {
        topologyService.deleteTopology(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取多 Agent 拓扑配置详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('aimultiagent:topology:query')")
    public CommonResult<MultiAgentTopologyRespVO> getTopology(@RequestParam("id") Long id) {
        MultiAgentTopologyDO topology = topologyService.getTopology(id);
        return success(BeanUtils.toBean(topology, MultiAgentTopologyRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取多 Agent 拓扑配置分页")
    @PreAuthorize("@ss.hasPermission('aimultiagent:topology:query')")
    public CommonResult<PageResult<MultiAgentTopologyRespVO>> getTopologyPage(@Valid MultiAgentTopologyPageReqVO pageReqVO) {
        PageResult<MultiAgentTopologyDO> pageResult = topologyService.getTopologyPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MultiAgentTopologyRespVO.class));
    }

}
