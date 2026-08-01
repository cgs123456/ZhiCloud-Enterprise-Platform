package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.financialstatement.ErpBalanceSheetRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.financialstatement.ErpCashFlowStatementRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.financialstatement.ErpIncomeStatementRespVO;

import java.time.LocalDate;

/**
 * ERP 单体财务报表 Service 接口（P0-4）
 *
 * <p>提供单体层面的三大财务报表：资产负债表、利润表、现金流量表。
 *
 * @author 芋道源码
 */
public interface ErpFinancialStatementService {

    /**
     * 生成资产负债表（资产 = 负债 + 权益）
     *
     * @param asOfDate 报表日期
     * @return 资产负债表
     */
    ErpBalanceSheetRespVO generateBalanceSheet(LocalDate asOfDate);

    /**
     * 生成利润表（收入 - 成本 - 费用 = 利润）
     *
     * @param startDate 起始日期
     * @param endDate   结束日期
     * @return 利润表
     */
    ErpIncomeStatementRespVO generateIncomeStatement(LocalDate startDate, LocalDate endDate);

    /**
     * 生成现金流量表（基于银行流水，无流水时基于凭证中银行科目变动）
     *
     * @param startDate 起始日期
     * @param endDate   结束日期
     * @return 现金流量表
     */
    ErpCashFlowStatementRespVO generateCashFlowStatement(LocalDate startDate, LocalDate endDate);

}