package cn.iocoder.yudao.module.wms.controller.admin.order.dock;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.wms.controller.admin.order.dock.vo.WmsDockPageReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.order.dock.vo.WmsDockRespVO;
import cn.iocoder.yudao.module.wms.controller.admin.order.dock.vo.WmsDockSaveReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.warehouse.WmsWarehouseDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.dock.WmsDockDO;
import cn.iocoder.yudao.module.wms.service.md.warehouse.WmsWarehouseService;
import cn.iocoder.yudao.module.wms.service.order.dock.WmsDockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

/**
 * WMS 月台 Controller
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - WMS 月台")
@RestController
@RequestMapping("/wms/dock")
@Validated
public class WmsDockController {

    @Resource
    private WmsDockService dockService;
    @Resource
    private WmsWarehouseService warehouseService;

    @PostMapping("/create")
    @Operation(summary = "创建月台")
    @PreAuthorize("@ss.hasPermission('wms:dock:create')")
    public CommonResult<Long> createDock(@Valid @RequestBody WmsDockSaveReqVO createReqVO) {
        return success(dockService.createDock(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新月台")
    @PreAuthorize("@ss.hasPermission('wms:dock:update')")
    public CommonResult<Boolean> updateDock(@Valid @RequestBody WmsDockSaveReqVO updateReqVO) {
        dockService.updateDock(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除月台")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('wms:dock:delete')")
    public CommonResult<Boolean> deleteDock(@RequestParam("id") Long id) {
        dockService.deleteDock(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得月台")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('wms:dock:query')")
    public CommonResult<WmsDockRespVO> getDock(@RequestParam("id") Long id) {
        WmsDockDO dock = dockService.getDock(id);
        return success(buildDockRespVO(dock));
    }

    @GetMapping("/page")
    @Operation(summary = "获得月台分页")
    @PreAuthorize("@ss.hasPermission('wms:dock:query')")
    public CommonResult<PageResult<WmsDockRespVO>> getDockPage(@Valid WmsDockPageReqVO pageReqVO) {
        PageResult<WmsDockDO> pageResult = dockService.getDockPage(pageReqVO);
        if (pageResult.getList().isEmpty()) {
            return success(new PageResult<>(Collections.emptyList(), pageResult.getTotal()));
        }
        Map<Long, WmsWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(
                convertSet(pageResult.getList(), WmsDockDO::getWarehouseId));
        return success(new PageResult<>(BeanUtils.toBean(pageResult.getList(), WmsDockRespVO.class, vo -> {
            if (vo.getWarehouseId() != null && warehouseMap.containsKey(vo.getWarehouseId())) {
                vo.setWarehouseName(warehouseMap.get(vo.getWarehouseId()).getName());
            }
        }), pageResult.getTotal()));
    }

    private WmsDockRespVO buildDockRespVO(WmsDockDO dock) {
        if (dock == null) {
            return null;
        }
        WmsDockRespVO respVO = BeanUtils.toBean(dock, WmsDockRespVO.class);
        if (dock.getWarehouseId() != null) {
            Map<Long, WmsWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(
                    Collections.singleton(dock.getWarehouseId()));
            if (warehouseMap.containsKey(dock.getWarehouseId())) {
                respVO.setWarehouseName(warehouseMap.get(dock.getWarehouseId()).getName());
            }
        }
        return respVO;
    }

}
