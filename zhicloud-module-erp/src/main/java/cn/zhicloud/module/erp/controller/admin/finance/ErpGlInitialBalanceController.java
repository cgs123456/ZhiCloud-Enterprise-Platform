package cn.zhicloud.module.erp.controller.admin.finance;

import cn.zhicloud.framework.apilog.core.annotation.ApiAccessLog;
import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.excel.core.util.ExcelUtils;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glinitialbalance.ErpGlInitialBalanceExcelVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glinitialbalance.ErpGlInitialBalanceRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glinitialbalance.ErpGlInitialBalanceSaveReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glinitialbalance.ErpTrialBalanceRespVO;
import cn.zhicloud.module.erp.service.finance.ErpGlInitialBalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static cn.zhicloud.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.zhicloud.framework.apilog.core.enums.OperateTypeEnum.IMPORT;
import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * ERP 期初余额 Controller（账套口径，C6③ / ADR 265）
 *
 * <p>FMS 语境页 fms/config/initial-balance 直连本组端点（V194 科目先例：
 * 单一真相在 ERP，FMS 不建平行表 / 代理 Controller）；权限串
 * erp:gl-initial-balance:{query,update,import,export}。
 *
 * @author 智云
 */
@Tag(name = "管理后台 - ERP 期初余额")
@RestController
@RequestMapping("/erp/gl-initial-balance")
@Validated
public class ErpGlInitialBalanceController {

    @Resource
    private ErpGlInitialBalanceService glInitialBalanceService;

    @GetMapping("/list")
    @Operation(summary = "获得账套某科目类别的期初余额列表", description = "含该类别的全科目树与派生年初余额；未录行金额为 0")
    @Parameters({
            @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1"),
            @Parameter(name = "subjectType", description = "科目类型（10 资产 / 20 负债 / 30 权益 / 40 收入 / 50 费用 / 60 共同）", required = true, example = "10")
    })
    @PreAuthorize("@ss.hasPermission('erp:gl-initial-balance:query')")
    public CommonResult<List<ErpGlInitialBalanceRespVO>> getInitialBalanceList(
            @RequestParam("accountSetId") Long accountSetId,
            @RequestParam("subjectType") Integer subjectType) {
        return success(glInitialBalanceService.getInitialBalanceList(accountSetId, subjectType));
    }

    @PutMapping("/save")
    @Operation(summary = "保存账套期初余额", description = "差分 upsert；账套存在已审核凭证时拒绝（服务端强校验）")
    @PreAuthorize("@ss.hasPermission('erp:gl-initial-balance:update')")
    public CommonResult<Boolean> saveInitialBalances(@Valid @RequestBody ErpGlInitialBalanceSaveReqVO saveReqVO) {
        glInitialBalanceService.saveInitialBalances(saveReqVO);
        return success(true);
    }

    @GetMapping("/trial-balance")
    @Operation(summary = "期初余额试算平衡", description = "期初差与累计差的绝对值均 < 0.005 视为平衡")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('erp:gl-initial-balance:query')")
    public CommonResult<ErpTrialBalanceRespVO> getTrialBalance(@RequestParam("accountSetId") Long accountSetId) {
        return success(glInitialBalanceService.getTrialBalance(accountSetId));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出账套期初余额 Excel")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('erp:gl-initial-balance:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportInitialBalanceExcel(@RequestParam("accountSetId") Long accountSetId,
                                          HttpServletResponse response) throws IOException {
        List<ErpGlInitialBalanceExcelVO> list = glInitialBalanceService.getInitialBalanceExcelList(accountSetId);
        ExcelUtils.write(response, "财务初始余额.xlsx", "数据", ErpGlInitialBalanceExcelVO.class, list);
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得期初余额导入模板", description = "带出全部末级科目，金额列留空；模板下载复用 import 权限")
    @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('erp:gl-initial-balance:import')")
    public void getImportTemplate(@RequestParam("accountSetId") Long accountSetId,
                                  HttpServletResponse response) throws IOException {
        List<ErpGlInitialBalanceExcelVO> list = glInitialBalanceService.getImportTemplateList(accountSetId);
        ExcelUtils.write(response, "财务初始余额导入模板.xlsx", "期初余额", ErpGlInitialBalanceExcelVO.class, list);
    }

    @PostMapping("/import")
    @Operation(summary = "导入账套期初余额", description = "按科目编码匹配；四金额全空的行跳过；返回实际写入行数")
    @Parameters({
            @Parameter(name = "accountSetId", description = "账套编号", required = true, example = "1")
    })
    @PreAuthorize("@ss.hasPermission('erp:gl-initial-balance:import')")
    @ApiAccessLog(operateType = IMPORT)
    public CommonResult<Integer> importInitialBalances(@RequestParam("accountSetId") Long accountSetId,
                                                       @RequestParam("file") MultipartFile file) throws Exception {
        List<ErpGlInitialBalanceExcelVO> list = ExcelUtils.read(file, ErpGlInitialBalanceExcelVO.class);
        return success(glInitialBalanceService.importInitialBalances(accountSetId, list));
    }

}
