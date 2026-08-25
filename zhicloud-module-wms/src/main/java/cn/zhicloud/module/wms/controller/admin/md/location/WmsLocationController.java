package cn.zhicloud.module.wms.controller.admin.md.location;

import cn.zhicloud.framework.apilog.core.annotation.ApiAccessLog;
import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageParam;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.excel.core.util.ExcelUtils;
import cn.zhicloud.module.wms.controller.admin.md.location.vo.WmsLocationPageReqVO;
import cn.zhicloud.module.wms.controller.admin.md.location.vo.WmsLocationRespVO;
import cn.zhicloud.module.wms.controller.admin.md.location.vo.WmsLocationSaveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.md.location.WmsLocationDO;
import cn.zhicloud.module.wms.service.md.location.WmsLocationService;
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

@Tag(name = "管理后台 - WMS 库位")
@RestController
@RequestMapping("/wms/location")
@Validated
public class WmsLocationController {

    @Resource
    private WmsLocationService locationService;

    @PostMapping("/create")
    @Operation(summary = "创建库位")
    @PreAuthorize("@ss.hasPermission('wms:location:create')")
    public CommonResult<Long> createLocation(@Valid @RequestBody WmsLocationSaveReqVO createReqVO) {
        return success(locationService.createLocation(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新库位")
    @PreAuthorize("@ss.hasPermission('wms:location:update')")
    public CommonResult<Boolean> updateLocation(@Valid @RequestBody WmsLocationSaveReqVO updateReqVO) {
        locationService.updateLocation(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除库位")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('wms:location:delete')")
    public CommonResult<Boolean> deleteLocation(@RequestParam("id") Long id) {
        locationService.deleteLocation(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得库位")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('wms:location:query')")
    public CommonResult<WmsLocationRespVO> getLocation(@RequestParam("id") Long id) {
        WmsLocationDO location = locationService.getLocation(id);
        return success(BeanUtils.toBean(location, WmsLocationRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得库位分页")
    @PreAuthorize("@ss.hasPermission('wms:location:query')")
    public CommonResult<PageResult<WmsLocationRespVO>> getLocationPage(@Valid WmsLocationPageReqVO pageReqVO) {
        PageResult<WmsLocationDO> pageResult = locationService.getLocationPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, WmsLocationRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得库位列表", description = "主要用于前端下拉")
    @Parameter(name = "zoneId", description = "库区 ID", example = "1024")
    @PreAuthorize("@ss.hasPermission('wms:location:query')")
    public CommonResult<List<WmsLocationRespVO>> getLocationList(
            @RequestParam(value = "zoneId", required = false) Long zoneId) {
        List<WmsLocationDO> list = zoneId != null
                ? locationService.getLocationListByZoneId(zoneId)
                : locationService.getLocationList();
        return success(BeanUtils.toBean(list, WmsLocationRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出库位 Excel")
    @PreAuthorize("@ss.hasPermission('wms:location:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportLocationExcel(@Valid WmsLocationPageReqVO pageReqVO,
                                    HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<WmsLocationDO> list = locationService.getLocationPage(pageReqVO).getList();
        ExcelUtils.write(response, "库位.xls", "数据", WmsLocationRespVO.class,
                BeanUtils.toBean(list, WmsLocationRespVO.class));
    }

}
