package cn.zhicloud.module.tms.controller.admin.vehicle;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.tms.controller.admin.vehicle.vo.TmsFleetOperationPageReqVO;
import cn.zhicloud.module.tms.controller.admin.vehicle.vo.TmsFleetOperationRespVO;
import cn.zhicloud.module.tms.controller.admin.vehicle.vo.TmsFleetOperationSaveReqVO;
import cn.zhicloud.module.tms.dal.dataobject.vehicle.TmsFleetOperationDO;
import cn.zhicloud.module.tms.service.vehicle.TmsFleetOperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - TMS 车队运营
 *
 * @author 智云
 */
@Tag(name = "管理后台 - TMS 车队运营")
@RestController
@RequestMapping("/tms/fleet-operation")
@PreAuthorize("@ss.hasPermission('tms:fleet-operation:query')")
public class TmsFleetOperationController {

    @Resource
    private TmsFleetOperationService fleetOperationService;

    @PostMapping("/create")
    @Operation(summary = "创建车队运营记录")
    @PreAuthorize("@ss.hasPermission('tms:fleet-operation:create')")
    public CommonResult<Long> createFleetOperation(@Valid @RequestBody TmsFleetOperationSaveReqVO createReqVO) {
        return success(fleetOperationService.createFleetOperation(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新车队运营记录")
    @PreAuthorize("@ss.hasPermission('tms:fleet-operation:update')")
    public CommonResult<Boolean> updateFleetOperation(@Valid @RequestBody TmsFleetOperationSaveReqVO updateReqVO) {
        fleetOperationService.updateFleetOperation(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除车队运营记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('tms:fleet-operation:delete')")
    public CommonResult<Boolean> deleteFleetOperation(@RequestParam("id") Long id) {
        fleetOperationService.deleteFleetOperation(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取车队运营记录")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<TmsFleetOperationRespVO> getFleetOperation(@RequestParam("id") Long id) {
        TmsFleetOperationDO operation = fleetOperationService.getFleetOperation(id);
        return success(BeanUtils.toBean(operation, TmsFleetOperationRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取车队运营分页")
    public CommonResult<PageResult<TmsFleetOperationRespVO>> getFleetOperationPage(@Valid TmsFleetOperationPageReqVO pageReqVO) {
        PageResult<TmsFleetOperationDO> pageResult = fleetOperationService.getFleetOperationPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TmsFleetOperationRespVO.class));
    }

}
