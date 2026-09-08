package cn.zhicloud.module.aimultiagent.controller.admin.skill;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.module.aimultiagent.controller.admin.skill.vo.MultiAgentSkillGroupRespVO;
import cn.zhicloud.module.aimultiagent.controller.admin.skill.vo.MultiAgentSkillRespVO;
import cn.zhicloud.module.aimultiagent.controller.admin.skill.vo.MultiAgentSkillSaveReqVO;
import cn.zhicloud.module.aimultiagent.service.skill.MultiAgentSkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 多 Agent 技能目录")
@RestController
@RequestMapping("/aimultiagent/skill")
@Validated
public class MultiAgentSkillController {

    @Resource
    private MultiAgentSkillService skillService;

    @GetMapping("/catalog")
    @Operation(summary = "查询完整技能目录（分组及其下属技能）")
    @PreAuthorize("@ss.hasPermission('aimultiagent:skill:query')")
    public CommonResult<List<MultiAgentSkillGroupRespVO>> getSkillCatalog() {
        return success(skillService.getSkillCatalog());
    }

    @GetMapping("/get")
    @Operation(summary = "查询技能详情")
    @Parameter(name = "id", description = "技能编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('aimultiagent:skill:query')")
    public CommonResult<MultiAgentSkillRespVO> getSkill(@RequestParam("id") Long id) {
        return success(skillService.getSkill(id));
    }

    @PostMapping("/create")
    @Operation(summary = "新建技能（config 中的 endpoint_url 会做 SSRF 校验）")
    @PreAuthorize("@ss.hasPermission('aimultiagent:skill:manage')")
    public CommonResult<Long> createSkill(@Valid @RequestBody MultiAgentSkillSaveReqVO reqVO) {
        return success(skillService.createSkill(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新技能（config 中的 endpoint_url 会做 SSRF 校验）")
    @Parameter(name = "id", description = "技能编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('aimultiagent:skill:manage')")
    public CommonResult<Boolean> updateSkill(@RequestParam("id") Long id,
                                             @Valid @RequestBody MultiAgentSkillSaveReqVO reqVO) {
        skillService.updateSkill(id, reqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新技能状态（启用/禁用）")
    @Parameter(name = "id", description = "技能编号", required = true, example = "1")
    @Parameter(name = "status", description = "状态（0开启 1关闭）", required = true, example = "0")
    @PreAuthorize("@ss.hasPermission('aimultiagent:skill:manage')")
    public CommonResult<Boolean> updateSkillStatus(@RequestParam("id") Long id,
                                                   @RequestParam("status") Integer status) {
        skillService.updateSkillStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除技能")
    @Parameter(name = "id", description = "技能编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('aimultiagent:skill:manage')")
    public CommonResult<Boolean> deleteSkill(@RequestParam("id") Long id) {
        skillService.deleteSkill(id);
        return success(true);
    }

}
