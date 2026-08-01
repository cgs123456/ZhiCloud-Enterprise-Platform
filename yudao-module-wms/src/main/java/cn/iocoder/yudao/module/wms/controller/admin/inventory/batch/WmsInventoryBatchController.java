package cn.iocoder.yudao.module.wms.controller.admin.inventory.batch;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.wms.controller.admin.inventory.batch.vo.WmsInventoryBatchPageReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.inventory.batch.vo.WmsInventoryBatchRespVO;
import cn.iocoder.yudao.module.wms.controller.admin.inventory.batch.vo.WmsInventoryBatchSaveReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.inventory.batch.vo.WmsInventoryBatchStrategyRespVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.inventory.WmsInventoryBatchDO;
import cn.iocoder.yudao.module.wms.service.inventory.batch.WmsInventoryBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * WMS 库存批次 Controller
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - WMS 库存批次")
@RestController
@RequestMapping("/wms/inventory-batch")
@Validated
public class WmsInventoryBatchController {

    @Resource
    private WmsInventoryBatchService inventoryBatchService;

    @PostMapping("/create")
    @Operation(summary = "创建库存批次")
    @PreAuthorize("@ss.hasPermission('wms:inventory-batch:create')")
    public CommonResult<Long> createBatch(@Valid @RequestBody WmsInventoryBatchSaveReqVO createReqVO) {
        return success(inventoryBatchService.createBatch(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新库存批次")
    @PreAuthorize("@ss.hasPermission('wms:inventory-batch:update')")
    public CommonResult<Boolean> updateBatch(@Valid @RequestBody WmsInventoryBatchSaveReqVO updateReqVO) {
        inventoryBatchService.updateBatch(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除库存批次")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('wms:inventory-batch:delete')")
    public CommonResult<Boolean> deleteBatch(@RequestParam("id") Long id) {
        inventoryBatchService.deleteBatch(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得库存批次")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('wms:inventory-batch:query')")
    public CommonResult<WmsInventoryBatchRespVO> getBatch(@RequestParam("id") Long id) {
        WmsInventoryBatchDO batch = inventoryBatchService.getBatch(id);
        return success(BeanUtils.toBean(batch, WmsInventoryBatchRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得库存批次分页")
    @PreAuthorize("@ss.hasPermission('wms:inventory-batch:query')")
    public CommonResult<PageResult<WmsInventoryBatchRespVO>> getBatchPage(@Valid WmsInventoryBatchPageReqVO pageReqVO) {
        PageResult<WmsInventoryBatchDO> pageResult = inventoryBatchService.getBatchPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, WmsInventoryBatchRespVO.class));
    }

    @GetMapping("/list-by-inventory")
    @Operation(summary = "按库存编号查询批次明细列表")
    @Parameter(name = "inventoryId", description = "库存编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('wms:inventory-batch:query')")
    public CommonResult<List<WmsInventoryBatchRespVO>> getBatchesByInventoryId(
            @RequestParam("inventoryId") Long inventoryId) {
        List<WmsInventoryBatchDO> list = inventoryBatchService.getBatchesByInventoryId(inventoryId);
        return success(BeanUtils.toBean(list, WmsInventoryBatchRespVO.class));
    }

    @GetMapping("/expiring")
    @Operation(summary = "查询即将过期的批次（预警）")
    @Parameter(name = "days", description = "临期天数", required = true, example = "7")
    @PreAuthorize("@ss.hasPermission('wms:inventory-batch:query')")
    public CommonResult<List<WmsInventoryBatchRespVO>> getExpiringBatches(@RequestParam("days") Integer days) {
        List<WmsInventoryBatchDO> list = inventoryBatchService.getExpiringBatches(days);
        return success(BeanUtils.toBean(list, WmsInventoryBatchRespVO.class));
    }

    @GetMapping("/expired")
    @Operation(summary = "查询已过期批次")
    @PreAuthorize("@ss.hasPermission('wms:inventory-batch:query')")
    public CommonResult<List<WmsInventoryBatchRespVO>> getExpiredBatches() {
        List<WmsInventoryBatchDO> list = inventoryBatchService.getExpiredBatches();
        return success(BeanUtils.toBean(list, WmsInventoryBatchRespVO.class));
    }

    @GetMapping("/fifo")
    @Operation(summary = "应用 FIFO 先进先出策略")
    @Parameter(name = "inventoryId", description = "库存编号", required = true, example = "1024")
    @Parameter(name = "quantity", description = "需求数量", required = true, example = "100.00")
    @PreAuthorize("@ss.hasPermission('wms:inventory-batch:query')")
    public CommonResult<WmsInventoryBatchStrategyRespVO> applyFifoStrategy(
            @RequestParam("inventoryId") Long inventoryId,
            @RequestParam("quantity") BigDecimal quantity) {
        return success(inventoryBatchService.applyFifoStrategy(inventoryId, quantity));
    }

    @GetMapping("/fefo")
    @Operation(summary = "应用 FEFO 先到期先出策略")
    @Parameter(name = "inventoryId", description = "库存编号", required = true, example = "1024")
    @Parameter(name = "quantity", description = "需求数量", required = true, example = "100.00")
    @PreAuthorize("@ss.hasPermission('wms:inventory-batch:query')")
    public CommonResult<WmsInventoryBatchStrategyRespVO> applyFefoStrategy(
            @RequestParam("inventoryId") Long inventoryId,
            @RequestParam("quantity") BigDecimal quantity) {
        return success(inventoryBatchService.applyFefoStrategy(inventoryId, quantity));
    }

}
