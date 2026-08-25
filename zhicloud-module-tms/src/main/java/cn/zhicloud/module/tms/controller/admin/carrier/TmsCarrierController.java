package cn.zhicloud.module.tms.controller.admin.carrier;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.tms.controller.admin.carrier.vo.TmsCarrierPageReqVO;
import cn.zhicloud.module.tms.controller.admin.carrier.vo.TmsCarrierRespVO;
import cn.zhicloud.module.tms.controller.admin.carrier.vo.TmsCarrierSaveReqVO;
import cn.zhicloud.module.tms.dal.dataobject.carrier.TmsCarrierDO;
import cn.zhicloud.module.tms.service.carrier.TmsCarrierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - TMS 承运商")
@RestController
@RequestMapping("/tms/carrier")
@Validated
public class TmsCarrierController {

    @Resource
    private TmsCarrierService carrierService;

    @PostMapping("/create")
    @Operation(summary = "创建承运商")
    @PreAuthorize("@ss.hasPermission('tms:carrier:create')")
    public CommonResult<Long> createCarrier(@Valid @RequestBody TmsCarrierSaveReqVO createReqVO) {
        return success(carrierService.createCarrier(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新承运商")
    @PreAuthorize("@ss.hasPermission('tms:carrier:update')")
    public CommonResult<Boolean> updateCarrier(@Valid @RequestBody TmsCarrierSaveReqVO updateReqVO) {
        carrierService.updateCarrier(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除承运商")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('tms:carrier:delete')")
    public CommonResult<Boolean> deleteCarrier(@RequestParam("id") Long id) {
        carrierService.deleteCarrier(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得承运商")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('tms:carrier:query')")
    public CommonResult<TmsCarrierRespVO> getCarrier(@RequestParam("id") Long id) {
        TmsCarrierDO carrier = carrierService.getCarrier(id);
        return success(BeanUtils.toBean(carrier, TmsCarrierRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得承运商分页")
    @PreAuthorize("@ss.hasPermission('tms:carrier:query')")
    public CommonResult<PageResult<TmsCarrierRespVO>> getCarrierPage(@Valid TmsCarrierPageReqVO pageReqVO) {
        PageResult<TmsCarrierDO> pageResult = carrierService.getCarrierPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TmsCarrierRespVO.class));
    }

}
