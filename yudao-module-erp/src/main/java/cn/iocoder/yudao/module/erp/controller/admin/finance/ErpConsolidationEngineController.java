package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.consolidation.ErpConsolidationWorksheetRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpConsolidationWorksheetDO;
import cn.iocoder.yudao.module.erp.enums.finance.ErpConsolidationEliminationTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpWorksheetStatusEnum;
import cn.iocoder.yudao.module.erp.service.finance.ErpConsolidationEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * ERP 合并报表自动抵消引擎 Controller（P1）
 *
 * <p>暴露自动抵消分录生成、合并资产负债表 / 合并利润表生成接口。
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - ERP 合并报表抵消引擎")
@RestController
@RequestMapping("/erp/consolidation-engine")
@Validated
public class ErpConsolidationEngineController {

    @Resource
    private ErpConsolidationEngineService consolidationEngineService;

    @PostMapping("/generate-investment-equity")
    @Operation(summary = "生成投资权益抵消分录")
    @PreAuthorize("@ss.hasPermission('erp:consolidation-engine:generate')")
    public CommonResult<List<ErpConsolidationWorksheetRespVO>> generateInvestmentEquityElimination(
            @RequestParam("parentId") Long parentId,
            @RequestParam("subId") Long subId,
            @RequestParam("period") String period) {
        List<ErpConsolidationWorksheetDO> worksheets = consolidationEngineService
                .generateInvestmentEquityElimination(parentId, subId, period);
        return success(convertToRespVOList(worksheets));
    }

    @PostMapping("/generate-intercompany-ar-ap")
    @Operation(summary = "生成内部应收应付抵消分录")
    @PreAuthorize("@ss.hasPermission('erp:consolidation-engine:generate')")
    public CommonResult<List<ErpConsolidationWorksheetRespVO>> generateIntercompanyArApElimination(
            @RequestParam("parentId") Long parentId,
            @RequestParam("subId") Long subId,
            @RequestParam("period") String period) {
        List<ErpConsolidationWorksheetDO> worksheets = consolidationEngineService
                .generateIntercompanyArApElimination(parentId, subId, period);
        return success(convertToRespVOList(worksheets));
    }

    @PostMapping("/generate-intercompany-sale-cogs")
    @Operation(summary = "生成内部销售成本抵消分录")
    @PreAuthorize("@ss.hasPermission('erp:consolidation-engine:generate')")
    public CommonResult<List<ErpConsolidationWorksheetRespVO>> generateIntercompanySaleCogsElimination(
            @RequestParam("parentId") Long parentId,
            @RequestParam("subId") Long subId,
            @RequestParam("period") String period) {
        List<ErpConsolidationWorksheetDO> worksheets = consolidationEngineService
                .generateIntercompanySaleCogsElimination(parentId, subId, period);
        return success(convertToRespVOList(worksheets));
    }

    @PostMapping("/generate-intercompany-fa")
    @Operation(summary = "生成内部固定资产交易抵消分录")
    @PreAuthorize("@ss.hasPermission('erp:consolidation-engine:generate')")
    public CommonResult<List<ErpConsolidationWorksheetRespVO>> generateIntercompanyFaElimination(
            @RequestParam("parentId") Long parentId,
            @RequestParam("subId") Long subId,
            @RequestParam("period") String period) {
        List<ErpConsolidationWorksheetDO> worksheets = consolidationEngineService
                .generateIntercompanyFaElimination(parentId, subId, period);
        return success(convertToRespVOList(worksheets));
    }

    @PostMapping("/generate-all-eliminations")
    @Operation(summary = "批量生成所有合并范围的抵消分录")
    @Parameter(name = "period", description = "合并周期（yyyyMM）", required = true, example = "202607")
    @PreAuthorize("@ss.hasPermission('erp:consolidation-engine:generate')")
    public CommonResult<List<ErpConsolidationWorksheetRespVO>> generateAllEliminations(
            @RequestParam("period") String period) {
        List<ErpConsolidationWorksheetDO> worksheets = consolidationEngineService.generateAllEliminations(period);
        return success(convertToRespVOList(worksheets));
    }

    @GetMapping("/consolidated-balance-sheet")
    @Operation(summary = "生成合并资产负债表", description = "返回按抵消类型名称分组的合并金额")
    @Parameter(name = "period", description = "合并周期（yyyyMM）", required = true, example = "202607")
    @PreAuthorize("@ss.hasPermission('erp:consolidation-engine:report')")
    public CommonResult<Map<String, BigDecimal>> generateConsolidatedBalanceSheet(
            @RequestParam("period") String period) {
        return success(consolidationEngineService.generateConsolidatedBalanceSheet(period));
    }

    @GetMapping("/consolidated-income-statement")
    @Operation(summary = "生成合并利润表", description = "返回按抵消类型名称分组的合并金额")
    @Parameter(name = "period", description = "合并周期（yyyyMM）", required = true, example = "202607")
    @PreAuthorize("@ss.hasPermission('erp:consolidation-engine:report')")
    public CommonResult<Map<String, BigDecimal>> generateConsolidatedIncomeStatement(
            @RequestParam("period") String period) {
        return success(consolidationEngineService.generateConsolidatedIncomeStatement(period));
    }

    // ==================== 内部辅助方法 ====================

    private List<ErpConsolidationWorksheetRespVO> convertToRespVOList(List<ErpConsolidationWorksheetDO> worksheets) {
        if (worksheets == null || worksheets.isEmpty()) {
            return new ArrayList<>();
        }
        List<ErpConsolidationWorksheetRespVO> result = new ArrayList<>(worksheets.size());
        for (ErpConsolidationWorksheetDO ws : worksheets) {
            ErpConsolidationWorksheetRespVO respVO = BeanUtils.toBean(ws, ErpConsolidationWorksheetRespVO.class);
            if (respVO.getEliminationType() != null) {
                for (ErpConsolidationEliminationTypeEnum typeEnum : ErpConsolidationEliminationTypeEnum.values()) {
                    if (typeEnum.getType().equals(respVO.getEliminationType())) {
                        respVO.setEliminationTypeName(typeEnum.getName());
                        break;
                    }
                }
            }
            if (respVO.getStatus() != null) {
                for (ErpWorksheetStatusEnum statusEnum : ErpWorksheetStatusEnum.values()) {
                    if (statusEnum.getStatus().equals(respVO.getStatus())) {
                        respVO.setStatusName(statusEnum.getName());
                        break;
                    }
                }
            }
            result.add(respVO);
        }
        return result;
    }

}
