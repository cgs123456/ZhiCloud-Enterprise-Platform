package cn.zhicloud.module.erp.api;

import cn.zhicloud.module.erp.controller.admin.finance.vo.financialstatement.ErpBalanceSheetRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.financialstatement.ErpCashFlowStatementRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.financialstatement.ErpIncomeStatementRespVO;

import java.time.LocalDate;

/**
 * ERP 财务报表查询 API（跨模块契约，位于 erp.api 边界包）
 *
 * <p>FMS/ERP 财务融合（V191–V193）后，FMS 账套配置层需要「按账套视角出三大报表」：
 * ERP 侧报表方法均已支持 accountSetId 过滤（按「账套 → 账簿 → 凭证」链路，凭证不落
 * 冗余列），但 Modulith 规则禁止跨模块直接 import service/dal，故在此 api 包发布
 * 只读契约（与 {@link ErpAccountBookApi}、{@link ErpGlVoucherApi} 同范式）。
 *
 * <p>只读：不暴露任何写操作；报表的全部数据来自 ERP 已审核凭证，无独立存储。
 *
 * @author 智云
 */
public interface ErpFinancialStatementApi {

    /**
     * 生成资产负债表
     *
     * @param accountSetId FMS 账套编号（可空；null 为租户全量）
     * @param asOfDate 报表日期
     * @return 资产负债表
     */
    ErpBalanceSheetRespVO generateBalanceSheet(Long accountSetId, LocalDate asOfDate);

    /**
     * 生成利润表
     *
     * @param accountSetId FMS 账套编号（可空；null 为租户全量）
     * @param startDate 起始日期
     * @param endDate   结束日期
     * @return 利润表
     */
    ErpIncomeStatementRespVO generateIncomeStatement(Long accountSetId, LocalDate startDate, LocalDate endDate);

    /**
     * 生成现金流量表
     *
     * @param accountSetId FMS 账套编号（可空；null 为租户全量）
     * @param startDate 起始日期
     * @param endDate   结束日期
     * @return 现金流量表
     */
    ErpCashFlowStatementRespVO generateCashFlowStatement(Long accountSetId, LocalDate startDate, LocalDate endDate);

}
