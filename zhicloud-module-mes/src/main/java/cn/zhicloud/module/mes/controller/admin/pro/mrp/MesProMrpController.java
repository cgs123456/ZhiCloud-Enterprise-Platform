package cn.zhicloud.module.mes.controller.admin.pro.mrp;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.mes.controller.admin.pro.mrp.vo.MesProMrpPlanCreateReqVO;
import cn.zhicloud.module.mes.controller.admin.pro.mrp.vo.MesProMrpPlanPageReqVO;
import cn.zhicloud.module.mes.controller.admin.pro.mrp.vo.MesProMrpPlanRespVO;
import cn.zhicloud.module.mes.controller.admin.pro.mrp.vo.MesProMrpResultRespVO;
import cn.zhicloud.module.mes.dal.dataobject.pro.mrp.MesProMrpPlanDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.mrp.MesProMrpResultDO;
import cn.zhicloud.module.mes.service.pro.mrp.MesProMrpService;
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

@Tag(name = "管理后台 - MES MRP 物料需求计划")
@RestController
@RequestMapping("/mes/pro/mrp")
@Validated
public class MesProMrpController {

    @Resource
    private MesProMrpService mrpService;

    @PostMapping("/plan/create")
    @Operation(summary = "创建 MRP 计划")
    @PreAuthorize("@ss.hasPermission('mes:pro-mrp:create')")
    public CommonResult<Long> createMrpPlan(@Valid @RequestBody MesProMrpPlanCreateReqVO createReqVO) {
        return success(mrpService.createMrpPlan(createReqVO));
    }

    @PostMapping("/calculate")
    @Operation(summary = "触发 MRP 计算")
    @Parameter(name = "planId", description = "MRP 计划编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:pro-mrp:calculate')")
    public CommonResult<List<MesProMrpResultRespVO>> calculateMrp(@RequestParam("planId") Long planId) {
        List<MesProMrpResultDO> list = mrpService.calculateMrp(planId);
        return success(BeanUtils.toBean(list, MesProMrpResultRespVO.class));
    }

    @GetMapping("/result/{planId}")
    @Operation(summary = "获取 MRP 计算结果")
    @Parameter(name = "planId", description = "MRP 计划编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:pro-mrp:query')")
    public CommonResult<List<MesProMrpResultRespVO>> getMrpResult(@PathVariable("planId") Long planId) {
        List<MesProMrpResultDO> list = mrpService.getMrpResult(planId);
        return success(BeanUtils.toBean(list, MesProMrpResultRespVO.class));
    }

    @GetMapping("/plan/list")
    @Operation(summary = "MRP 计划列表")
    @PreAuthorize("@ss.hasPermission('mes:pro-mrp:query')")
    public CommonResult<PageResult<MesProMrpPlanRespVO>> getMrpPlanPage(@Valid MesProMrpPlanPageReqVO pageReqVO) {
        PageResult<MesProMrpPlanDO> pageResult = mrpService.getMrpPlanPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MesProMrpPlanRespVO.class));
    }

    @GetMapping("/plan/get")
    @Operation(summary = "获得 MRP 计划")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:pro-mrp:query')")
    public CommonResult<MesProMrpPlanRespVO> getMrpPlan(@RequestParam("id") Long id) {
        MesProMrpPlanDO plan = mrpService.getMrpPlan(id);
        return success(BeanUtils.toBean(plan, MesProMrpPlanRespVO.class));
    }

}
