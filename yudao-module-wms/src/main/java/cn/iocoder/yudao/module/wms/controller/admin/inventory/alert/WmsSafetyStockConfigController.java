package cn.iocoder.yudao.module.wms.controller.admin.inventory.alert;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.wms.controller.admin.inventory.alert.vo.WmsSafetyStockConfigPageReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.inventory.alert.vo.WmsSafetyStockConfigRespVO;
import cn.iocoder.yudao.module.wms.controller.admin.inventory.alert.vo.WmsSafetyStockConfigSaveReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.inventory.WmsSafetyStockConfigDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.item.WmsItemSkuDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.warehouse.WmsWarehouseDO;
import cn.iocoder.yudao.module.wms.service.inventory.alert.WmsSafetyStockConfigService;
import cn.iocoder.yudao.module.wms.service.md.item.WmsItemSkuService;
import cn.iocoder.yudao.module.wms.service.md.warehouse.WmsWarehouseService;
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
 * WMS 安全库存配置 Controller
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - WMS 安全库存配置")
@RestController
@RequestMapping("/wms/safety-stock-config")
@Validated
public class WmsSafetyStockConfigController {

    @Resource
    private WmsSafetyStockConfigService safetyStockConfigService;
    @Resource
    private WmsWarehouseService warehouseService;
    @Resource
    private WmsItemSkuService itemSkuService;

    @PostMapping("/create")
    @Operation(summary = "创建安全库存配置")
    @PreAuthorize("@ss.hasPermission('wms:safety-stock-config:create')")
    public CommonResult<Long> create(@Valid @RequestBody WmsSafetyStockConfigSaveReqVO createReqVO) {
        return success(safetyStockConfigService.createSafetyStockConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新安全库存配置")
    @PreAuthorize("@ss.hasPermission('wms:safety-stock-config:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody WmsSafetyStockConfigSaveReqVO updateReqVO) {
        safetyStockConfigService.updateSafetyStockConfig(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除安全库存配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('wms:safety-stock-config:delete')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        safetyStockConfigService.deleteSafetyStockConfig(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得安全库存配置")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('wms:safety-stock-config:query')")
    public CommonResult<WmsSafetyStockConfigRespVO> get(@RequestParam("id") Long id) {
        WmsSafetyStockConfigDO config = safetyStockConfigService.getSafetyStockConfig(id);
        return success(buildRespVO(config));
    }

    @GetMapping("/page")
    @Operation(summary = "获得安全库存配置分页")
    @PreAuthorize("@ss.hasPermission('wms:safety-stock-config:query')")
    public CommonResult<PageResult<WmsSafetyStockConfigRespVO>> page(@Valid WmsSafetyStockConfigPageReqVO pageReqVO) {
        PageResult<WmsSafetyStockConfigDO> pageResult = safetyStockConfigService.getSafetyStockConfigPage(pageReqVO);
        return success(new PageResult<>(buildRespVOList(pageResult.getList()), pageResult.getTotal()));
    }

    private WmsSafetyStockConfigRespVO buildRespVO(WmsSafetyStockConfigDO config) {
        if (config == null) {
            return null;
        }
        List<WmsSafetyStockConfigRespVO> list = buildRespVOList(Collections.singletonList(config));
        return list.isEmpty() ? null : list.get(0);
    }

    private List<WmsSafetyStockConfigRespVO> buildRespVOList(List<WmsSafetyStockConfigDO> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, WmsWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(
                convertSet(list, WmsSafetyStockConfigDO::getWarehouseId));
        Map<Long, WmsItemSkuDO> skuMap = itemSkuService.getItemSkuMap(
                convertSet(list, WmsSafetyStockConfigDO::getProductId));
        return BeanUtils.toBean(list, WmsSafetyStockConfigRespVO.class, vo -> {
            MapUtils.findAndThen(warehouseMap, vo.getWarehouseId(),
                    warehouse -> vo.setWarehouseName(warehouse.getName()));
            MapUtils.findAndThen(skuMap, vo.getProductId(), sku -> {
                vo.setSkuCode(sku.getCode());
                vo.setSkuName(sku.getName());
            });
        });
    }

}