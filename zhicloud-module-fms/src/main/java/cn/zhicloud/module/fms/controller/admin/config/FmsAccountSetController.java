package cn.zhicloud.module.fms.controller.admin.config;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsAccountSetInitializeReqVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsAccountSetRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsAccountBookRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsAccountSetVoucherRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsAccountSetSaveReqVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsBalanceSheetRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsCashFlowStatementRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsIncomeStatementRespVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsAccountSetDO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsAccountUserDO;
import cn.zhicloud.module.fms.service.config.FmsAccountSetService;
import cn.zhicloud.module.fms.service.config.FmsAccountUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;
import static cn.zhicloud.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - FMS 账套")
@RestController
@RequestMapping("/fms/config/account-set")
@Validated
public class FmsAccountSetController {

    @Resource
    private FmsAccountSetService accountSetService;
    @Resource
    private FmsAccountUserService accountUserService;
    @GetMapping("/list")
    @Operation(summary = "获得当前用户的账套列表")
    public CommonResult<List<FmsAccountSetRespVO>> getAccountSetList() {
        Long userId = getLoginUserId();
        List<FmsAccountSetDO> list = accountSetService.getAccountSetList(userId);
        List<FmsAccountSetRespVO> respList = BeanUtils.toBean(list, FmsAccountSetRespVO.class);
        // 组装当前用户在各账套的 level / defaultStatus / founder
        Map<Long, FmsAccountUserDO> memberMap = list.stream()
                .collect(Collectors.toMap(FmsAccountSetDO::getId,
                        accountSet -> accountUserService.getAccountUser(accountSet.getId(), userId),
                        (a, b) -> a));
        respList.forEach(item -> {
            FmsAccountUserDO member = memberMap.get(item.getId());
            item.setLevel(member == null ? null : member.getLevel());
            item.setDefaultStatus(member != null && Boolean.TRUE.equals(member.getDefaultStatus()));
            item.setFounder(item.getCreateTime() != null && member != null
                    && String.valueOf(userId).equals(member.getCreator()));
        });
        return success(respList);
    }

    @GetMapping("/get")
    @Operation(summary = "获得账套")
    @Parameter(name = "id", description = "账套编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:account-set:query')")
    public CommonResult<FmsAccountSetRespVO> getAccountSet(@RequestParam("id") Long id) {
        FmsAccountSetDO accountSet = accountSetService.getAccountSet(id);
        return success(BeanUtils.toBean(accountSet, FmsAccountSetRespVO.class));
    }

    @GetMapping("/account-books")
    @Operation(summary = "获得账套关联的 ERP 账簿列表",
            description = "V191 打通『账套 → 账簿 → 凭证』层级后，账套详情页据此展示本账套挂了哪些账簿；跨模块只读查询，经 erp.api 边界包")
    @Parameter(name = "id", description = "账套编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('fms:config:account-set:query')")
    public CommonResult<List<FmsAccountBookRespVO>> getAccountBookList(@RequestParam("id") Long id) {
        List<FmsAccountBookRespVO> list = accountSetService.getAccountBookList(id);
        return success(list);
    }

    @GetMapping("/vouchers")
    @Operation(summary = "分页查询账套下的 ERP 凭证流水",
            description = "V193：账套 → 账簿 → 凭证层级打通后账套详情页的凭证流水；跨模块只读查询，经 erp.api 边界包，不建平行表")
    @Parameter(name = "id", description = "账套编号", required = true, example = "1")
    @Parameters({
            @Parameter(name = "pageNo", description = "页码", example = "1"),
            @Parameter(name = "pageSize", description = "每页条数", example = "10") })
    @PreAuthorize("@ss.hasPermission('fms:config:account-set:query')")
    public CommonResult<PageResult<FmsAccountSetVoucherRespVO>> getVoucherPage(
            @RequestParam("id") Long id,
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return success(accountSetService.getVoucherPage(id, pageNo, pageSize));
    }

    @GetMapping("/balance-sheet")
    @Operation(summary = "账套视角资产负债表",
            description = "FMS/ERP 融合：按账套出报表，数据来自 ERP 已审核凭证（账套 → 账簿 → 凭证链路），跨模块只读查询，经 erp.api 边界包")
    @Parameters({
            @Parameter(name = "id", description = "账套编号", required = true, example = "1"),
            @Parameter(name = "asOfDate", description = "报表日期", required = true, example = "2026-07-31") })
    @PreAuthorize("@ss.hasPermission('fms:config:account-set:query')")
    public CommonResult<FmsBalanceSheetRespVO> getBalanceSheet(
            @RequestParam("id") Long id,
            @RequestParam("asOfDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate asOfDate) {
        return success(accountSetService.getBalanceSheet(id, asOfDate));
    }

    @GetMapping("/income-statement")
    @Operation(summary = "账套视角利润表",
            description = "FMS/ERP 融合：按账套出报表，数据来自 ERP 已审核凭证（账套 → 账簿 → 凭证链路），跨模块只读查询，经 erp.api 边界包")
    @Parameters({
            @Parameter(name = "id", description = "账套编号", required = true, example = "1"),
            @Parameter(name = "startDate", description = "起始日期", required = true, example = "2026-01-01"),
            @Parameter(name = "endDate", description = "结束日期", required = true, example = "2026-01-31") })
    @PreAuthorize("@ss.hasPermission('fms:config:account-set:query')")
    public CommonResult<FmsIncomeStatementRespVO> getIncomeStatement(
            @RequestParam("id") Long id,
            @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return success(accountSetService.getIncomeStatement(id, startDate, endDate));
    }

    @GetMapping("/cash-flow-statement")
    @Operation(summary = "账套视角现金流量表",
            description = "FMS/ERP 融合：按账套出报表，数据来自 ERP 已审核凭证（账套 → 账簿 → 凭证链路），跨模块只读查询，经 erp.api 边界包")
    @Parameters({
            @Parameter(name = "id", description = "账套编号", required = true, example = "1"),
            @Parameter(name = "startDate", description = "起始日期", required = true, example = "2026-01-01"),
            @Parameter(name = "endDate", description = "结束日期", required = true, example = "2026-01-31") })
    @PreAuthorize("@ss.hasPermission('fms:config:account-set:query')")
    public CommonResult<FmsCashFlowStatementRespVO> getCashFlowStatement(
            @RequestParam("id") Long id,
            @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return success(accountSetService.getCashFlowStatement(id, startDate, endDate));
    }

    @PostMapping("/create")
    @Operation(summary = "创建账套")
    @PreAuthorize("@ss.hasPermission('fms:config:account-set:create')")
    public CommonResult<Long> createAccountSet(@Valid @RequestBody FmsAccountSetSaveReqVO createReqVO) {
        return success(accountSetService.createAccountSet(getLoginUserId(), createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新账套")
    @PreAuthorize("@ss.hasPermission('fms:config:account-set:update')")
    public CommonResult<Boolean> updateAccountSet(@Valid @RequestBody FmsAccountSetSaveReqVO updateReqVO) {
        accountSetService.updateAccountSet(getLoginUserId(), updateReqVO);
        return success(true);
    }

    @PutMapping("/initialize")
    @Operation(summary = "初始化账套")
    @PreAuthorize("@ss.hasPermission('fms:config:account-set:initialize')")
    public CommonResult<Boolean> initializeAccountSet(@Valid @RequestBody FmsAccountSetInitializeReqVO reqVO) {
        accountSetService.initializeAccountSet(reqVO);
        return success(true);
    }

}
