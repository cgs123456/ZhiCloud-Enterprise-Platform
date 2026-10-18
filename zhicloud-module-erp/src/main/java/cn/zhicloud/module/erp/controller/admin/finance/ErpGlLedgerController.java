package cn.zhicloud.module.erp.controller.admin.finance;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.converters.longconverter.LongStringConverter;
import cn.zhicloud.framework.apilog.core.annotation.ApiAccessLog;
import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.util.http.HttpUtils;
import cn.zhicloud.framework.excel.core.handler.ColumnWidthMatchStyleStrategy;
import cn.zhicloud.framework.excel.core.util.ExcelUtils;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerDetailExcelVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerDetailRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerGeneralExcelVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerGeneralRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerMultiColumnExcelDataVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerMultiColumnRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerSubjectBalanceExcelVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerSubjectBalanceRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerSubjectRespVO;
import cn.zhicloud.module.erp.service.finance.ErpGlLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

import static cn.zhicloud.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * ERP 账表 Controller（账套口径，金额式四族，C6② / ADR 266）
 *
 * <p>FMS 语境账表页（fms/ledger/{detail,general,subject-balance,multi-column}）直连本组端点
 * （V194 科目先例：单一真相在 ERP，不建 FMS 平行表 / 代理层）；权限串 erp:gl-ledger:{query,export}。
 *
 * <p>本组端点无业务拒绝分支（账套无账簿 / 无凭证 / 不存在的账套一律空结果，code=400 仅由入参校验触发），
 * 因此本项不新增错误码（ADR 266 §6）。
 *
 * @author 智云
 */
@Tag(name = "管理后台 - ERP 账表")
@RestController
@RequestMapping("/erp/gl-ledger")
@Validated
public class ErpGlLedgerController {

    @Resource
    private ErpGlLedgerService glLedgerService;

    @GetMapping("/detail/list")
    @Operation(summary = "获得明细账行", description = "期初行 + 明细行（逐笔滚动余额）+ 本期合计行 + 本年累计行；"
            + "按所选科目的子树聚合；subjectId 缺省或不可解析时返回空列表")
    @Parameters({
            @Parameter(name = "accountSetId", description = "FMS 账套编号", required = true, example = "1"),
            @Parameter(name = "startMonth", description = "会计期间起始月（yyyy-MM）", required = true, example = "2026-01"),
            @Parameter(name = "endMonth", description = "会计期间结束月（yyyy-MM）", required = true, example = "2026-03"),
            @Parameter(name = "subjectId", description = "科目编号（按科目子树聚合）", example = "1001")
    })
    @PreAuthorize("@ss.hasPermission('erp:gl-ledger:query')")
    public CommonResult<List<ErpGlLedgerDetailRespVO>> getDetailList(@Valid ErpGlLedgerReqVO reqVO) {
        return success(glLedgerService.getDetailList(reqVO));
    }

    @GetMapping("/detail/export-excel")
    @Operation(summary = "导出明细账 Excel", description = "列：日期 / 凭证字号 / 摘要 / 借方 / 贷方 / 方向 / 余额")
    @PreAuthorize("@ss.hasPermission('erp:gl-ledger:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportDetail(@Valid ErpGlLedgerReqVO reqVO, HttpServletResponse response) throws IOException {
        ExcelUtils.write(response, "明细账.xlsx", "明细账", ErpGlLedgerDetailExcelVO.class,
                glLedgerService.getDetailExcelList(reqVO));
    }

    @GetMapping("/general/list")
    @Operation(summary = "获得总账行", description = "按科目分组：期初行 + 各月合计行（有发生额的月份）+ 本年累计行；"
            + "科目域 = 级次区间 + 科目编码闭区间")
    @Parameters({
            @Parameter(name = "accountSetId", description = "FMS 账套编号", required = true, example = "1"),
            @Parameter(name = "startMonth", description = "会计期间起始月（yyyy-MM）", required = true, example = "2026-01"),
            @Parameter(name = "endMonth", description = "会计期间结束月（yyyy-MM）", required = true, example = "2026-12"),
            @Parameter(name = "startSubjectId", description = "起始科目编号（按编码闭区间）", example = "1001"),
            @Parameter(name = "endSubjectId", description = "结束科目编号（按编码闭区间）", example = "6001"),
            @Parameter(name = "minLevel", description = "科目级次下限（含），缺省 1", example = "1"),
            @Parameter(name = "maxLevel", description = "科目级次上限（含），缺省 1", example = "1")
    })
    @PreAuthorize("@ss.hasPermission('erp:gl-ledger:query')")
    public CommonResult<List<ErpGlLedgerGeneralRespVO>> getGeneralList(@Valid ErpGlLedgerReqVO reqVO) {
        return success(glLedgerService.getGeneralList(reqVO));
    }

    @GetMapping("/general/export-excel")
    @Operation(summary = "导出总账 Excel", description = "列：科目编码 / 科目名称 / 期间 / 摘要 / 借方 / 贷方 / 方向 / 余额")
    @PreAuthorize("@ss.hasPermission('erp:gl-ledger:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportGeneral(@Valid ErpGlLedgerReqVO reqVO, HttpServletResponse response) throws IOException {
        ExcelUtils.write(response, "总账.xlsx", "总账", ErpGlLedgerGeneralExcelVO.class,
                glLedgerService.getGeneralExcelList(reqVO));
    }

    @GetMapping("/subject-balance/list")
    @Operation(summary = "获得科目余额表", description = "期初余额 / 本期发生额 / 本年累计发生额 / 期末余额 四组借贷列；"
            + "仅含有数据科目及其祖先；nodeType 恒为科目（金额式一期无辅助组合节点）")
    @Parameters({
            @Parameter(name = "accountSetId", description = "FMS 账套编号", required = true, example = "1"),
            @Parameter(name = "startMonth", description = "会计期间起始月（yyyy-MM）", required = true, example = "2026-01"),
            @Parameter(name = "endMonth", description = "会计期间结束月（yyyy-MM）", required = true, example = "2026-12"),
            @Parameter(name = "startSubjectId", description = "起始科目编号（按编码闭区间）", example = "1001"),
            @Parameter(name = "endSubjectId", description = "结束科目编号（按编码闭区间）", example = "6001"),
            @Parameter(name = "minLevel", description = "科目级次下限（含），缺省 1", example = "1"),
            @Parameter(name = "maxLevel", description = "科目级次上限（含），缺省 1（页面传 FMS_SUBJECT_LEVEL_MAX）", example = "8")
    })
    @PreAuthorize("@ss.hasPermission('erp:gl-ledger:query')")
    public CommonResult<List<ErpGlLedgerSubjectBalanceRespVO>> getSubjectBalanceList(@Valid ErpGlLedgerReqVO reqVO) {
        return success(glLedgerService.getSubjectBalanceList(reqVO));
    }

    @GetMapping("/subject-balance/export-excel")
    @Operation(summary = "导出科目余额表 Excel", description = "树按 DFS 序扁平化，列：科目级次 / 科目编码 / 科目名称 + 8 金额列")
    @PreAuthorize("@ss.hasPermission('erp:gl-ledger:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSubjectBalance(@Valid ErpGlLedgerReqVO reqVO,
                                     HttpServletResponse response) throws IOException {
        ExcelUtils.write(response, "科目余额表.xlsx", "科目余额表", ErpGlLedgerSubjectBalanceExcelVO.class,
                glLedgerService.getSubjectBalanceExcelList(reqVO));
    }

    @GetMapping("/multi-column/list")
    @Operation(summary = "获得多栏账", description = "专栏 = 所选科目的直接子科目（balanceDirection 用 ERP 值域 10/20）；"
            + "行 = 期初行 + 明细行（columnAmounts）+ 本期合计行（columnAmounts）")
    @Parameters({
            @Parameter(name = "accountSetId", description = "FMS 账套编号", required = true, example = "1"),
            @Parameter(name = "startMonth", description = "会计期间起始月（yyyy-MM）", required = true, example = "2026-01"),
            @Parameter(name = "endMonth", description = "会计期间结束月（yyyy-MM）", required = true, example = "2026-03"),
            @Parameter(name = "subjectId", description = "父科目编号（专栏为其直接子科目）", example = "1001")
    })
    @PreAuthorize("@ss.hasPermission('erp:gl-ledger:query')")
    public CommonResult<ErpGlLedgerMultiColumnRespVO> getMultiColumn(@Valid ErpGlLedgerReqVO reqVO) {
        return success(glLedgerService.getMultiColumn(reqVO));
    }

    @GetMapping("/multi-column/export-excel")
    @Operation(summary = "导出多栏账 Excel", description = "动态表头：日期 / 凭证字号 / 摘要 / 借方 / 贷方 / 方向 / 余额 + 各子科目列")
    @PreAuthorize("@ss.hasPermission('erp:gl-ledger:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportMultiColumn(@Valid ErpGlLedgerReqVO reqVO, HttpServletResponse response) throws IOException {
        ErpGlLedgerMultiColumnExcelDataVO data = glLedgerService.getMultiColumnExcelData(reqVO);
        writeDynamicExcel(response, "多栏账.xlsx", "多栏账", data.getHead(), data.getRows());
    }

    @GetMapping("/detail-subjects")
    @Operation(summary = "获得明细账科目树节点", description = "期间内有发生额或期初非零的科目及其祖先；平铺返回，"
            + "由前端 handleTree 建树")
    @Parameters({
            @Parameter(name = "accountSetId", description = "FMS 账套编号", required = true, example = "1"),
            @Parameter(name = "startMonth", description = "会计期间起始月（yyyy-MM）", required = true, example = "2026-01"),
            @Parameter(name = "endMonth", description = "会计期间结束月（yyyy-MM）", required = true, example = "2026-03")
    })
    @PreAuthorize("@ss.hasPermission('erp:gl-ledger:query')")
    public CommonResult<List<ErpGlLedgerSubjectRespVO>> getDetailSubjectList(@Valid ErpGlLedgerReqVO reqVO) {
        return success(glLedgerService.getDetailSubjectList(reqVO));
    }

    /**
     * 动态表头 Excel 写出（多栏账专用）
     *
     * <p>框架 {@link ExcelUtils#write} 只支持类头（{@code @ExcelProperty} 固定列），多栏账的专栏随
     * 所选科目的直接子科目变化，无法用类头表达。此处复用其行数护栏常量与列宽 handler，
     * 直接用 FastExcel 写动态表头；单场景直接实现，<b>不扩框架工具类</b>（ADR 266 §8-11）。
     *
     * <p>响应头必须最后写（与 ExcelUtils 同序），避免异常时 contentType 已被改写。
     *
     * @param response 响应
     * @param filename 文件名
     * @param sheetName sheet 名
     * @param head 表头
     * @param rows 行数据
     * @throws IOException 写出失败
     */
    private void writeDynamicExcel(HttpServletResponse response, String filename, String sheetName,
                                   List<List<String>> head, List<List<Object>> rows) throws IOException {
        if (rows != null && rows.size() > ExcelUtils.MAX_EXPORT_ROWS) {
            throw new ServiceException(GlobalErrorCodeConstants.BAD_REQUEST.getCode(),
                    "导出数据量过大（上限 " + ExcelUtils.MAX_EXPORT_ROWS + " 行），请缩小查询范围或改用异步导出任务中心");
        }
        FastExcelFactory.write(response.getOutputStream())
                .head(head)
                .autoCloseStream(false)
                .registerWriteHandler(new ColumnWidthMatchStyleStrategy())
                .registerConverter(new LongStringConverter())
                .sheet(sheetName)
                .doWrite(rows);
        response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(filename));
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
    }

}
