package cn.zhicloud.module.mes.controller.admin.pro.route;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.mes.controller.admin.pro.route.vo.capacity.MesProRouteCapacityRespVO;
import cn.zhicloud.module.mes.controller.admin.pro.route.vo.capacity.MesProRouteCapacitySaveReqVO;
import cn.zhicloud.module.mes.dal.dataobject.pro.route.MesProRouteCapacityDO;
import cn.zhicloud.module.mes.service.pro.route.MesProRouteCapacityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES 工艺路线产能")
@RestController
@RequestMapping("/mes/pro/route-capacity")
@Validated
public class MesProRouteCapacityController {

    @Resource
    private MesProRouteCapacityService routeCapacityService;

    @GetMapping("/get-by-route")
    @Operation(summary = "按工艺路线获得产能配置")
    @Parameter(name = "routeId", description = "工艺路线编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:query')")
    public CommonResult<MesProRouteCapacityRespVO> getRouteCapacityByRoute(@RequestParam("routeId") Long routeId) {
        MesProRouteCapacityDO capacity = routeCapacityService.getRouteCapacityByRouteId(routeId);
        return success(BeanUtils.toBean(capacity, MesProRouteCapacityRespVO.class));
    }

    @PutMapping("/save")
    @Operation(summary = "保存工艺路线产能（upsert）")
    @PreAuthorize("@ss.hasPermission('mes:pro-route:update')")
    public CommonResult<Long> saveRouteCapacity(@Valid @RequestBody MesProRouteCapacitySaveReqVO saveReqVO) {
        return success(routeCapacityService.saveRouteCapacity(saveReqVO));
    }

}
