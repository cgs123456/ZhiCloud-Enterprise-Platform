package cn.iocoder.yudao.module.wms.controller.admin.order.asn;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.wms.controller.admin.order.asn.vo.WmsAsnOrderDetailRespVO;
import cn.iocoder.yudao.module.wms.controller.admin.order.asn.vo.WmsAsnOrderPageReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.order.asn.vo.WmsAsnOrderRespVO;
import cn.iocoder.yudao.module.wms.controller.admin.order.asn.vo.WmsAsnOrderSaveReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.item.WmsItemDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.item.WmsItemSkuDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.merchant.WmsMerchantDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.warehouse.WmsWarehouseDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.asn.WmsAsnOrderDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.asn.WmsAsnOrderDetailDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.dock.WmsDockDO;
import cn.iocoder.yudao.module.wms.service.md.item.WmsItemService;
import cn.iocoder.yudao.module.wms.service.md.item.WmsItemSkuService;
import cn.iocoder.yudao.module.wms.service.md.merchant.WmsMerchantService;
import cn.iocoder.yudao.module.wms.service.md.warehouse.WmsWarehouseService;
import cn.iocoder.yudao.module.wms.service.order.asn.WmsAsnOrderDetailService;
import cn.iocoder.yudao.module.wms.service.order.asn.WmsAsnOrderService;
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
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

/**
 * WMS ASN 到货通知单 Controller
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - WMS ASN 到货通知单")
@RestController
@RequestMapping("/wms/asn-order")
@Validated
public class WmsAsnOrderController {

    @Resource
    private WmsAsnOrderService asnOrderService;
    @Resource
    private WmsAsnOrderDetailService asnOrderDetailService;
    @Resource
    private WmsMerchantService merchantService;
    @Resource
    private WmsWarehouseService warehouseService;
    @Resource
    private WmsDockService dockService;
    @Resource
    private WmsItemSkuService itemSkuService;
    @Resource
    private WmsItemService itemService;

    @PostMapping("/create")
    @Operation(summary = "创建 ASN 到货通知单")
    @PreAuthorize("@ss.hasPermission('wms:asn-order:create')")
    public CommonResult<Long> createAsnOrder(@Valid @RequestBody WmsAsnOrderSaveReqVO createReqVO) {
        return success(asnOrderService.createAsnOrder(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 ASN 到货通知单")
    @PreAuthorize("@ss.hasPermission('wms:asn-order:update')")
    public CommonResult<Boolean> updateAsnOrder(@Valid @RequestBody WmsAsnOrderSaveReqVO updateReqVO) {
        asnOrderService.updateAsnOrder(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 ASN 到货通知单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('wms:asn-order:delete')")
    public CommonResult<Boolean> deleteAsnOrder(@RequestParam("id") Long id) {
        asnOrderService.deleteAsnOrder(id);
        return success(true);
    }

    @PutMapping("/close")
    @Operation(summary = "关闭 ASN 到货通知单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('wms:asn-order:update')")
    public CommonResult<Boolean> closeAsnOrder(@RequestParam("id") Long id) {
        asnOrderService.closeAsnOrder(id);
        return success(true);
    }

    @PutMapping("/confirm-arrival")
    @Operation(summary = "确认到货")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('wms:asn-order:update')")
    public CommonResult<Boolean> confirmArrival(@RequestParam("id") Long id) {
        asnOrderService.confirmArrival(id);
        return success(true);
    }

    @PostMapping("/convert-to-receipt")
    @Operation(summary = "转收货单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('wms:asn-order:update')")
    public CommonResult<Long> convertToReceipt(@RequestParam("id") Long id) {
        return success(asnOrderService.convertToReceipt(id));
    }

    @GetMapping("/get")
    @Operation(summary = "获得 ASN 到货通知单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('wms:asn-order:query')")
    public CommonResult<WmsAsnOrderRespVO> getAsnOrder(@RequestParam("id") Long id) {
        WmsAsnOrderDO order = asnOrderService.getAsnOrder(id);
        if (order == null) {
            return success(null);
        }
        List<WmsAsnOrderDetailDO> detailList = asnOrderDetailService.getAsnOrderDetailList(id);
        WmsAsnOrderRespVO respVO = buildAsnOrderRespVOList(Collections.singletonList(order)).get(0);
        respVO.setDetails(buildAsnOrderDetailRespVOList(detailList));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得 ASN 到货通知单分页")
    @PreAuthorize("@ss.hasPermission('wms:asn-order:query')")
    public CommonResult<PageResult<WmsAsnOrderRespVO>> getAsnOrderPage(@Valid WmsAsnOrderPageReqVO pageReqVO) {
        PageResult<WmsAsnOrderDO> pageResult = asnOrderService.getAsnOrderPage(pageReqVO);
        return success(new PageResult<>(buildAsnOrderRespVOList(pageResult.getList()), pageResult.getTotal()));
    }

    // ==================== 拼接 VO ====================

    private List<WmsAsnOrderRespVO> buildAsnOrderRespVOList(List<WmsAsnOrderDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        Map<Long, WmsMerchantDO> merchantMap = merchantService.getMerchantMap(
                convertSet(list, WmsAsnOrderDO::getSupplierId));
        Map<Long, WmsWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(
                convertSet(list, WmsAsnOrderDO::getWarehouseId));
        Map<Long, WmsDockDO> dockMap = dockService.getDockMap(
                convertSet(list, WmsAsnOrderDO::getDockId));
        return BeanUtils.toBean(list, WmsAsnOrderRespVO.class, vo -> {
            if (vo.getSupplierId() != null && merchantMap.containsKey(vo.getSupplierId())) {
                vo.setSupplierName(merchantMap.get(vo.getSupplierId()).getName());
            }
            if (vo.getWarehouseId() != null && warehouseMap.containsKey(vo.getWarehouseId())) {
                vo.setWarehouseName(warehouseMap.get(vo.getWarehouseId()).getName());
            }
            if (vo.getDockId() != null && dockMap.containsKey(vo.getDockId())) {
                vo.setDockName(dockMap.get(vo.getDockId()).getDockName());
            }
        });
    }

    private List<WmsAsnOrderDetailRespVO> buildAsnOrderDetailRespVOList(List<WmsAsnOrderDetailDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        Map<Long, WmsItemSkuDO> skuMap = itemSkuService.getItemSkuMap(
                convertSet(list, WmsAsnOrderDetailDO::getSkuId));
        Map<Long, WmsItemDO> itemMap = itemService.getItemMap(
                convertSet(skuMap.values(), WmsItemSkuDO::getItemId));
        return BeanUtils.toBean(list, WmsAsnOrderDetailRespVO.class, vo -> {
            if (vo.getSkuId() != null && skuMap.containsKey(vo.getSkuId())) {
                WmsItemSkuDO sku = skuMap.get(vo.getSkuId());
                vo.setSkuCode(sku.getCode()).setSkuName(sku.getName());
                if (sku.getItemId() != null && itemMap.containsKey(sku.getItemId())) {
                    vo.setProductName(itemMap.get(sku.getItemId()).getName());
                }
            }
        });
    }

}
