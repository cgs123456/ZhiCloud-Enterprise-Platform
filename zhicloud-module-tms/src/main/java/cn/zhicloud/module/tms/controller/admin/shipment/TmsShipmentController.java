package cn.zhicloud.module.tms.controller.admin.shipment;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.tms.controller.admin.shipment.vo.TmsShipmentDispatchReqVO;
import cn.zhicloud.module.tms.controller.admin.shipment.vo.TmsShipmentPageReqVO;
import cn.zhicloud.module.tms.controller.admin.shipment.vo.TmsShipmentRespVO;
import cn.zhicloud.module.tms.controller.admin.shipment.vo.TmsShipmentSaveReqVO;
import cn.zhicloud.module.tms.controller.admin.shipment.vo.TmsShipmentStopRespVO;
import cn.zhicloud.module.tms.dal.dataobject.shipment.TmsShipmentDO;
import cn.zhicloud.module.tms.dal.dataobject.shipment.TmsShipmentStopDO;
import cn.zhicloud.module.tms.service.shipment.TmsShipmentService;
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

@Tag(name = "管理后台 - TMS 运单")
@RestController
@RequestMapping("/tms/shipment")
@Validated
public class TmsShipmentController {

    @Resource
    private TmsShipmentService shipmentService;

    @PostMapping("/create")
    @Operation(summary = "创建运单")
    @PreAuthorize("@ss.hasPermission('tms:shipment:create')")
    public CommonResult<Long> createShipment(@Valid @RequestBody TmsShipmentSaveReqVO createReqVO) {
        return success(shipmentService.createShipment(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新运单")
    @PreAuthorize("@ss.hasPermission('tms:shipment:update')")
    public CommonResult<Boolean> updateShipment(@Valid @RequestBody TmsShipmentSaveReqVO updateReqVO) {
        shipmentService.updateShipment(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除运单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('tms:shipment:delete')")
    public CommonResult<Boolean> deleteShipment(@RequestParam("id") Long id) {
        shipmentService.deleteShipment(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得运单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('tms:shipment:query')")
    public CommonResult<TmsShipmentRespVO> getShipment(@RequestParam("id") Long id) {
        TmsShipmentDO shipment = shipmentService.getShipment(id);
        return success(BeanUtils.toBean(shipment, TmsShipmentRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得运单分页")
    @PreAuthorize("@ss.hasPermission('tms:shipment:query')")
    public CommonResult<PageResult<TmsShipmentRespVO>> getShipmentPage(@Valid TmsShipmentPageReqVO pageReqVO) {
        PageResult<TmsShipmentDO> pageResult = shipmentService.getShipmentPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TmsShipmentRespVO.class));
    }

    @GetMapping("/stop-list")
    @Operation(summary = "获得运单站点列表")
    @Parameter(name = "shipmentId", description = "运单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('tms:shipment:query')")
    public CommonResult<List<TmsShipmentStopRespVO>> getShipmentStopList(
            @RequestParam("shipmentId") Long shipmentId) {
        List<TmsShipmentStopDO> list = shipmentService.getShipmentStopList(shipmentId);
        return success(BeanUtils.toBean(list, TmsShipmentStopRespVO.class));
    }

    @PostMapping("/dispatch")
    @Operation(summary = "调度运单：匹配车辆/司机")
    @PreAuthorize("@ss.hasPermission('tms:shipment:update')")
    public CommonResult<Boolean> dispatch(@Valid @RequestBody TmsShipmentDispatchReqVO dispatchReqVO) {
        shipmentService.dispatch(dispatchReqVO);
        return success(true);
    }

    @PostMapping("/confirm-arrival")
    @Operation(summary = "确认到达")
    @Parameter(name = "id", description = "运单编号", required = true)
    @PreAuthorize("@ss.hasPermission('tms:shipment:update')")
    public CommonResult<Boolean> confirmArrival(@RequestParam("id") Long id) {
        shipmentService.confirmArrival(id);
        return success(true);
    }

    @PostMapping("/confirm-sign")
    @Operation(summary = "确认签收")
    @Parameter(name = "id", description = "运单编号", required = true)
    @PreAuthorize("@ss.hasPermission('tms:shipment:update')")
    public CommonResult<Boolean> confirmSign(@RequestParam("id") Long id) {
        shipmentService.confirmSign(id);
        return success(true);
    }

}
