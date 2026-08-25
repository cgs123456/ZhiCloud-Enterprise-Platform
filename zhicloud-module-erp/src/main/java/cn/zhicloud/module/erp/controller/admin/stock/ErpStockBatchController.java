package cn.zhicloud.module.erp.controller.admin.stock;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.stock.vo.stockbatch.ErpStockBatchPageReqVO;
import cn.zhicloud.module.erp.controller.admin.stock.vo.stockbatch.ErpStockBatchRespVO;
import cn.zhicloud.module.erp.controller.admin.stock.vo.stockbatch.ErpStockBatchSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.stock.ErpStockBatchDO;
import cn.zhicloud.module.erp.service.stock.ErpStockBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - ERP 库存批次")
@RestController
@RequestMapping("/erp/stock-batch")
@Validated
public class ErpStockBatchController {

    @Resource
    private ErpStockBatchService stockBatchService;

    @PostMapping("/create")
    @Operation(summary = "创建库存批次")
    @PreAuthorize("@ss.hasPermission('erp:stock-batch:create')")
    public CommonResult<Long> createStockBatch(@Valid @RequestBody ErpStockBatchSaveReqVO createReqVO) {
        return success(stockBatchService.createStockBatch(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新库存批次")
    @PreAuthorize("@ss.hasPermission('erp:stock-batch:update')")
    public CommonResult<Boolean> updateStockBatch(@Valid @RequestBody ErpStockBatchSaveReqVO updateReqVO) {
        stockBatchService.updateStockBatch(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除库存批次")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:stock-batch:delete')")
    public CommonResult<Boolean> deleteStockBatch(@RequestParam("id") Long id) {
        stockBatchService.deleteStockBatch(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得库存批次")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:stock-batch:query')")
    public CommonResult<ErpStockBatchRespVO> getStockBatch(@RequestParam("id") Long id) {
        ErpStockBatchDO batch = stockBatchService.getStockBatch(id);
        return success(BeanUtils.toBean(batch, ErpStockBatchRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得库存批次分页")
    @PreAuthorize("@ss.hasPermission('erp:stock-batch:query')")
    public CommonResult<PageResult<ErpStockBatchRespVO>> getStockBatchPage(@Valid ErpStockBatchPageReqVO pageReqVO) {
        PageResult<ErpStockBatchDO> pageResult = stockBatchService.getStockBatchPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpStockBatchRespVO.class));
    }

    @GetMapping("/expiring-list")
    @Operation(summary = "获得即将过期的批次列表", description = "默认返回早于今天但仍可用的批次")
    @Parameter(name = "date", description = "截止日期（默认今天）", example = "2026-07-29")
    @PreAuthorize("@ss.hasPermission('erp:stock-batch:query')")
    public CommonResult<List<ErpStockBatchRespVO>> getExpiringBatchList(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        List<ErpStockBatchDO> list = stockBatchService.getExpiringBatchList(date);
        return success(convertList(list, batch -> BeanUtils.toBean(batch, ErpStockBatchRespVO.class)));
    }

}
