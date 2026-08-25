package cn.zhicloud.module.erp.service;

import cn.zhicloud.module.erp.dal.dataobject.finance.ErpCurrencyDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpExchangeRateDO;
import cn.zhicloud.module.erp.service.finance.ErpCurrencyService;
import cn.zhicloud.module.erp.service.finance.ErpExchangeRateService;
import cn.zhicloud.module.erp.service.finance.ErpMultiCurrencyServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link ErpMultiCurrencyServiceImpl} 的单元测试
 *
 * <p>覆盖汇率折算（本位币识别、有效汇率查询、外币金额折算）。
 *
 * @author 智云
 */
@ExtendWith(MockitoExtension.class)
public class ErpMultiCurrencyServiceImplTest {

    @Mock
    private ErpCurrencyService currencyService;
    @Mock
    private ErpExchangeRateService exchangeRateService;

    @InjectMocks
    private ErpMultiCurrencyServiceImpl multiCurrencyService;

    private static final Long CNY_ID = 1L;
    private static final Long USD_ID = 2L;
    private static final LocalDate DATE = LocalDate.of(2026, 7, 15);

    /**
     * 构建本位币 CNY
     */
    private ErpCurrencyDO buildBaseCurrency() {
        return ErpCurrencyDO.builder().id(CNY_ID).code("CNY").name("人民币")
                .isBase(true).enabled(0).build();
    }

    /**
     * 构建外币 USD
     */
    private ErpCurrencyDO buildUsdCurrency() {
        return ErpCurrencyDO.builder().id(USD_ID).code("USD").name("美元")
                .isBase(false).enabled(0).build();
    }

    // ==================== 本位币识别 ====================

    @Test
    public void testGetBaseCurrency_found() {
        List<ErpCurrencyDO> currencies = Arrays.asList(buildUsdCurrency(), buildBaseCurrency());
        when(currencyService.getEnabledCurrencyList()).thenReturn(currencies);

        ErpCurrencyDO result = multiCurrencyService.getBaseCurrency();

        assertNotNull(result);
        assertEquals(CNY_ID, result.getId());
        assertTrue(result.getIsBase());
    }

    @Test
    public void testGetBaseCurrency_notConfigured() {
        // 无本位币配置
        when(currencyService.getEnabledCurrencyList())
                .thenReturn(Collections.singletonList(buildUsdCurrency()));

        ErpCurrencyDO result = multiCurrencyService.getBaseCurrency();

        assertNull(result);
    }

    @Test
    public void testGetBaseCurrency_emptyList() {
        when(currencyService.getEnabledCurrencyList()).thenReturn(Collections.emptyList());

        ErpCurrencyDO result = multiCurrencyService.getBaseCurrency();

        assertNull(result);
    }

    @Test
    public void testGetBaseCurrencyId_found() {
        when(currencyService.getEnabledCurrencyList())
                .thenReturn(Arrays.asList(buildBaseCurrency(), buildUsdCurrency()));

        Long baseId = multiCurrencyService.getBaseCurrencyId();

        assertEquals(CNY_ID, baseId);
    }

    // ==================== 有效汇率查询 ====================

    @Test
    public void testGetEffectiveRate_baseCurrency_returnsOne() {
        // 查询本位币到本位币，汇率 = 1
        when(currencyService.getEnabledCurrencyList())
                .thenReturn(Collections.singletonList(buildBaseCurrency()));

        BigDecimal rate = multiCurrencyService.getEffectiveRate(CNY_ID, DATE);

        assertEquals(0, BigDecimal.ONE.compareTo(rate));
    }

    @Test
    public void testGetEffectiveRate_foreignCurrency() {
        // USD -> CNY，汇率 7.25
        when(currencyService.getEnabledCurrencyList())
                .thenReturn(Arrays.asList(buildBaseCurrency(), buildUsdCurrency()));
        ErpExchangeRateDO rateDO = ErpExchangeRateDO.builder()
                .id(1L).fromCurrencyId(USD_ID).toCurrencyId(CNY_ID)
                .rate(new BigDecimal("7.25")).build();
        when(exchangeRateService.getLatestRate(USD_ID, CNY_ID, DATE)).thenReturn(rateDO);

        BigDecimal rate = multiCurrencyService.getEffectiveRate(USD_ID, DATE);

        assertEquals(0, new BigDecimal("7.25").compareTo(rate));
    }

    @Test
    public void testGetEffectiveRate_rateNotFound_returnsNull() {
        when(currencyService.getEnabledCurrencyList())
                .thenReturn(Arrays.asList(buildBaseCurrency(), buildUsdCurrency()));
        when(exchangeRateService.getLatestRate(USD_ID, CNY_ID, DATE)).thenReturn(null);

        BigDecimal rate = multiCurrencyService.getEffectiveRate(USD_ID, DATE);

        assertNull(rate);
    }

    @Test
    public void testGetEffectiveRate_nullCurrencyId_returnsOne() {
        // null 币种编号视为本位币
        BigDecimal rate = multiCurrencyService.getEffectiveRate(null, DATE);

        assertEquals(0, BigDecimal.ONE.compareTo(rate));
    }

    @Test
    public void testGetEffectiveRate_noBaseCurrency_returnsOne() {
        // 未配置本位币，按汇率 1 处理
        when(currencyService.getEnabledCurrencyList()).thenReturn(Collections.emptyList());

        BigDecimal rate = multiCurrencyService.getEffectiveRate(USD_ID, DATE);

        assertEquals(0, BigDecimal.ONE.compareTo(rate));
    }

    // ==================== 金额折算 ====================

    @Test
    public void testConvertToBaseCurrency_normal() {
        // 1000 USD * 7.25 = 7250 CNY
        when(currencyService.getEnabledCurrencyList())
                .thenReturn(Arrays.asList(buildBaseCurrency(), buildUsdCurrency()));
        ErpExchangeRateDO rateDO = ErpExchangeRateDO.builder()
                .id(1L).fromCurrencyId(USD_ID).toCurrencyId(CNY_ID)
                .rate(new BigDecimal("7.25")).build();
        when(exchangeRateService.getLatestRate(eq(USD_ID), eq(CNY_ID), any(LocalDate.class)))
                .thenReturn(rateDO);

        BigDecimal result = multiCurrencyService.convertToBaseCurrency(USD_ID, new BigDecimal("1000"), DATE);

        assertNotNull(result);
        // 1000 * 7.25 = 7250.0000（MoneyUtils.priceMultiply 默认保留 4 位小数）
        assertEquals(0, new BigDecimal("7250.0000").compareTo(result));
    }

    @Test
    public void testConvertToBaseCurrency_baseCurrency() {
        // 本位币折算，汇率 = 1
        when(currencyService.getEnabledCurrencyList())
                .thenReturn(Collections.singletonList(buildBaseCurrency()));

        BigDecimal result = multiCurrencyService.convertToBaseCurrency(CNY_ID, new BigDecimal("500"), DATE);

        assertNotNull(result);
        assertEquals(0, new BigDecimal("500.0000").compareTo(result));
    }

    @Test
    public void testConvertToBaseCurrency_nullAmount() {
        BigDecimal result = multiCurrencyService.convertToBaseCurrency(USD_ID, null, DATE);

        assertNull(result);
    }

    @Test
    public void testConvertToBaseCurrency_zeroAmount() {
        when(currencyService.getEnabledCurrencyList())
                .thenReturn(Collections.singletonList(buildBaseCurrency()));

        BigDecimal result = multiCurrencyService.convertToBaseCurrency(CNY_ID, BigDecimal.ZERO, DATE);

        assertNotNull(result);
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    public void testConvertToBaseCurrency_rateNotFound_returnsNull() {
        when(currencyService.getEnabledCurrencyList())
                .thenReturn(Arrays.asList(buildBaseCurrency(), buildUsdCurrency()));
        when(exchangeRateService.getLatestRate(eq(USD_ID), eq(CNY_ID), any(LocalDate.class)))
                .thenReturn(null);

        BigDecimal result = multiCurrencyService.convertToBaseCurrency(USD_ID, new BigDecimal("1000"), DATE);

        assertNull(result);
    }

}
