package cn.zhicloud.module.tms.controller.admin.driver;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.tms.controller.admin.driver.vo.TmsDriverPageReqVO;
import cn.zhicloud.module.tms.controller.admin.driver.vo.TmsDriverRespVO;
import cn.zhicloud.module.tms.controller.admin.driver.vo.TmsDriverSaveReqVO;
import cn.zhicloud.module.tms.dal.dataobject.driver.TmsDriverDO;
import cn.zhicloud.module.tms.service.driver.TmsDriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - TMS 司机")
@RestController
@RequestMapping("/tms/driver")
@Validated
public class TmsDriverController {

    @Resource
    private TmsDriverService driverService;

    @PostMapping("/create")
    @Operation(summary = "创建司机")
    @PreAuthorize("@ss.hasPermission('tms:driver:create')")
    public CommonResult<Long> createDriver(@Valid @RequestBody TmsDriverSaveReqVO createReqVO) {
        return success(driverService.createDriver(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新司机")
    @PreAuthorize("@ss.hasPermission('tms:driver:update')")
    public CommonResult<Boolean> updateDriver(@Valid @RequestBody TmsDriverSaveReqVO updateReqVO) {
        driverService.updateDriver(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除司机")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('tms:driver:delete')")
    public CommonResult<Boolean> deleteDriver(@RequestParam("id") Long id) {
        driverService.deleteDriver(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得司机")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('tms:driver:query')")
    public CommonResult<TmsDriverRespVO> getDriver(@RequestParam("id") Long id) {
        TmsDriverDO driver = driverService.getDriver(id);
        return success(BeanUtils.toBean(driver, TmsDriverRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得司机分页")
    @PreAuthorize("@ss.hasPermission('tms:driver:query')")
    public CommonResult<PageResult<TmsDriverRespVO>> getDriverPage(@Valid TmsDriverPageReqVO pageReqVO) {
        PageResult<TmsDriverDO> pageResult = driverService.getDriverPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TmsDriverRespVO.class));
    }

}
