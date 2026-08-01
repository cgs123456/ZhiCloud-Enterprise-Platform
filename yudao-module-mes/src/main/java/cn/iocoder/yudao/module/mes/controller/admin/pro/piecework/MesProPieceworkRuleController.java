package cn.iocoder.yudao.module.mes.controller.admin.pro.piecework;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.piecework.vo.MesProPieceworkRulePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.piecework.vo.MesProPieceworkRuleRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.piecework.vo.MesProPieceworkRuleSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.piecework.MesProPieceworkRuleDO;
import cn.iocoder.yudao.module.mes.service.pro.piecework.MesProPieceworkRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES 计件工资规则")
@RestController
@RequestMapping("/mes/pro-piecework-rule")
@Validated
public class MesProPieceworkRuleController {

    @Resource
    private MesProPieceworkRuleService pieceworkRuleService;

    @PostMapping("/create")
    @Operation(summary = "创建计件工资规则")
    @PreAuthorize("@ss.hasPermission('mes:pro-piecework-rule:create')")
    public CommonResult<Long> createPieceworkRule(@Valid @RequestBody MesProPieceworkRuleSaveReqVO createReqVO) {
        return success(pieceworkRuleService.createPieceworkRule(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新计件工资规则")
    @PreAuthorize("@ss.hasPermission('mes:pro-piecework-rule:update')")
    public CommonResult<Boolean> updatePieceworkRule(@Valid @RequestBody MesProPieceworkRuleSaveReqVO updateReqVO) {
        pieceworkRuleService.updatePieceworkRule(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除计件工资规则")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-piecework-rule:delete')")
    public CommonResult<Boolean> deletePieceworkRule(@RequestParam("id") Long id) {
        pieceworkRuleService.deletePieceworkRule(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得计件工资规则")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:pro-piecework-rule:query')")
    public CommonResult<MesProPieceworkRuleRespVO> getPieceworkRule(@RequestParam("id") Long id) {
        MesProPieceworkRuleDO rule = pieceworkRuleService.getPieceworkRule(id);
        return success(BeanUtils.toBean(rule, MesProPieceworkRuleRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得计件工资规则分页")
    @PreAuthorize("@ss.hasPermission('mes:pro-piecework-rule:query')")
    public CommonResult<PageResult<MesProPieceworkRuleRespVO>> getPieceworkRulePage(@Valid MesProPieceworkRulePageReqVO pageReqVO) {
        PageResult<MesProPieceworkRuleDO> pageResult = pieceworkRuleService.getPieceworkRulePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MesProPieceworkRuleRespVO.class));
    }

}
