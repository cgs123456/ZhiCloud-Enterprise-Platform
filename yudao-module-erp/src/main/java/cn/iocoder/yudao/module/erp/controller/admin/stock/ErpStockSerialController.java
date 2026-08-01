package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stockserial.ErpStockSerialPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stockserial.ErpStockSerialRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stockserial.ErpStockSerialSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockSerialDO;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockSerialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 库存序列号")
@RestController
@RequestMapping("/erp/stock-serial")
@Validated
public class ErpStockSerialController {

    @Resource
    private ErpStockSerialService stockSerialService;

    @PostMapping("/create")
    @Operation(summary = "创建库存序列号")
    @PreAuthorize("@ss.hasPermission('erp:stock-serial:create')")
    public CommonResult<Long> createStockSerial(@Valid @RequestBody ErpStockSerialSaveReqVO createReqVO) {
        return success(stockSerialService.createStockSerial(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新库存序列号")
    @PreAuthorize("@ss.hasPermission('erp:stock-serial:update')")
    public CommonResult<Boolean> updateStockSerial(@Valid @RequestBody ErpStockSerialSaveReqVO updateReqVO) {
        stockSerialService.updateStockSerial(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除库存序列号")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:stock-serial:delete')")
    public CommonResult<Boolean> deleteStockSerial(@RequestParam("id") Long id) {
        stockSerialService.deleteStockSerial(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得库存序列号")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:stock-serial:query')")
    public CommonResult<ErpStockSerialRespVO> getStockSerial(@RequestParam("id") Long id) {
        ErpStockSerialDO serial = stockSerialService.getStockSerial(id);
        return success(BeanUtils.toBean(serial, ErpStockSerialRespVO.class));
    }

    @GetMapping("/scan")
    @Operation(summary = "扫码查询库存序列号", description = "通过序列号扫码获取序列号详情")
    @Parameter(name = "serialNo", description = "序列号", required = true, example = "SN2026070001")
    @PreAuthorize("@ss.hasPermission('erp:stock-serial:query')")
    public CommonResult<ErpStockSerialRespVO> scanStockSerial(@RequestParam("serialNo") String serialNo) {
        ErpStockSerialDO serial = stockSerialService.getStockSerialBySerialNo(serialNo);
        return success(BeanUtils.toBean(serial, ErpStockSerialRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得库存序列号分页")
    @PreAuthorize("@ss.hasPermission('erp:stock-serial:query')")
    public CommonResult<PageResult<ErpStockSerialRespVO>> getStockSerialPage(@Valid ErpStockSerialPageReqVO pageReqVO) {
        PageResult<ErpStockSerialDO> pageResult = stockSerialService.getStockSerialPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpStockSerialRespVO.class));
    }

}
