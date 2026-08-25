package cn.zhicloud.module.wms.controller.admin.inventory.batch;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.wms.controller.admin.inventory.batch.vo.WmsBatchExpiryAlertPageReqVO;
import cn.zhicloud.module.wms.controller.admin.inventory.batch.vo.WmsBatchExpiryAlertRespVO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryAlertDO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryBatchDO;
import cn.zhicloud.module.wms.dal.dataobject.md.item.WmsItemSkuDO;
import cn.zhicloud.module.wms.dal.dataobject.md.warehouse.WmsWarehouseDO;
import cn.zhicloud.module.wms.dal.mysql.inventory.WmsInventoryBatchMapper;
import cn.zhicloud.module.wms.service.inventory.batch.WmsBatchExpiryAlertService;
import cn.zhicloud.module.wms.service.md.item.WmsItemSkuService;
import cn.zhicloud.module.wms.service.md.warehouse.WmsWarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertSet;

/**
 * WMS 批次效期预警 Controller
 *
 * @author 智云
 */
@Tag(name = "管理后台 - WMS 批次效期预警")
@RestController
@RequestMapping("/wms/batch-expiry-alert")
@Validated
public class WmsBatchExpiryAlertController {

    @Resource
    private WmsBatchExpiryAlertService batchExpiryAlertService;
    @Resource
    private WmsWarehouseService warehouseService;
    @Resource
    private WmsItemSkuService itemSkuService;
    @Resource
    private WmsInventoryBatchMapper inventoryBatchMapper;

    @GetMapping("/page")
    @Operation(summary = "获得批次效期预警分页")
    @PreAuthorize("@ss.hasPermission('wms:batch-expiry-alert:query')")
    public CommonResult<PageResult<WmsBatchExpiryAlertRespVO>> getExpiryAlertPage(
            @Valid WmsBatchExpiryAlertPageReqVO pageReqVO) {
        PageResult<WmsInventoryAlertDO> pageResult = batchExpiryAlertService.getExpiryAlertPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(new PageResult<>(new ArrayList<>(), pageResult.getTotal()));
        }
        List<WmsInventoryAlertDO> alerts = pageResult.getList();
        // 批量查询仓库名称
        Map<Long, WmsWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(
                convertSet(alerts, WmsInventoryAlertDO::getWarehouseId));
        // 批量查询 SKU 信息
        Map<Long, WmsItemSkuDO> skuMap = itemSkuService.getItemSkuMap(
                convertSet(alerts, WmsInventoryAlertDO::getProductId));
        // 批量查询批次明细
        List<WmsInventoryBatchDO> batches = inventoryBatchMapper.selectListByBatchNos(
                convertSet(alerts, WmsInventoryAlertDO::getBatchNo));
        Map<String, WmsInventoryBatchDO> batchMap = batches.stream()
                .collect(Collectors.toMap(WmsInventoryBatchDO::getBatchNo, b -> b, (a, b) -> a));
        // 组装返回
        List<WmsBatchExpiryAlertRespVO> respVOs = new ArrayList<>();
        for (WmsInventoryAlertDO alert : alerts) {
            WmsBatchExpiryAlertRespVO respVO = BeanUtils.toBean(alert, WmsBatchExpiryAlertRespVO.class);
            // 仓库名称
            if (alert.getWarehouseId() != null && warehouseMap.containsKey(alert.getWarehouseId())) {
                respVO.setWarehouseName(warehouseMap.get(alert.getWarehouseId()).getName());
            }
            // SKU 信息
            if (alert.getProductId() != null && skuMap.containsKey(alert.getProductId())) {
                WmsItemSkuDO sku = skuMap.get(alert.getProductId());
                respVO.setSkuCode(sku.getCode());
                respVO.setSkuName(sku.getName());
            }
            // 批次明细
            if (alert.getBatchNo() != null && batchMap.containsKey(alert.getBatchNo())) {
                WmsInventoryBatchDO batch = batchMap.get(alert.getBatchNo());
                respVO.setProductionDate(batch.getProductionDate());
                respVO.setExpiryDate(batch.getExpiryDate());
                respVO.setShelfLifeDays(batch.getShelfLifeDays());
                respVO.setSupplierBatchNo(batch.getSupplierBatchNo());
            }
            respVOs.add(respVO);
        }
        return success(new PageResult<>(respVOs, pageResult.getTotal()));
    }

    @PostMapping("/scan")
    @Operation(summary = "手动触发批次效期扫描")
    @Parameter(name = "description", description = "手动触发批次效期扫描，更新批次状态并生成预警")
    @PreAuthorize("@ss.hasPermission('wms:batch-expiry-alert:scan')")
    public CommonResult<Integer> scanExpiryAlerts() {
        int count = batchExpiryAlertService.scanExpiryAlerts();
        return success(count);
    }

}
