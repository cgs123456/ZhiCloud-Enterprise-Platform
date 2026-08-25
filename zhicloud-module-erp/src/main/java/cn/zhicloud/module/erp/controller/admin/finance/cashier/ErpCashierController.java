package cn.zhicloud.module.erp.controller.admin.finance.cashier;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.finance.cashier.vo.ErpBankStatementRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.cashier.vo.ErpCashierPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.cashier.vo.ErpCashierRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.cashier.vo.ErpCashierSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpBankAccountDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.cashier.ErpCashierDO;
import cn.zhicloud.module.erp.service.finance.ErpBankAccountService;
import cn.zhicloud.module.erp.service.finance.cashier.ErpBankDirectLinkService;
import cn.zhicloud.module.erp.service.finance.cashier.ErpCashierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;
import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Tag(name = "管理后台 - ERP 出纳管理")
@RestController
@RequestMapping("/erp/cashier")
@Validated
public class ErpCashierController {

    @Resource
    private ErpCashierService cashierService;
    @Resource
    private ErpBankAccountService bankAccountService;
    @Resource
    private ErpBankDirectLinkService bankDirectLinkService;

    @PostMapping("/create")
    @Operation(summary = "创建出纳单")
    @PreAuthorize("@ss.hasPermission('erp:cashier:create')")
    public CommonResult<Long> createCashier(@Valid @RequestBody ErpCashierSaveReqVO createReqVO) {
        return success(cashierService.createCashier(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新出纳单")
    @PreAuthorize("@ss.hasPermission('erp:cashier:update')")
    public CommonResult<Boolean> updateCashier(@Valid @RequestBody ErpCashierSaveReqVO updateReqVO) {
        cashierService.updateCashier(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除出纳单")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:cashier:delete')")
    public CommonResult<Boolean> deleteCashier(@RequestParam("ids") List<Long> ids) {
        cashierService.deleteCashier(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得出纳单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:cashier:query')")
    public CommonResult<ErpCashierRespVO> getCashier(@RequestParam("id") Long id) {
        ErpCashierDO cashier = cashierService.getCashier(id);
        if (cashier == null) {
            return success(null);
        }
        ErpCashierRespVO respVO = BeanUtils.toBean(cashier, ErpCashierRespVO.class);
        fillBankAccountName(respVO, cashier.getBankAccountId());
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得出纳单分页")
    @PreAuthorize("@ss.hasPermission('erp:cashier:query')")
    public CommonResult<PageResult<ErpCashierRespVO>> getCashierPage(@Valid ErpCashierPageReqVO pageReqVO) {
        PageResult<ErpCashierDO> pageResult = cashierService.getCashierPage(pageReqVO);
        PageResult<ErpCashierRespVO> respPage = BeanUtils.toBean(pageResult, ErpCashierRespVO.class);
        // 填充银行账户名称（按需查询，避免 N+1：对唯一 id 缓存）
        Map<Long, ErpBankAccountDO> accountMap = new java.util.HashMap<>();
        respPage.getList().forEach(vo -> {
            if (vo.getBankAccountId() == null) {
                return;
            }
            ErpBankAccountDO account = accountMap.computeIfAbsent(vo.getBankAccountId(),
                    bankAccountService::getBankAccount);
            if (account != null) {
                vo.setBankAccountName(account.getAccountName());
            }
        });
        return success(respPage);
    }

    @PutMapping("/submit-bank")
    @Operation(summary = "提交银行（发送支付指令）")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:cashier:submit')")
    public CommonResult<String> submitToBank(@RequestParam("id") Long id) {
        return success(cashierService.submitToBank(id));
    }

    @PutMapping("/sync-bank-status")
    @Operation(summary = "同步银行状态")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:cashier:sync')")
    public CommonResult<Integer> syncBankStatus(@RequestParam("id") Long id) {
        return success(cashierService.syncBankStatus(id));
    }

    @GetMapping("/bank-statement")
    @Operation(summary = "获取银行对账单（网银直联）")
    @PreAuthorize("@ss.hasPermission('erp:cashier:query')")
    public CommonResult<List<ErpBankStatementRespVO>> receiveBankStatement(
            @RequestParam("start") @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND) LocalDateTime end) {
        return success(bankDirectLinkService.receiveBankStatement(start, end));
    }

    private void fillBankAccountName(ErpCashierRespVO vo, Long bankAccountId) {
        if (bankAccountId == null) {
            return;
        }
        ErpBankAccountDO account = bankAccountService.getBankAccount(bankAccountId);
        if (account != null) {
            vo.setBankAccountName(account.getAccountName());
        }
    }

}
