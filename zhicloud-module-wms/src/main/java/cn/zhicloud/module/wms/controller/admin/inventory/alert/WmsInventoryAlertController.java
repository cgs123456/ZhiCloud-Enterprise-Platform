package cn.zhicloud.module.wms.controller.admin.inventory.alert;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.collection.MapUtils;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.wms.controller.admin.inventory.alert.vo.WmsInventoryAlertPageReqVO;
import cn.zhicloud.module.wms.controller.admin.inventory.alert.vo.WmsInventoryAlertRespVO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryAlertDO;
import cn.zhicloud.module.wms.dal.dataobject.md.item.WmsItemSkuDO;
import cn.zhicloud.module.wms.dal.dataobject.md.warehouse.WmsWarehouseDO;
import cn.zhicloud.module.wms.service.inventory.alert.WmsInventoryAlertService;
import cn.zhicloud.module.wms.service.md.item.WmsItemSkuService;
import cn.zhicloud.module.wms.service.md.warehouse.WmsWarehouseService;
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

import static cn.zhicloud.framework.common.pojo.CommonResult.success;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertSet;

/**
 * WMS 库存预警 Controller
 *
 * @author 智云
 */
@Tag(name = "管理后台 - WMS 库存预警")
@RestController
@RequestMapping("/wms/inventory-alert")
@Validated
public class WmsInventoryAlertController {

    @Resource
    private WmsInventoryAlertService inventoryAlertService;
    @Resource
    private WmsWarehouseService warehouseService;
    @Resource
    private WmsItemSkuService itemSkuService;

    @GetMapping("/page")
    @Operation(summary = "获得库存预警分页")
    @PreAuthorize("@ss.hasPermission('wms:inventory-alert:query')")
    public CommonResult<PageResult<WmsInventoryAlertRespVO>> page(@Valid WmsInventoryAlertPageReqVO pageReqVO) {
        PageResult<WmsInventoryAlertDO> pageResult = inventoryAlertService.getInventoryAlertPage(pageReqVO);
        return success(new PageResult<>(buildRespVOList(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping("/get")
    @Operation(summary = "获得库存预警")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('wms:inventory-alert:query')")
    public CommonResult<WmsInventoryAlertRespVO> get(@RequestParam("id") Long id) {
        WmsInventoryAlertDO alert = inventoryAlertService.getInventoryAlert(id);
        return success(buildRespVO(alert));
    }

    @PutMapping("/confirm")
    @Operation(summary = "确认预警")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('wms:inventory-alert:update')")
    public CommonResult<Boolean> confirm(@RequestParam("id") Long id) {
        inventoryAlertService.confirmInventoryAlert(id);
        return success(true);
    }

    @PutMapping("/process")
    @Operation(summary = "处理预警")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('wms:inventory-alert:update')")
    public CommonResult<Boolean> process(@RequestParam("id") Long id) {
        inventoryAlertService.processInventoryAlert(id);
        return success(true);
    }

    private WmsInventoryAlertRespVO buildRespVO(WmsInventoryAlertDO alert) {
        if (alert == null) {
            return null;
        }
        List<WmsInventoryAlertRespVO> list = buildRespVOList(Collections.singletonList(alert));
        return list.isEmpty() ? null : list.get(0);
    }

    private List<WmsInventoryAlertRespVO> buildRespVOList(List<WmsInventoryAlertDO> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, WmsWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(
                convertSet(list, WmsInventoryAlertDO::getWarehouseId));
        Map<Long, WmsItemSkuDO> skuMap = itemSkuService.getItemSkuMap(
                convertSet(list, WmsInventoryAlertDO::getProductId));
        return BeanUtils.toBean(list, WmsInventoryAlertRespVO.class, vo -> {
            MapUtils.findAndThen(warehouseMap, vo.getWarehouseId(),
                    warehouse -> vo.setWarehouseName(warehouse.getName()));
            MapUtils.findAndThen(skuMap, vo.getProductId(), sku -> {
                vo.setSkuCode(sku.getCode());
                vo.setSkuName(sku.getName());
            });
        });
    }

}