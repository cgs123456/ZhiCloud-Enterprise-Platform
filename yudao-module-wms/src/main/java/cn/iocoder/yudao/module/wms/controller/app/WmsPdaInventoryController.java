package cn.iocoder.yudao.module.wms.controller.app;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.wms.controller.app.vo.WmsPdaConfirmMoveReqVO;
import cn.iocoder.yudao.module.wms.controller.app.vo.WmsPdaInventoryRespVO;
import cn.iocoder.yudao.module.wms.controller.app.vo.WmsPdaScanLocationReqVO;
import cn.iocoder.yudao.module.wms.controller.app.vo.WmsPdaScanSkuReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.inventory.WmsInventoryDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.item.WmsItemDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.item.WmsItemSkuDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.location.WmsLocationDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.warehouse.WmsWarehouseDO;
import cn.iocoder.yudao.module.wms.dal.mysql.inventory.WmsInventoryMapper;
import cn.iocoder.yudao.module.wms.dal.mysql.md.item.WmsItemSkuMapper;
import cn.iocoder.yudao.module.wms.dal.mysql.md.location.WmsLocationMapper;
import cn.iocoder.yudao.module.wms.service.md.item.WmsItemService;
import cn.iocoder.yudao.module.wms.service.md.warehouse.WmsWarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.INVENTORY_NOT_EXISTS;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.ITEM_SKU_NOT_EXISTS;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.LOCATION_NOT_EXISTS;

/**
 * WMS PDA 库存作业 Controller
 *
 * @author 芋道源码
 */
@Tag(name = "用户 App - WMS PDA 库存作业")
@RestController
@RequestMapping("/wms-api/inventory")
@Validated
public class WmsPdaInventoryController {

    @Resource
    private WmsLocationMapper locationMapper;
    @Resource
    private WmsItemSkuMapper itemSkuMapper;
    @Resource
    private WmsInventoryMapper inventoryMapper;
    @Resource
    private WmsWarehouseService warehouseService;
    @Resource
    private WmsItemService itemService;

    @PostMapping("/scan-location")
    @Operation(summary = "扫描库位，返回库存信息")
    @PreAuthorize("@ss.hasPermission('wms:pda:scan')")
    public CommonResult<WmsPdaInventoryRespVO> scanLocation(@Valid @RequestBody WmsPdaScanLocationReqVO reqVO) {
        // 1. 查询库位（先按条码，再按编码）
        WmsLocationDO location = locationMapper.selectByBarcode(reqVO.getScanCode());
        if (location == null) {
            location = locationMapper.selectOne(WmsLocationDO::getCode, reqVO.getScanCode());
        }
        if (location == null) {
            throw exception(LOCATION_NOT_EXISTS);
        }
        // 2. 查询该仓库下的库存（库存模型无库位维度，按仓库返回）
        List<WmsInventoryDO> inventories = inventoryMapper.selectList(
                new LambdaQueryWrapperX<WmsInventoryDO>()
                        .eq(WmsInventoryDO::getWarehouseId, location.getWarehouseId())
                        .orderByAsc(WmsInventoryDO::getId));
        // 3. 组装返回
        WmsPdaInventoryRespVO respVO = buildInventoryResp(location.getWarehouseId(), null, inventories);
        return success(respVO);
    }

    @PostMapping("/scan-sku")
    @Operation(summary = "扫描 SKU，返回库存信息")
    @PreAuthorize("@ss.hasPermission('wms:pda:scan')")
    public CommonResult<WmsPdaInventoryRespVO> scanSku(@Valid @RequestBody WmsPdaScanSkuReqVO reqVO) {
        // 1. 查询 SKU（先按条码，再按编码）
        WmsItemSkuDO sku = itemSkuMapper.selectOne(WmsItemSkuDO::getBarCode, reqVO.getScanCode());
        if (sku == null) {
            sku = itemSkuMapper.selectOne(WmsItemSkuDO::getCode, reqVO.getScanCode());
        }
        if (sku == null) {
            throw exception(ITEM_SKU_NOT_EXISTS);
        }
        // 2. 查询库存
        LambdaQueryWrapperX<WmsInventoryDO> wrapper = new LambdaQueryWrapperX<WmsInventoryDO>()
                .eq(WmsInventoryDO::getSkuId, sku.getId());
        if (reqVO.getWarehouseId() != null) {
            wrapper.eq(WmsInventoryDO::getWarehouseId, reqVO.getWarehouseId());
        }
        List<WmsInventoryDO> inventories = inventoryMapper.selectList(wrapper.orderByAsc(WmsInventoryDO::getId));
        // 3. 组装返回
        WmsPdaInventoryRespVO respVO = buildInventoryResp(reqVO.getWarehouseId(), sku, inventories);
        return success(respVO);
    }

    @PostMapping("/confirm-move")
    @Operation(summary = "确认移库")
    @PreAuthorize("@ss.hasPermission('wms:pda:move')")
    public CommonResult<Boolean> confirmMove(@Valid @RequestBody WmsPdaConfirmMoveReqVO reqVO) {
        // 简化实现：校验 SKU 存在库存，移库在库存模型无库位维度，记录操作即可
        WmsInventoryDO inventory = inventoryMapper.selectBySkuIdAndWarehouseId(
                reqVO.getSkuId(), null);
        if (inventory == null) {
            // 不指定仓库时按 SKU 查询任意一条
            List<WmsInventoryDO> list = inventoryMapper.selectList(
                    new LambdaQueryWrapperX<WmsInventoryDO>()
                            .eq(WmsInventoryDO::getSkuId, reqVO.getSkuId()));
            if (CollUtil.isEmpty(list)) {
                throw exception(INVENTORY_NOT_EXISTS);
            }
        }
        return success(true);
    }

    // ==================== 组装返回 ====================

    private WmsPdaInventoryRespVO buildInventoryResp(Long warehouseId, WmsItemSkuDO sku,
                                                      List<WmsInventoryDO> inventories) {
        WmsPdaInventoryRespVO respVO = new WmsPdaInventoryRespVO();
        respVO.setWarehouseId(warehouseId);
        if (warehouseId != null) {
            WmsWarehouseDO warehouse = warehouseService.validateWarehouseExists(warehouseId);
            if (warehouse != null) {
                respVO.setWarehouseName(warehouse.getName());
            }
        }
        if (sku != null) {
            respVO.setSkuId(sku.getId());
            respVO.setSkuCode(sku.getCode());
            respVO.setSkuName(sku.getName());
            WmsItemDO item = itemService.getItemMap(Collections.singleton(sku.getItemId())).get(sku.getItemId());
            if (item != null) {
                respVO.setItemName(item.getName());
                respVO.setUnit(item.getUnit());
            }
        }
        List<WmsPdaInventoryRespVO.InventoryItem> items = new ArrayList<>();
        if (CollUtil.isNotEmpty(inventories)) {
            for (WmsInventoryDO inv : inventories) {
                WmsPdaInventoryRespVO.InventoryItem item = new WmsPdaInventoryRespVO.InventoryItem();
                item.setInventoryId(inv.getId());
                item.setQuantity(inv.getQuantity());
                item.setAvailableQuantity(inv.getAvailableQuantity());
                items.add(item);
            }
        }
        respVO.setInventories(items);
        return respVO;
    }

}
