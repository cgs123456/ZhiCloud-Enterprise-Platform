package cn.iocoder.yudao.module.wms.controller.admin.order.crossdock;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.wms.controller.admin.order.crossdock.vo.WmsCrossDockOrderDetailRespVO;
import cn.iocoder.yudao.module.wms.controller.admin.order.crossdock.vo.WmsCrossDockOrderPageReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.order.crossdock.vo.WmsCrossDockOrderRespVO;
import cn.iocoder.yudao.module.wms.controller.admin.order.crossdock.vo.WmsCrossDockOrderSaveReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.merchant.WmsMerchantDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.crossdock.WmsCrossDockOrderDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.crossdock.WmsCrossDockOrderDetailDO;
import cn.iocoder.yudao.module.wms.service.md.merchant.WmsMerchantService;
import cn.iocoder.yudao.module.wms.service.order.crossdock.WmsCrossDockOrderDetailService;
import cn.iocoder.yudao.module.wms.service.order.crossdock.WmsCrossDockOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSetByFlatMap;

@Tag(name = "管理后台 - WMS 越库单")
@RestController
@RequestMapping("/wms/cross-dock-order")
@Validated
public class WmsCrossDockOrderController {

    @Resource
    private WmsCrossDockOrderService crossDockOrderService;
    @Resource
    private WmsCrossDockOrderDetailService crossDockOrderDetailService;
    @Resource
    private WmsMerchantService merchantService;
    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建越库单")
    @PreAuthorize("@ss.hasPermission('wms:cross-dock-order:create')")
    public CommonResult<Long> createCrossDockOrder(@Valid @RequestBody WmsCrossDockOrderSaveReqVO createReqVO) {
        return success(crossDockOrderService.createCrossDockOrder(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新越库单")
    @PreAuthorize("@ss.hasPermission('wms:cross-dock-order:update')")
    public CommonResult<Boolean> updateCrossDockOrder(@Valid @RequestBody WmsCrossDockOrderSaveReqVO updateReqVO) {
        crossDockOrderService.updateCrossDockOrder(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除越库单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('wms:cross-dock-order:delete')")
    public CommonResult<Boolean> deleteCrossDockOrder(@RequestParam("id") Long id) {
        crossDockOrderService.deleteCrossDockOrder(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得越库单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('wms:cross-dock-order:query')")
    public CommonResult<WmsCrossDockOrderRespVO> getCrossDockOrder(@RequestParam("id") Long id) {
        WmsCrossDockOrderDO order = crossDockOrderService.getCrossDockOrder(id);
        if (order == null) {
            return success(null);
        }
        // 获得越库单的明细列表
        List<WmsCrossDockOrderDetailDO> detailList = crossDockOrderDetailService.getCrossDockOrderDetailList(id);
        // 拼接结果返回
        WmsCrossDockOrderRespVO respVO = buildCrossDockOrderRespVO(order)
                .setDetails(buildCrossDockOrderDetailRespVOList(detailList));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得越库单分页")
    @PreAuthorize("@ss.hasPermission('wms:cross-dock-order:query')")
    public CommonResult<PageResult<WmsCrossDockOrderRespVO>> getCrossDockOrderPage(@Valid WmsCrossDockOrderPageReqVO pageReqVO) {
        PageResult<WmsCrossDockOrderDO> pageResult = crossDockOrderService.getCrossDockOrderPage(pageReqVO);
        return success(new PageResult<>(buildCrossDockOrderRespVOList(pageResult.getList()), pageResult.getTotal()));
    }

    @PutMapping("/confirm-receipt")
    @Operation(summary = "确认收货（跳过上架，直接分配至出库月台）")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('wms:cross-dock-order:confirm-receipt')")
    public CommonResult<Boolean> confirmReceipt(@RequestParam("id") Long id) {
        crossDockOrderService.confirmReceipt(id);
        return success(true);
    }

    @PutMapping("/complete")
    @Operation(summary = "完成越库单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('wms:cross-dock-order:complete')")
    public CommonResult<Boolean> completeCrossDockOrder(@RequestParam("id") Long id) {
        crossDockOrderService.completeCrossDockOrder(id);
        return success(true);
    }

    // ==================== 拼接 VO ====================

    private WmsCrossDockOrderRespVO buildCrossDockOrderRespVO(WmsCrossDockOrderDO order) {
        if (order == null) {
            return null;
        }
        List<WmsCrossDockOrderRespVO> list = buildCrossDockOrderRespVOList(Collections.singletonList(order));
        return CollUtil.getFirst(list);
    }

    private List<WmsCrossDockOrderRespVO> buildCrossDockOrderRespVOList(List<WmsCrossDockOrderDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 获取相关的商户、用户等数据
        Map<Long, WmsMerchantDO> merchantMap = merchantService.getMerchantMap(convertSetByFlatMap(list,
                order -> Stream.of(order.getSourceSupplierId(), order.getTargetCustomerId())));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertSetByFlatMap(list,
                order -> Stream.of(parseUserId(order.getCreator()), parseUserId(order.getUpdater()))));
        // 拼接数据
        return BeanUtils.toBean(list, WmsCrossDockOrderRespVO.class, vo -> {
            MapUtils.findAndThen(merchantMap, vo.getSourceSupplierId(),
                    merchant -> vo.setSourceSupplierName(merchant.getName()));
            MapUtils.findAndThen(merchantMap, vo.getTargetCustomerId(),
                    merchant -> vo.setTargetCustomerName(merchant.getName()));
            MapUtils.findAndThen(userMap, parseUserId(vo.getCreator()), user -> vo.setCreatorName(user.getNickname()));
            MapUtils.findAndThen(userMap, parseUserId(vo.getUpdater()), user -> vo.setUpdaterName(user.getNickname()));
        });
    }

    private Long parseUserId(String userId) {
        return NumberUtil.parseLong(userId, null);
    }

    private List<WmsCrossDockOrderDetailRespVO> buildCrossDockOrderDetailRespVOList(List<WmsCrossDockOrderDetailDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 拼接数据
        return BeanUtils.toBean(list, WmsCrossDockOrderDetailRespVO.class);
    }

}
