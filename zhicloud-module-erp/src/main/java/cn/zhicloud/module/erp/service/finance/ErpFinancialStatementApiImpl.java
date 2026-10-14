package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.module.erp.api.ErpFinancialStatementApi;
import cn.zhicloud.module.erp.controller.admin.finance.vo.financialstatement.ErpBalanceSheetRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.financialstatement.ErpCashFlowStatementRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.financialstatement.ErpIncomeStatementRespVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * {@link ErpFinancialStatementApi} 的 ERP 侧实现
 *
 * <p>FMS 账套配置层按账套视角出三大报表的跨模块只读入口；
 * accountSetId 的账簿解析在 {@link ErpFinancialStatementServiceImpl} 内完成
 * （「账套 → 账簿 → 凭证」链路，与凭证流水 V193 同口径）。
 *
 * @author 智云
 */
@Component
public class ErpFinancialStatementApiImpl implements ErpFinancialStatementApi {

    @Resource
    private ErpFinancialStatementService financialStatementService;

    @Override
    public ErpBalanceSheetRespVO generateBalanceSheet(Long accountSetId, LocalDate asOfDate) {
        return financialStatementService.generateBalanceSheet(accountSetId, asOfDate);
    }

    @Override
    public ErpIncomeStatementRespVO generateIncomeStatement(Long accountSetId, LocalDate startDate, LocalDate endDate) {
        return financialStatementService.generateIncomeStatement(accountSetId, startDate, endDate);
    }

    @Override
    public ErpCashFlowStatementRespVO generateCashFlowStatement(Long accountSetId, LocalDate startDate, LocalDate endDate) {
        return financialStatementService.generateCashFlowStatement(accountSetId, startDate, endDate);
    }

}
