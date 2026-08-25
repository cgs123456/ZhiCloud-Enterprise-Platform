package cn.zhicloud.module.wms.controller.admin.order.wave;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.wms.controller.admin.order.wave.vo.detail.WmsWaveOrderDetailRespVO;
import cn.zhicloud.module.wms.controller.admin.order.wave.vo.order.WmsWaveOrderPageReqVO;
import cn.zhicloud.module.wms.controller.admin.order.wave.vo.order.WmsWaveOrderRespVO;
import cn.zhicloud.module.wms.controller.admin.order.wave.vo.order.WmsWaveOrderSaveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.order.wave.WmsWaveOrderDO;
import cn.zhicloud.module.wms.dal.dataobject.order.wave.WmsWaveOrderDetailDO;
import cn.zhicloud.module.wms.service.order.wave.WmsWaveOrderService;
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

/**
 * WMS 波次单 Controller
 *
 * @author 智云
 */
@Tag(name = "管理后台 - WMS 波次单")
@RestController
@RequestMapping("/wms/wave-order")
@Validated
public class WmsWaveOrderController {

    @Resource
    private WmsWaveOrderService waveOrderService;

    @PostMapping("/create")
    @Operation(summary = "生成波次单")
    @PreAuthorize("@ss.hasPermission('wms:wave-order:create')")
    public CommonResult<Long> createWaveOrder(@Valid @RequestBody WmsWaveOrderSaveReqVO createReqVO) {
        return success(waveOrderService.createWaveOrder(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新波次单")
    @PreAuthorize("@ss.hasPermission('wms:wave-order:update')")
    public CommonResult<Boolean> updateWaveOrder(@Valid @RequestBody WmsWaveOrderSaveReqVO updateReqVO) {
        waveOrderService.updateWaveOrder(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除波次单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('wms:wave-order:delete')")
    public CommonResult<Boolean> deleteWaveOrder(@RequestParam("id") Long id) {
        waveOrderService.deleteWaveOrder(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取波次单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('wms:wave-order:query')")
    public CommonResult<WmsWaveOrderRespVO> getWaveOrder(@RequestParam("id") Long id) {
        WmsWaveOrderDO waveOrder = waveOrderService.getWaveOrder(id);
        return success(BeanUtils.toBean(waveOrder, WmsWaveOrderRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得波次单分页")
    @PreAuthorize("@ss.hasPermission('wms:wave-order:query')")
    public CommonResult<PageResult<WmsWaveOrderRespVO>> getWaveOrderPage(@Valid WmsWaveOrderPageReqVO pageReqVO) {
        PageResult<WmsWaveOrderDO> pageResult = waveOrderService.getWaveOrderPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, WmsWaveOrderRespVO.class));
    }

    @GetMapping("/detail/list")
    @Operation(summary = "获取波次单明细列表")
    @Parameter(name = "waveOrderId", description = "波次单 ID", required = true)
    @PreAuthorize("@ss.hasPermission('wms:wave-order:query')")
    public CommonResult<List<WmsWaveOrderDetailRespVO>> getWaveOrderDetailList(
            @RequestParam("waveOrderId") Long waveOrderId) {
        List<WmsWaveOrderDetailDO> list = waveOrderService.getWaveOrderDetailListByWaveOrderId(waveOrderId);
        return success(BeanUtils.toBean(list, WmsWaveOrderDetailRespVO.class));
    }

}
