package cn.iocoder.yudao.module.wms.controller.admin.pickpath;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.wms.controller.admin.pickpath.vo.WmsPickPathRespVO;
import cn.iocoder.yudao.module.wms.service.order.pickpath.WmsPickPathService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * WMS 拣货路径优化 Controller
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - WMS 拣货路径优化")
@RestController
@RequestMapping("/wms/pick-path")
@Validated
public class WmsPickPathController {

    @Resource
    private WmsPickPathService pickPathService;

    @GetMapping("/optimize/{shipmentOrderId}")
    @Operation(summary = "优化单出库单拣货路径")
    @Parameter(name = "shipmentOrderId", description = "出库单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('wms:pick-path:query')")
    public CommonResult<WmsPickPathRespVO> optimizePickPath(@PathVariable("shipmentOrderId") Long shipmentOrderId) {
        return success(pickPathService.optimizePickPath(shipmentOrderId));
    }

    @GetMapping("/optimize-wave/{waveOrderId}")
    @Operation(summary = "优化波次拣货路径")
    @Parameter(name = "waveOrderId", description = "波次单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('wms:pick-path:query')")
    public CommonResult<WmsPickPathRespVO> optimizeBatchPickPath(@PathVariable("waveOrderId") Long waveOrderId) {
        return success(pickPathService.optimizeBatchPickPath(waveOrderId));
    }

}
