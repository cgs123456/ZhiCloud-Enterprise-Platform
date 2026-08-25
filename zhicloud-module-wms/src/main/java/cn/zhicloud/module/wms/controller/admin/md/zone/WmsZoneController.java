package cn.zhicloud.module.wms.controller.admin.md.zone;

import cn.zhicloud.framework.apilog.core.annotation.ApiAccessLog;
import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageParam;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.excel.core.util.ExcelUtils;
import cn.zhicloud.module.wms.controller.admin.md.zone.vo.WmsZonePageReqVO;
import cn.zhicloud.module.wms.controller.admin.md.zone.vo.WmsZoneRespVO;
import cn.zhicloud.module.wms.controller.admin.md.zone.vo.WmsZoneSaveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.md.zone.WmsZoneDO;
import cn.zhicloud.module.wms.service.md.zone.WmsZoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

import static cn.zhicloud.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - WMS 库区")
@RestController
@RequestMapping("/wms/zone")
@Validated
public class WmsZoneController {

    @Resource
    private WmsZoneService zoneService;

    @PostMapping("/create")
    @Operation(summary = "创建库区")
    @PreAuthorize("@ss.hasPermission('wms:zone:create')")
    public CommonResult<Long> createZone(@Valid @RequestBody WmsZoneSaveReqVO createReqVO) {
        return success(zoneService.createZone(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新库区")
    @PreAuthorize("@ss.hasPermission('wms:zone:update')")
    public CommonResult<Boolean> updateZone(@Valid @RequestBody WmsZoneSaveReqVO updateReqVO) {
        zoneService.updateZone(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除库区")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('wms:zone:delete')")
    public CommonResult<Boolean> deleteZone(@RequestParam("id") Long id) {
        zoneService.deleteZone(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得库区")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('wms:zone:query')")
    public CommonResult<WmsZoneRespVO> getZone(@RequestParam("id") Long id) {
        WmsZoneDO zone = zoneService.getZone(id);
        return success(BeanUtils.toBean(zone, WmsZoneRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得库区分页")
    @PreAuthorize("@ss.hasPermission('wms:zone:query')")
    public CommonResult<PageResult<WmsZoneRespVO>> getZonePage(@Valid WmsZonePageReqVO pageReqVO) {
        PageResult<WmsZoneDO> pageResult = zoneService.getZonePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, WmsZoneRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得库区列表", description = "主要用于前端下拉")
    @Parameter(name = "warehouseId", description = "仓库 ID", example = "1024")
    @PreAuthorize("@ss.hasPermission('wms:zone:query')")
    public CommonResult<List<WmsZoneRespVO>> getZoneList(
            @RequestParam(value = "warehouseId", required = false) Long warehouseId) {
        List<WmsZoneDO> list = warehouseId != null
                ? zoneService.getZoneListByWarehouseId(warehouseId)
                : zoneService.getZoneList();
        return success(BeanUtils.toBean(list, WmsZoneRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出库区 Excel")
    @PreAuthorize("@ss.hasPermission('wms:zone:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportZoneExcel(@Valid WmsZonePageReqVO pageReqVO,
                                HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<WmsZoneDO> list = zoneService.getZonePage(pageReqVO).getList();
        ExcelUtils.write(response, "库区.xls", "数据", WmsZoneRespVO.class,
                BeanUtils.toBean(list, WmsZoneRespVO.class));
    }

}
