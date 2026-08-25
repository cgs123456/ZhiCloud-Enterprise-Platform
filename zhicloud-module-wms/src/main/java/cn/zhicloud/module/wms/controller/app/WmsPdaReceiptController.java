package cn.zhicloud.module.wms.controller.app;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.wms.controller.app.vo.WmsPdaAsnRespVO;
import cn.zhicloud.module.wms.controller.app.vo.WmsPdaConfirmReceiptReqVO;
import cn.zhicloud.module.wms.controller.app.vo.WmsPdaPendingReceiptRespVO;
import cn.zhicloud.module.wms.controller.app.vo.WmsPdaScanAsnReqVO;
import cn.zhicloud.module.wms.dal.dataobject.md.item.WmsItemDO;
import cn.zhicloud.module.wms.dal.dataobject.md.item.WmsItemSkuDO;
import cn.zhicloud.module.wms.dal.dataobject.md.warehouse.WmsWarehouseDO;
import cn.zhicloud.module.wms.dal.dataobject.order.asn.WmsAsnOrderDO;
import cn.zhicloud.module.wms.dal.dataobject.order.asn.WmsAsnOrderDetailDO;
import cn.zhicloud.module.wms.dal.dataobject.order.dock.WmsDockDO;
import cn.zhicloud.module.wms.dal.mysql.order.asn.WmsAsnOrderMapper;
import cn.zhicloud.module.wms.service.md.item.WmsItemService;
import cn.zhicloud.module.wms.service.md.item.WmsItemSkuService;
import cn.zhicloud.module.wms.service.md.warehouse.WmsWarehouseService;
import cn.zhicloud.module.wms.service.order.asn.WmsAsnOrderDetailService;
import cn.zhicloud.module.wms.service.order.dock.WmsDockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.framework.common.pojo.CommonResult.success;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.ASN_ORDER_NOT_EXISTS;

/**
 * WMS PDA 收货作业 Controller
 *
 * @author 智云
 */
@Tag(name = "用户 App - WMS PDA 收货作业")
@RestController
@RequestMapping("/wms-api/receipt")
@Validated
public class WmsPdaReceiptController {

    @Resource
    private WmsAsnOrderMapper asnOrderMapper;
    @Resource
    private WmsAsnOrderDetailService asnOrderDetailService;
    @Resource
    private WmsWarehouseService warehouseService;
    @Resource
    private WmsDockService dockService;
    @Resource
    private WmsItemSkuService itemSkuService;
    @Resource
    private WmsItemService itemService;

    @PostMapping("/scan-asn")
    @Operation(summary = "扫描 ASN 条码，返回 ASN 详情")
    @PreAuthorize("@ss.hasPermission('wms:pda:scan')")
    public CommonResult<WmsPdaAsnRespVO> scanAsn(@Valid @RequestBody WmsPdaScanAsnReqVO reqVO) {
        // 1. 查询 ASN 单
        WmsAsnOrderDO order = asnOrderMapper.selectByNo(reqVO.getScanCode());
        if (order == null) {
            throw exception(ASN_ORDER_NOT_EXISTS);
        }
        // 2. 查询明细
        List<WmsAsnOrderDetailDO> details = asnOrderDetailService.getAsnOrderDetailList(order.getId());
        // 3. 拼接返回
        WmsPdaAsnRespVO respVO = BeanUtils.toBean(order, WmsPdaAsnRespVO.class);
        // 仓库名称
        if (order.getWarehouseId() != null) {
            Map<Long, WmsWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(
                    Collections.singleton(order.getWarehouseId()));
            if (warehouseMap.containsKey(order.getWarehouseId())) {
                respVO.setWarehouseName(warehouseMap.get(order.getWarehouseId()).getName());
            }
        }
        // 月台名称
        if (order.getDockId() != null) {
            WmsDockDO dock = dockService.getDock(order.getDockId());
            if (dock != null) {
                respVO.setDockName(dock.getDockName());
            }
        }
        // 明细 SKU 名称
        if (CollUtil.isNotEmpty(details)) {
            Map<Long, WmsItemSkuDO> skuMap = itemSkuService.getItemSkuMap(
                    convertSet(details, WmsAsnOrderDetailDO::getSkuId));
            Map<Long, WmsItemDO> itemMap = itemService.getItemMap(
                    convertSet(skuMap.values(), WmsItemSkuDO::getItemId));
            List<WmsPdaAsnRespVO.AsnDetail> detailVOs = BeanUtils.toBean(details, WmsPdaAsnRespVO.AsnDetail.class);
            for (WmsPdaAsnRespVO.AsnDetail detailVO : detailVOs) {
                WmsItemSkuDO sku = skuMap.get(detailVO.getSkuId());
                if (sku != null) {
                    detailVO.setSkuCode(sku.getCode()).setSkuName(sku.getName());
                    WmsItemDO item = itemMap.get(sku.getItemId());
                    if (item != null) {
                        detailVO.setProductName(item.getName());
                    }
                }
            }
            respVO.setDetails(detailVOs);
        }
        return success(respVO);
    }

    @PostMapping("/confirm-receipt")
    @Operation(summary = "确认收货")
    @PreAuthorize("@ss.hasPermission('wms:pda:receipt')")
    public CommonResult<Boolean> confirmReceipt(@Valid @RequestBody WmsPdaConfirmReceiptReqVO reqVO) {
        asnOrderDetailService.addReceivedQuantity(reqVO.getDetailId(), reqVO.getReceivedQuantity());
        return success(true);
    }

    @GetMapping("/pending-receipt-list")
    @Operation(summary = "待收货列表")
    public CommonResult<List<WmsPdaPendingReceiptRespVO>> getPendingReceiptList() {
        // 查询待到货(10)与已到货(20)的 ASN 单
        List<WmsAsnOrderDO> list = asnOrderMapper.selectList(
                new cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX<WmsAsnOrderDO>()
                        .in(WmsAsnOrderDO::getStatus, 10, 20)
                        .orderByDesc(WmsAsnOrderDO::getId));
        if (CollUtil.isEmpty(list)) {
            return success(Collections.emptyList());
        }
        Map<Long, WmsWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(
                convertSet(list, WmsAsnOrderDO::getWarehouseId));
        Map<Long, WmsDockDO> dockMap = dockService.getDockMap(convertSet(list, WmsAsnOrderDO::getDockId));
        List<WmsPdaPendingReceiptRespVO> respVOs = new java.util.ArrayList<>();
        for (WmsAsnOrderDO order : list) {
            WmsPdaPendingReceiptRespVO vo = BeanUtils.toBean(order, WmsPdaPendingReceiptRespVO.class);
            if (order.getWarehouseId() != null && warehouseMap.containsKey(order.getWarehouseId())) {
                vo.setWarehouseName(warehouseMap.get(order.getWarehouseId()).getName());
            }
            if (order.getDockId() != null && dockMap.containsKey(order.getDockId())) {
                vo.setDockName(dockMap.get(order.getDockId()).getDockName());
            }
            respVOs.add(vo);
        }
        return success(respVOs);
    }

}
