package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.cashflow.ErpCashFlowPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.cashflow.ErpCashFlowRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.cashflow.ErpCashFlowSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpCashFlowDO;
import cn.iocoder.yudao.module.erp.service.finance.ErpCashFlowService;
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

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 现金流记录")
@RestController
@RequestMapping("/erp/cash-flow")
@Validated
public class ErpCashFlowController {

    @Resource
    private ErpCashFlowService cashFlowService;

    @PostMapping("/create")
    @Operation(summary = "创建现金流记录")
    @PreAuthorize("@ss.hasPermission('erp:cash-flow:create')")
    public CommonResult<Long> createCashFlow(@Valid @RequestBody ErpCashFlowSaveReqVO createReqVO) {
        return success(cashFlowService.createCashFlow(createReqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除现金流记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:cash-flow:delete')")
    public CommonResult<Boolean> deleteCashFlow(@RequestParam("id") Long id) {
        cashFlowService.deleteCashFlow(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得现金流记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:cash-flow:query')")
    public CommonResult<ErpCashFlowRespVO> getCashFlow(@RequestParam("id") Long id) {
        ErpCashFlowDO cashFlow = cashFlowService.getCashFlow(id);
        return success(BeanUtils.toBean(cashFlow, ErpCashFlowRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得现金流记录分页")
    @PreAuthorize("@ss.hasPermission('erp:cash-flow:query')")
    public CommonResult<PageResult<ErpCashFlowRespVO>> getCashFlowPage(@Valid ErpCashFlowPageReqVO pageReqVO) {
        PageResult<ErpCashFlowDO> pageResult = cashFlowService.getCashFlowPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpCashFlowRespVO.class));
    }

    @GetMapping("/list-by-period")
    @Operation(summary = "按期间获取现金流记录列表")
    @PreAuthorize("@ss.hasPermission('erp:cash-flow:query')")
    public CommonResult<List<ErpCashFlowRespVO>> getCashFlowByPeriod(
            @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        List<ErpCashFlowDO> list = cashFlowService.getCashFlowByPeriod(startDate, endDate);
        return success(BeanUtils.toBean(list, ErpCashFlowRespVO.class));
    }

}