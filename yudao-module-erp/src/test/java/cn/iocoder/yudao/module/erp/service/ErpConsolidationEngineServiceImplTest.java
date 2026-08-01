package cn.iocoder.yudao.module.erp.service;

import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpConsolidationEntryDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpConsolidationScopeDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpConsolidationWorksheetDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpConsolidationEntryMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpConsolidationWorksheetMapper;
import cn.iocoder.yudao.module.erp.enums.finance.ErpConsolidationEliminationTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpWorksheetStatusEnum;
import cn.iocoder.yudao.module.erp.service.finance.ErpConsolidationEngineServiceImpl;
import cn.iocoder.yudao.module.erp.service.finance.ErpConsolidationScopeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link ErpConsolidationEngineServiceImpl} 的单元测试
 *
 * <p>覆盖投资权益抵消分录生成（持股比例计算、少数股东权益）、内部应收应付抵消等。
 *
 * @author 芋道源码
 */
@ExtendWith(MockitoExtension.class)
public class ErpConsolidationEngineServiceImplTest {

    @Mock
    private ErpConsolidationScopeService consolidationScopeService;
    @Mock
    private ErpConsolidationEntryMapper consolidationEntryMapper;
    @Mock
    private ErpConsolidationWorksheetMapper consolidationWorksheetMapper;

    @InjectMocks
    private ErpConsolidationEngineServiceImpl consolidationEngineService;

    private static final Long PARENT_ID = 1L;
    private static final Long SUB_ID = 2L;
    private static final String PERIOD = "202607";

    /**
     * 构建合并范围
     */
    private ErpConsolidationScopeDO buildScope(BigDecimal holdingRatio) {
        return ErpConsolidationScopeDO.builder()
                .id(1L).parentCompanyId(PARENT_ID).subsidiaryCompanyId(SUB_ID)
                .holdingRatio(holdingRatio).status(0).build();
    }

    /**
     * 构建已审核的抵消分录
     */
    private ErpConsolidationEntryDO buildEntry(Integer eliminationType, BigDecimal amount) {
        return ErpConsolidationEntryDO.builder()
                .id(1L).periodCode(PERIOD).eliminationType(eliminationType)
                .eliminationAmount(amount).status(20).build();
    }

    // ==================== 投资权益抵消 ====================

    @Test
    public void testInvestmentEquityElimination_80PercentHolding() {
        // 准备：母公司持股 80%，投资权益分录金额 100000
        ErpConsolidationScopeDO scope = buildScope(new BigDecimal("0.80"));
        when(consolidationScopeService.getEnabledScopeListByParent(PARENT_ID))
                .thenReturn(Collections.singletonList(scope));
        when(consolidationEntryMapper.selectListByPeriodCode(PERIOD))
                .thenReturn(Collections.singletonList(
                        buildEntry(ErpConsolidationEliminationTypeEnum.INVESTMENT_EQUITY.getType(),
                                new BigDecimal("100000"))));

        // 调用
        List<ErpConsolidationWorksheetDO> result = consolidationEngineService
                .generateInvestmentEquityElimination(PARENT_ID, SUB_ID, PERIOD);

        // 断言：生成 3 条工作底稿（借权益 80000 + 贷投资 80000 + 少数股东权益 20000）
        assertEquals(3, result.size());
        // 母公司份额 = 100000 * 0.8 = 80000
        assertEquals(0, new BigDecimal("80000").compareTo(result.get(0).getEliminationAmount()));
        assertEquals(0, new BigDecimal("80000").compareTo(result.get(1).getEliminationAmount()));
        // 少数股东权益 = 100000 - 80000 = 20000
        assertEquals(0, new BigDecimal("20000").compareTo(result.get(2).getEliminationAmount()));
        // 验证插入
        verify(consolidationWorksheetMapper, times(3)).insert(any(ErpConsolidationWorksheetDO.class));
        verify(consolidationWorksheetMapper).deleteByPeriodAndParentAndSubsidiary(PERIOD, PARENT_ID, SUB_ID);
    }

    @Test
    public void testInvestmentEquityElimination_100PercentHolding_noMinority() {
        // 准备：母公司持股 100%，不应产生少数股东权益
        ErpConsolidationScopeDO scope = buildScope(new BigDecimal("1.00"));
        when(consolidationScopeService.getEnabledScopeListByParent(PARENT_ID))
                .thenReturn(Collections.singletonList(scope));
        when(consolidationEntryMapper.selectListByPeriodCode(PERIOD))
                .thenReturn(Collections.singletonList(
                        buildEntry(ErpConsolidationEliminationTypeEnum.INVESTMENT_EQUITY.getType(),
                                new BigDecimal("50000"))));

        // 调用
        List<ErpConsolidationWorksheetDO> result = consolidationEngineService
                .generateInvestmentEquityElimination(PARENT_ID, SUB_ID, PERIOD);

        // 断言：仅 2 条工作底稿（借 + 贷），无少数股东权益
        assertEquals(2, result.size());
        // 全部为 50000
        assertEquals(0, new BigDecimal("50000").compareTo(result.get(0).getEliminationAmount()));
        assertEquals(0, new BigDecimal("50000").compareTo(result.get(1).getEliminationAmount()));
    }

    @Test
    public void testInvestmentEquityElimination_zeroEntries() {
        // 准备：无已审核抵消分录
        ErpConsolidationScopeDO scope = buildScope(new BigDecimal("0.65"));
        when(consolidationScopeService.getEnabledScopeListByParent(PARENT_ID))
                .thenReturn(Collections.singletonList(scope));
        when(consolidationEntryMapper.selectListByPeriodCode(PERIOD))
                .thenReturn(Collections.emptyList());

        // 调用
        List<ErpConsolidationWorksheetDO> result = consolidationEngineService
                .generateInvestmentEquityElimination(PARENT_ID, SUB_ID, PERIOD);

        // 断言：仅借 + 贷 2 条（金额均为 0），无少数股东权益（0 被跳过）
        assertEquals(2, result.size());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.get(0).getEliminationAmount()));
    }

    @Test
    public void testInvestmentEquityElimination_scopeNotFound_throwsException() {
        // 合并范围不存在
        when(consolidationScopeService.getEnabledScopeListByParent(PARENT_ID))
                .thenReturn(Collections.emptyList());

        assertThrows(Exception.class, () ->
                consolidationEngineService.generateInvestmentEquityElimination(PARENT_ID, SUB_ID, PERIOD));
    }

    @Test
    public void testInvestmentEquityElimination_multipleEntries_aggregated() {
        // 准备：多条已审核分录，应聚合后计算
        ErpConsolidationScopeDO scope = buildScope(new BigDecimal("0.60"));
        when(consolidationScopeService.getEnabledScopeListByParent(PARENT_ID))
                .thenReturn(Collections.singletonList(scope));
        when(consolidationEntryMapper.selectListByPeriodCode(PERIOD))
                .thenReturn(Arrays.asList(
                        buildEntry(ErpConsolidationEliminationTypeEnum.INVESTMENT_EQUITY.getType(),
                                new BigDecimal("60000")),
                        buildEntry(ErpConsolidationEliminationTypeEnum.INVESTMENT_EQUITY.getType(),
                                new BigDecimal("40000")),
                        // 非投资权益类型，应被过滤
                        buildEntry(ErpConsolidationEliminationTypeEnum.INTERCOMPANY_AR_AP.getType(),
                                new BigDecimal("30000")),
                        // 未审核的分录，应被过滤
                        ErpConsolidationEntryDO.builder().periodCode(PERIOD)
                                .eliminationType(ErpConsolidationEliminationTypeEnum.INVESTMENT_EQUITY.getType())
                                .eliminationAmount(new BigDecimal("10000")).status(10).build()
                ));

        // 调用
        List<ErpConsolidationWorksheetDO> result = consolidationEngineService
                .generateInvestmentEquityElimination(PARENT_ID, SUB_ID, PERIOD);

        // 断言：聚合金额 = 60000 + 40000 = 100000
        // 母公司份额 = 100000 * 0.6 = 60000
        // 少数股东权益 = 100000 - 60000 = 40000
        assertEquals(3, result.size());
        assertEquals(0, new BigDecimal("60000").compareTo(result.get(0).getEliminationAmount()));
        assertEquals(0, new BigDecimal("40000").compareTo(result.get(2).getEliminationAmount()));
    }

    // ==================== 内部应收应付抵消 ====================

    @Test
    public void testIntercompanyArApElimination_normal() {
        ErpConsolidationScopeDO scope = buildScope(new BigDecimal("0.80"));
        when(consolidationScopeService.getEnabledScopeListByParent(PARENT_ID))
                .thenReturn(Collections.singletonList(scope));
        when(consolidationEntryMapper.selectListByPeriodCode(PERIOD))
                .thenReturn(Collections.singletonList(
                        buildEntry(ErpConsolidationEliminationTypeEnum.INTERCOMPANY_AR_AP.getType(),
                                new BigDecimal("30000"))));

        // 调用
        List<ErpConsolidationWorksheetDO> result = consolidationEngineService
                .generateIntercompanyArApElimination(PARENT_ID, SUB_ID, PERIOD);

        // 断言：借应付 + 贷应收，各 30000
        assertEquals(2, result.size());
        assertEquals(0, new BigDecimal("30000").compareTo(result.get(0).getEliminationAmount()));
        assertEquals(0, new BigDecimal("30000").compareTo(result.get(1).getEliminationAmount()));
    }

    // ==================== 内部销售成本抵消 ====================

    @Test
    public void testIntercompanySaleCogsElimination_unrealizedProfit() {
        ErpConsolidationScopeDO scope = buildScope(new BigDecimal("0.80"));
        when(consolidationScopeService.getEnabledScopeListByParent(PARENT_ID))
                .thenReturn(Collections.singletonList(scope));
        when(consolidationEntryMapper.selectListByPeriodCode(PERIOD))
                .thenReturn(Collections.singletonList(
                        buildEntry(ErpConsolidationEliminationTypeEnum.INTERCOMPANY_SALE_COGS.getType(),
                                new BigDecimal("100000"))));

        // 调用
        List<ErpConsolidationWorksheetDO> result = consolidationEngineService
                .generateIntercompanySaleCogsElimination(PARENT_ID, SUB_ID, PERIOD);

        // 断言：借销售收入 100000 + 贷销售成本 80000 + 贷存货（未实现利润）20000
        // 未实现利润 = 100000 * 0.20（默认毛利率）= 20000
        assertEquals(3, result.size());
        assertEquals(0, new BigDecimal("100000").compareTo(result.get(0).getEliminationAmount()));
        assertEquals(0, new BigDecimal("80000").compareTo(result.get(1).getEliminationAmount()));
        assertEquals(0, new BigDecimal("20000").compareTo(result.get(2).getEliminationAmount()));
    }

    // ==================== 批量生成 ====================

    @Test
    public void testGenerateAllEliminations_multipleScopes() {
        // 准备：两个合并范围
        ErpConsolidationScopeDO scope1 = buildScope(new BigDecimal("0.80"));
        ErpConsolidationScopeDO scope2 = ErpConsolidationScopeDO.builder()
                .id(2L).parentCompanyId(3L).subsidiaryCompanyId(4L)
                .holdingRatio(new BigDecimal("1.00")).status(0).build();
        when(consolidationScopeService.getEnabledScopeList())
                .thenReturn(Arrays.asList(scope1, scope2));
        // 按 parent 分别桩出对应的合并范围：validateScopeExists 会用 parentId 查列表后再匹配 subId，
        // 若两个 parent 都返回 scope1，则 scope2(parent=3, sub=4) 会因匹配不到 subId 抛「合并范围不存在」
        when(consolidationScopeService.getEnabledScopeListByParent(PARENT_ID))
                .thenReturn(Collections.singletonList(scope1));
        when(consolidationScopeService.getEnabledScopeListByParent(3L))
                .thenReturn(Collections.singletonList(scope2));
        when(consolidationEntryMapper.selectListByPeriodCode(anyString()))
                .thenReturn(Collections.emptyList());

        // 调用
        List<ErpConsolidationWorksheetDO> result = consolidationEngineService
                .generateAllEliminations(PERIOD);

        // 断言：每个 scope 生成 4 类抵消，每类至少 2 条（借+贷）
        assertNotNull(result);
        assertTrue(result.size() >= 8);
    }

    @Test
    public void testGenerateAllEliminations_noScope_throwsException() {
        when(consolidationScopeService.getEnabledScopeList()).thenReturn(Collections.emptyList());
        assertThrows(Exception.class, () ->
                consolidationEngineService.generateAllEliminations(PERIOD));
    }

}
