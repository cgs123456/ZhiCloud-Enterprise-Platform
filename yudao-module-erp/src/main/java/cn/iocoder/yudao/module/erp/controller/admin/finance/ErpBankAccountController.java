package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.bankaccount.ErpBankAccountPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.bankaccount.ErpBankAccountRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.bankaccount.ErpBankAccountSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpBankAccountDO;
import cn.iocoder.yudao.module.erp.service.finance.ErpBankAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 银行账户")
@RestController
@RequestMapping("/erp/bank-account")
@Validated
public class ErpBankAccountController {

    @Resource
    private ErpBankAccountService bankAccountService;

    @PostMapping("/create")
    @Operation(summary = "创建银行账户")
    @PreAuthorize("@ss.hasPermission('erp:bank-account:create')")
    public CommonResult<Long> createBankAccount(@Valid @RequestBody ErpBankAccountSaveReqVO createReqVO) {
        return success(bankAccountService.createBankAccount(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新银行账户")
    @PreAuthorize("@ss.hasPermission('erp:bank-account:update')")
    public CommonResult<Boolean> updateBankAccount(@Valid @RequestBody ErpBankAccountSaveReqVO updateReqVO) {
        bankAccountService.updateBankAccount(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除银行账户")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:bank-account:delete')")
    public CommonResult<Boolean> deleteBankAccount(@RequestParam("id") Long id) {
        bankAccountService.deleteBankAccount(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得银行账户")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:bank-account:query')")
    public CommonResult<ErpBankAccountRespVO> getBankAccount(@RequestParam("id") Long id) {
        ErpBankAccountDO bankAccount = bankAccountService.getBankAccount(id);
        return success(BeanUtils.toBean(bankAccount, ErpBankAccountRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得银行账户分页")
    @PreAuthorize("@ss.hasPermission('erp:bank-account:query')")
    public CommonResult<PageResult<ErpBankAccountRespVO>> getBankAccountPage(@Valid ErpBankAccountPageReqVO pageReqVO) {
        PageResult<ErpBankAccountDO> pageResult = bankAccountService.getBankAccountPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpBankAccountRespVO.class));
    }

}