package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.module.erp.controller.admin.finance.vo.financialstatement.ErpBalanceSheetRespVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlAccountDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlVoucherDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlVoucherEntryDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpCashFlowMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlAccountMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlVoucherEntryMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlVoucherMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * {@link ErpFinancialStatementServiceImpl} 资产负债表平衡断言测试（§12.146）
 *
 * <p>锁死修复的核心不变式：损益结转有意不生成凭证（§12.145），未结转的累计净利润
 * 作为「本年利润（未分配）」并入权益后，恒等式 资产 = 负债 + 权益 必须在任意
 * 已审核凭证组合下成立（盈利、亏损、损益互抵三种情形）。
 *
 * <p>accountSetId 一律传 null（租户全量口径）——账套视角过滤的链路由 FMS 侧
 * E2E（zc_fms_report_e2e.py）覆盖，此处只锁单体报表不变式。
 *
 * @author 智云
 */
@ExtendWith(MockitoExtension.class)
public class ErpFinancialStatementBalanceSheetTest {

    @Mock
    private ErpGlVoucherMapper voucherMapper;
    @Mock
    private ErpGlVoucherEntryMapper voucherEntryMapper;
    @Mock
    private ErpGlAccountMapper accountMapper;
    @Mock
    private ErpCashFlowMapper cashFlowMapper;

    @InjectMocks
    private ErpFinancialStatementServiceImpl statementService;

    private static final LocalDate AS_OF = LocalDate.of(2026, 10, 31);

    private ErpGlAccountDO account(Long id, String code, Integer type) {
        return ErpGlAccountDO.builder().id(id).code(code).name("科目" + code).type(type).build();
    }

    private ErpGlVoucherEntryDO entry(Long accountId, String debit, String credit) {
        return ErpGlVoucherEntryDO.builder().accountId(accountId)
                .debitAmount(new BigDecimal(debit)).creditAmount(new BigDecimal(credit)).build();
    }

    private void stubVouchers(List<ErpGlVoucherEntryDO> entries) {
        when(voucherMapper.selectList(any())).thenReturn(List.of(ErpGlVoucherDO.builder().id(100L).build()));
        when(voucherEntryMapper.selectListByVoucherIds(anyList())).thenReturn(entries);
    }

    @Test
    public void testProfitScenarioBalanceHoldsViaEquityPlug() {
        // 借 1001 现金 1000 / 贷 6001 收入 1000 —— 只有资产和收入，无结转则原先不平
        stubVouchers(List.of(entry(1L, "1000", "0"), entry(4L, "0", "1000")));
        when(accountMapper.selectList(any())).thenReturn(List.of(
                account(1L, "1001", 10),   // 资产
                account(4L, "6001", 40))); // 收入
        ErpBalanceSheetRespVO vo = statementService.generateBalanceSheet(null, AS_OF);
        assertEquals(0, new BigDecimal("1000").compareTo(vo.getTotalAssets()));
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getTotalLiabilities()));
        // 未分配利润 1000 进权益 → 恒等式
        assertEquals(0, new BigDecimal("1000").compareTo(vo.getTotalEquity()));
        assertEquals(0, vo.getTotalAssets().compareTo(
                vo.getTotalLiabilities().add(vo.getTotalEquity())));
    }

    @Test
    public void testLossScenarioBalanceHolds() {
        // 借 6602 费用 300 / 贷 1001 现金 300 —— 资产 -300，权益（未分配利润）-300
        stubVouchers(List.of(entry(5L, "300", "0"), entry(1L, "0", "300")));
        when(accountMapper.selectList(any())).thenReturn(List.of(
                account(1L, "1001", 10),
                account(5L, "6602", 50)));  // 费用（6xxx 归费用非成本）
        ErpBalanceSheetRespVO vo = statementService.generateBalanceSheet(null, AS_OF);
        assertEquals(0, new BigDecimal("-300").compareTo(vo.getTotalAssets()));
        assertEquals(0, new BigDecimal("-300").compareTo(vo.getTotalEquity()));
        assertEquals(0, vo.getTotalAssets().compareTo(
                vo.getTotalLiabilities().add(vo.getTotalEquity())));
    }

    @Test
    public void testOffsetPnlNoEquityPlugLine() {
        // 收入 500 与费用 500 互抵：净利润 0 → 不追加权益行，恒等式仍成立
        stubVouchers(List.of(entry(4L, "0", "500"), entry(5L, "500", "0"),
                entry(1L, "0", "0")));
        when(accountMapper.selectList(any())).thenReturn(List.of(
                account(4L, "6001", 40),
                account(5L, "6602", 50),
                account(1L, "1001", 10)));
        ErpBalanceSheetRespVO vo = statementService.generateBalanceSheet(null, AS_OF);
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getTotalAssets()));
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getTotalEquity()));
        assertEquals(0, vo.getTotalAssets().compareTo(
                vo.getTotalLiabilities().add(vo.getTotalEquity())));
    }

}
