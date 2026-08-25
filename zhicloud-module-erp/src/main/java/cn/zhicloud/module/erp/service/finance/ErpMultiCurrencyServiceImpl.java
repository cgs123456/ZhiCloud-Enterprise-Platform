package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.common.util.number.MoneyUtils;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpCurrencyDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpExchangeRateDO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * ERP 多币种转换 Service 实现类
 *
 * <p>基于 {@link ErpCurrencyService} 与 {@link ErpExchangeRateService} 组合，
 * 为业务单据提供"币种 + 汇率 + 本位币折算"一站式能力。
 *
 * @author 智云
 */
@Service
@Validated
public class ErpMultiCurrencyServiceImpl implements ErpMultiCurrencyService {

    @Resource
    private ErpCurrencyService currencyService;
    @Resource
    private ErpExchangeRateService exchangeRateService;

    @Override
    public ErpCurrencyDO getBaseCurrency() {
        return currencyService.getEnabledCurrencyList().stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsBase()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Long getBaseCurrencyId() {
        ErpCurrencyDO base = getBaseCurrency();
        return base == null ? null : base.getId();
    }

    @Override
    public BigDecimal getEffectiveRate(Long fromCurrencyId, LocalDate date) {
        if (fromCurrencyId == null) {
            return BigDecimal.ONE;
        }
        Long baseCurrencyId = getBaseCurrencyId();
        if (baseCurrencyId == null) {
            // 未配置本位币，按汇率 1 处理（兼容单币种场景）
            return BigDecimal.ONE;
        }
        if (Objects.equals(fromCurrencyId, baseCurrencyId)) {
            return BigDecimal.ONE;
        }
        LocalDate queryDate = date == null ? LocalDate.now() : date;
        ErpExchangeRateDO rate = exchangeRateService.getLatestRate(fromCurrencyId, baseCurrencyId, queryDate);
        return rate == null ? null : rate.getRate();
    }

    @Override
    public BigDecimal convertToBaseCurrency(Long fromCurrencyId, BigDecimal amount, LocalDate date) {
        if (amount == null) {
            return null;
        }
        BigDecimal rate = getEffectiveRate(fromCurrencyId, date);
        if (rate == null) {
            return null;
        }
        return MoneyUtils.priceMultiply(amount, rate);
    }

}
