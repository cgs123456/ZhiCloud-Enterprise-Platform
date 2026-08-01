package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.period.ErpPeriodCloseResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.period.ErpPeriodPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.period.ErpPeriodRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.period.ErpPeriodSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpPeriodDO;
import cn.iocoder.yudao.module.erp.enums.finance.ErpPeriodStatusEnum;
import cn.iocoder.yudao.module.erp.service.finance.ErpPeriodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * ERP 会计期间 + 期末处理 Controller（P0-6）
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - ERP 会计期间与期末处理")
@RestController
@RequestMapping("/erp/period")
@Validated
public class ErpPeriodController {

    @Resource
    private ErpPeriodService periodService;

    // ==================== 会计期间 CRUD ====================

    @PostMapping("/create")
    @Operation(summary = "创建会计期间")
    @PreAuthorize("@ss.hasPermission('erp:period:create')")
    public CommonResult<Long> createPeriod(@Valid @RequestBody ErpPeriodSaveReqVO createReqVO) {
        return success(periodService.createPeriod(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新会计期间")
    @PreAuthorize("@ss.hasPermission('erp:period:create')")
    public CommonResult<Boolean> updatePeriod(@Valid @RequestBody ErpPeriodSaveReqVO updateReqVO) {
        periodService.updatePeriod(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除会计期间")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:period:create')")
    public CommonResult<Boolean> deletePeriod(@RequestParam("id") Long id) {
        periodService.deletePeriod(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取会计期间")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:period:query')")
    public CommonResult<ErpPeriodRespVO> getPeriod(@RequestParam("id") Long id) {
        ErpPeriodDO period = periodService.getPeriod(id);
        if (period == null) {
            return success(null);
        }
        ErpPeriodRespVO respVO = BeanUtils.toBean(period, ErpPeriodRespVO.class);
        // 填充状态名称
        for (ErpPeriodStatusEnum statusEnum : ErpPeriodStatusEnum.values()) {
            if (statusEnum.getStatus().equals(period.getStatus())) {
                respVO.setStatusName(statusEnum.getName());
                break;
            }
        }
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询会计期间")
    @PreAuthorize("@ss.hasPermission('erp:period:query')")
    public CommonResult<PageResult<ErpPeriodRespVO>> getPeriodPage(@Valid ErpPeriodPageReqVO pageReqVO) {
        PageResult<ErpPeriodDO> pageResult = periodService.getPeriodPage(pageReqVO);
        PageResult<ErpPeriodRespVO> result = BeanUtils.toBean(pageResult, ErpPeriodRespVO.class);
        // 填充状态名称
        if (result.getList() != null) {
            for (ErpPeriodRespVO respVO : result.getList()) {
                for (ErpPeriodStatusEnum statusEnum : ErpPeriodStatusEnum.values()) {
                    if (statusEnum.getStatus().equals(respVO.getStatus())) {
                        respVO.setStatusName(statusEnum.getName());
                        break;
                    }
                }
            }
        }
        return success(result);
    }

    // ==================== 期末处理链 ====================

    @PostMapping("/month-check")
    @Operation(summary = "执行月末检查", description = "统计本期间内未审核单据数量")
    @Parameter(name = "periodId", description = "期间编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:period:month-check')")
    public CommonResult<ErpPeriodCloseResultRespVO> executeMonthCheck(@RequestParam("periodId") Long periodId) {
        return success(periodService.executeMonthCheck(periodId));
    }

    @PostMapping("/revalue")
    @Operation(summary = "执行调汇", description = "基于期末汇率调整外币账户余额（简化版，当前账户体系未启用外币时跳过）")
    @PreAuthorize("@ss.hasPermission('erp:period:revalue')")
    public CommonResult<ErpPeriodCloseResultRespVO> executeRevaluation(
            @RequestParam("periodId") Long periodId,
            @RequestParam(value = "periodExchangeRate", required = false) BigDecimal periodExchangeRate) {
        if (periodExchangeRate == null) {
            periodExchangeRate = BigDecimal.ONE;
        }
        return success(periodService.executeRevaluation(periodId, periodExchangeRate));
    }

    @PostMapping("/transfer")
    @Operation(summary = "执行损益结转", description = "基于本期间已审核的销售出库单和采购入库单汇总本期净利润")
    @Parameter(name = "periodId", description = "期间编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:period:transfer')")
    public CommonResult<ErpPeriodCloseResultRespVO> executeProfitLossTransfer(@RequestParam("periodId") Long periodId) {
        return success(periodService.executeProfitLossTransfer(periodId));
    }

    @PostMapping("/close")
    @Operation(summary = "关账", description = "前置条件：已执行月末检查 + 损益结转，且无未审核单据")
    @Parameter(name = "periodId", description = "期间编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:period:close')")
    public CommonResult<Boolean> closePeriod(@RequestParam("periodId") Long periodId) {
        periodService.closePeriod(periodId);
        return success(true);
    }

}
