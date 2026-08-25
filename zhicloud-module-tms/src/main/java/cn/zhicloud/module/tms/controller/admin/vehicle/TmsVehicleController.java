package cn.zhicloud.module.tms.controller.admin.vehicle;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.tms.controller.admin.vehicle.vo.TmsVehiclePageReqVO;
import cn.zhicloud.module.tms.controller.admin.vehicle.vo.TmsVehicleRespVO;
import cn.zhicloud.module.tms.controller.admin.vehicle.vo.TmsVehicleSaveReqVO;
import cn.zhicloud.module.tms.dal.dataobject.vehicle.TmsVehicleDO;
import cn.zhicloud.module.tms.service.vehicle.TmsVehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - TMS 车辆")
@RestController
@RequestMapping("/tms/vehicle")
@Validated
public class TmsVehicleController {

    @Resource
    private TmsVehicleService vehicleService;

    @PostMapping("/create")
    @Operation(summary = "创建车辆")
    @PreAuthorize("@ss.hasPermission('tms:vehicle:create')")
    public CommonResult<Long> createVehicle(@Valid @RequestBody TmsVehicleSaveReqVO createReqVO) {
        return success(vehicleService.createVehicle(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新车辆")
    @PreAuthorize("@ss.hasPermission('tms:vehicle:update')")
    public CommonResult<Boolean> updateVehicle(@Valid @RequestBody TmsVehicleSaveReqVO updateReqVO) {
        vehicleService.updateVehicle(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除车辆")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('tms:vehicle:delete')")
    public CommonResult<Boolean> deleteVehicle(@RequestParam("id") Long id) {
        vehicleService.deleteVehicle(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得车辆")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('tms:vehicle:query')")
    public CommonResult<TmsVehicleRespVO> getVehicle(@RequestParam("id") Long id) {
        TmsVehicleDO vehicle = vehicleService.getVehicle(id);
        return success(BeanUtils.toBean(vehicle, TmsVehicleRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得车辆分页")
    @PreAuthorize("@ss.hasPermission('tms:vehicle:query')")
    public CommonResult<PageResult<TmsVehicleRespVO>> getVehiclePage(@Valid TmsVehiclePageReqVO pageReqVO) {
        PageResult<TmsVehicleDO> pageResult = vehicleService.getVehiclePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TmsVehicleRespVO.class));
    }

}
