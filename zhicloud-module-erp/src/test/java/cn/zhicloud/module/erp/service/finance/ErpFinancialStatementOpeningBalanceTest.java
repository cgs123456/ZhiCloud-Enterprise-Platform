package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.module.erp.controller.admin.finance.vo.financialstatement.ErpBalanceSheetRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.financialstatement.ErpFinancialStatementItemVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpAccountBookDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlAccountDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlInitialBalanceDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlVoucherDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlVoucherEntryDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpAccountBookMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpCashFlowMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlAccountMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlInitialBalanceMapper;
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
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link ErpFinancialStatementServiceImpl} 期初接线测试（D2 / ADR 268）
 *
 * <p>锁定二轨按作用域接线：账套口径消费 {@code erp_gl_initial_balance}（opening_amount 按科目
 * 自然方向，经方向系数折算为借正贷负后播种）；租户口径消费 {@code erp_gl_account.opening_debit
 * /opening_credit}（已是借贷两列）；同一调用只用一轨、互不参与。P&L 科目期初随播种进入
 * 「本年利润（未分配）」恒等式补平。
 *
 * @author 智云
 */
@ExtendWith(MockitoExtension.class)
public class ErpFinancialStatementOpeningBalanceTest {

    @Mock
    private ErpGlVoucherMapper voucherMapper;
    @Mock
    private ErpGlVoucherEntryMapper voucherEntryMapper;
    @Mock
    private ErpGlAccountMapper accountMapper;
    @Mock
    private ErpCashFlowMapper cashFlowMapper;
    @Mock
    private ErpAccountBookMapper accountBookMapper;
    @Mock
    private ErpGlInitialBalanceMapper initialBalanceMapper;

    @InjectMocks
    private ErpFinancialStatementServiceImpl statementService;

    private static final LocalDate AS_OF = LocalDate.of(2026, 10, 31);
    private static final Long SET_ID = 77L;
    private static final Long BOOK_ID = 900L;

    private static final int TYPE_ASSET = 10;
    private static final int TYPE_LIABILITY = 20;
    private static final int TYPE_EQUITY = 30;
    private static final int TYPE_REVENUE = 40;

    private ErpGlAccountDO account(Long id, String code, Integer type, Integer direction) {
        return ErpGlAccountDO.builder().id(id).code(code).name("科目" + code)
                .type(type).balanceDirection(direction).build();
    }

    private ErpGlVoucherEntryDO entry(Long accountId, String debit, String credit) {
        return ErpGlVoucherEntryDO.builder().accountId(accountId)
                .debitAmount(new BigDecimal(debit)).creditAmount(new BigDecimal(credit)).build();
    }

    private ErpGlInitialBalanceDO initialRow(Long accountId, String openingAmount) {
        return ErpGlInitialBalanceDO.builder().accountSetId(SET_ID).accountId(accountId)
                .openingAmount(new BigDecimal(openingAmount)).build();
    }

    private boolean hasItem(List<ErpFinancialStatementItemVO> items, String code, String amount) {
        return items.stream().anyMatch(i -> Objects.equals(code, i.getItemCode())
                && i.getAmount() != null && new BigDecimal(amount).compareTo(i.getAmount()) == 0);
    }

    @Test
    public void testTenantScopeConsumesAccountOpeningColumns() {
        // 租户口径＝科目表期初两列（借方 3000 / 贷方 1200 / 贷方 1000 / 贷方 800）
        when(accountMapper.selectList(any())).thenReturn(List.of(
                ErpGlAccountDO.builder().id(1L).code("1001").name("库存现金").type(TYPE_ASSET)
                        .balanceDirection(10).openingDebit(new BigDecimal("3000")).openingCredit(BigDecimal.ZERO).build(),
                ErpGlAccountDO.builder().id(2L).code("2001").name("短期借款").type(TYPE_LIABILITY)
                        .balanceDirection(20).openingDebit(BigDecimal.ZERO).openingCredit(new BigDecimal("1200")).build(),
                ErpGlAccountDO.builder().id(3L).code("3001").name("实收资本").type(TYPE_EQUITY)
                        .balanceDirection(20).openingDebit(BigDecimal.ZERO).openingCredit(new BigDecimal("1000")).build(),
                ErpGlAccountDO.builder().id(4L).code("6001").name("主营业务收入").type(TYPE_REVENUE)
                        .balanceDirection(20).openingDebit(BigDecimal.ZERO).openingCredit(new BigDecimal("800")).build()));
        // 已审核凭证：借 1001 现金 500 / 贷 6001 收入 500
        when(voucherMapper.selectList(any())).thenReturn(List.of(ErpGlVoucherDO.builder().id(100L).build()));
        when(voucherEntryMapper.selectListByVoucherIds(anyList())).thenReturn(List.of(
                entry(1L, "500", "0"), entry(4L, "0", "500")));

        ErpBalanceSheetRespVO vo = statementService.generateBalanceSheet(null, AS_OF);

        // 期末 = 期初 + 凭证：资产 3000+500；负债 1200；权益 1000 + 本年利润 (800+500)
        assertEquals(0, new BigDecimal("3500").compareTo(vo.getTotalAssets()));
        assertEquals(0, new BigDecimal("1200").compareTo(vo.getTotalLiabilities()));
        assertEquals(0, new BigDecimal("2300").compareTo(vo.getTotalEquity()));
        assertTrue(hasItem(vo.getAssetItems(), "1001", "3500"));
        assertTrue(hasItem(vo.getEquityItems(), null, "1300"));
        assertEquals(0, vo.getTotalAssets().compareTo(
                vo.getTotalLiabilities().add(vo.getTotalEquity())));
        // 租户口径不得触发 Track B（期初余额表）
        verifyNoInteractions(initialBalanceMapper);
    }

    @Test
    public void testTenantScopeOpeningOnlyAccountAppears() {
        // 无任何凭证；仅期初：资产 700 借 / 权益 700 贷
        when(accountMapper.selectList(any())).thenReturn(List.of(
                ErpGlAccountDO.builder().id(1L).code("1001").name("库存现金").type(TYPE_ASSET)
                        .balanceDirection(10).openingDebit(new BigDecimal("700")).build(),
                ErpGlAccountDO.builder().id(2L).code("3001").name("实收资本").type(TYPE_EQUITY)
                        .balanceDirection(20).openingCredit(new BigDecimal("700")).build()));
        when(voucherMapper.selectList(any())).thenReturn(List.of());

        ErpBalanceSheetRespVO vo = statementService.generateBalanceSheet(null, AS_OF);

        assertEquals(0, new BigDecimal("700").compareTo(vo.getTotalAssets()));
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getTotalLiabilities()));
        assertEquals(0, new BigDecimal("700").compareTo(vo.getTotalEquity()));
        assertTrue(hasItem(vo.getAssetItems(), "1001", "700"));
        assertTrue(hasItem(vo.getEquityItems(), "3001", "700"));
    }

    @Test
    public void testAccountSetScopeConsumesInitialBalanceTrack() {
        when(accountBookMapper.selectListByAccountSetId(SET_ID))
                .thenReturn(List.of(ErpAccountBookDO.builder().id(BOOK_ID).build()));
        when(voucherMapper.selectList(any())).thenReturn(List.of(ErpGlVoucherDO.builder().id(100L).build()));
        when(voucherEntryMapper.selectListByVoucherIds(anyList())).thenReturn(List.of(
                entry(1L, "400", "0"), entry(2L, "0", "400")));
        when(accountMapper.selectList(any())).thenReturn(List.of(
                // 冲抵混淆项：科目 1 的租户口径期初不得进入账套口径（openingDebit=999999）
                ErpGlAccountDO.builder().id(1L).code("1001").name("库存现金").type(TYPE_ASSET)
                        .balanceDirection(10).openingDebit(new BigDecimal("999999")).build(),
                account(2L, "2001", TYPE_LIABILITY, 20),
                account(3L, "3001", TYPE_EQUITY, 20),
                account(4L, "1101", TYPE_ASSET, 30),
                account(5L, "6001", TYPE_REVENUE, 20)));
        when(initialBalanceMapper.selectListByAccountSetId(SET_ID)).thenReturn(List.of(
                initialRow(1L, "5000"),   // 借向：+5000
                initialRow(4L, "250"),    // 双向：按借方口径 +250
                initialRow(2L, "3000"),   // 贷向：−3000 → 贷方 3000
                initialRow(3L, "1250"),   // 贷向：−1250 → 贷方 1250
                initialRow(5L, "1000"))); // 收入（贷向）：−1000 → 贷方 1000 → 本年利润

        ErpBalanceSheetRespVO vo = statementService.generateBalanceSheet(SET_ID, AS_OF);

        // 期末 = 期初锚点 + 账簿内凭证（借 1001 400 / 贷 2001 400）
        assertEquals(0, new BigDecimal("5650").compareTo(vo.getTotalAssets()));      // 5000+250+400
        assertEquals(0, new BigDecimal("3400").compareTo(vo.getTotalLiabilities())); // 3000+400
        assertEquals(0, new BigDecimal("2250").compareTo(vo.getTotalEquity()));      // 1250+1000
        assertTrue(hasItem(vo.getEquityItems(), null, "1000"));
        assertEquals(0, vo.getTotalAssets().compareTo(
                vo.getTotalLiabilities().add(vo.getTotalEquity())));
    }

    @Test
    public void testAccountSetScopeSkipsZeroAndUnknownRows() {
        when(accountBookMapper.selectListByAccountSetId(SET_ID))
                .thenReturn(List.of(ErpAccountBookDO.builder().id(BOOK_ID).build()));
        when(voucherMapper.selectList(any())).thenReturn(List.of());
        when(accountMapper.selectList(any())).thenReturn(List.of(account(1L, "1001", TYPE_ASSET, 10)));
        when(initialBalanceMapper.selectListByAccountSetId(SET_ID)).thenReturn(List.of(
                initialRow(1L, "0"),
                initialRow(99L, "500")));

        ErpBalanceSheetRespVO vo = statementService.generateBalanceSheet(SET_ID, AS_OF);

        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getTotalAssets()));
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getTotalLiabilities()));
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getTotalEquity()));
        assertTrue(vo.getAssetItems().isEmpty());
    }

    @Test
    public void testAccountSetScopeWithoutInitialRowsKeepsVoucherOnly() {
        when(accountBookMapper.selectListByAccountSetId(SET_ID))
                .thenReturn(List.of(ErpAccountBookDO.builder().id(BOOK_ID).build()));
        when(voucherMapper.selectList(any())).thenReturn(List.of(ErpGlVoucherDO.builder().id(100L).build()));
        when(voucherEntryMapper.selectListByVoucherIds(anyList())).thenReturn(List.of(
                entry(1L, "700", "0"), entry(5L, "0", "700")));
        when(accountMapper.selectList(any())).thenReturn(List.of(
                account(1L, "1001", TYPE_ASSET, 10),
                account(5L, "6001", TYPE_REVENUE, 20)));
        when(initialBalanceMapper.selectListByAccountSetId(SET_ID)).thenReturn(List.of());

        ErpBalanceSheetRespVO vo = statementService.generateBalanceSheet(SET_ID, AS_OF);

        assertEquals(0, new BigDecimal("700").compareTo(vo.getTotalAssets()));
        assertEquals(0, new BigDecimal("700").compareTo(vo.getTotalEquity()));
        assertEquals(0, vo.getTotalAssets().compareTo(
                vo.getTotalLiabilities().add(vo.getTotalEquity())));
    }

}
