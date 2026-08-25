package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.module.erp.dal.dataobject.finance.ErpCurrencyDO;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ERP 多币种转换 Service 接口
 *
 * <p>封装币种与汇率的统一入口，提供本位币识别、有效汇率查询、本位币金额折算等能力。
 * 供采购单、销售单、入库单、出库单等业务单据在创建/审核时调用。
 *
 * @author 智云
 */
public interface ErpMultiCurrencyService {

    /**
     * 获取当前租户的本位币
     *
     * @return 本位币 DO，若未配置返回 null
     */
    ErpCurrencyDO getBaseCurrency();

    /**
     * 获取当前租户的本位币 ID
     *
     * @return 本位币 ID，若未配置返回 null
     */
    Long getBaseCurrencyId();

    /**
     * 获取指定币种到本位币的有效汇率
     *
     * <p>若 fromCurrencyId 与本位币相同，返回 {@link BigDecimal#ONE}。
     *
     * @param fromCurrencyId 源币种编号
     * @param date           查询日期（null 默认今天）
     * @return 汇率（fromCurrency -> 本位币），找不到返回 null
     */
    BigDecimal getEffectiveRate(Long fromCurrencyId, LocalDate date);

    /**
     * 将指定币种金额折算为本位币金额
     *
     * @param fromCurrencyId 源币种编号（null 视为本位币）
     * @param amount         原币金额
     * @param date           查询日期（null 默认今天）
     * @return 本位币金额；若 amount 为 null 返回 null
     */
    BigDecimal convertToBaseCurrency(Long fromCurrencyId, BigDecimal amount, LocalDate date);

}
