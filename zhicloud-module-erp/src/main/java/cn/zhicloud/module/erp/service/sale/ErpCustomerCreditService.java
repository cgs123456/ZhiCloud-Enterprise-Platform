package cn.zhicloud.module.erp.service.sale;

import java.math.BigDecimal;

/**
 * ERP 客户信用 Service 接口（P0-2 客户信用控制）
 *
 * <p>提供客户信用额度的校验、锁定与释放能力，供销售订单审核/反审核调用。
 *
 * @author 智云
 */
public interface ErpCustomerCreditService {

    /**
     * 校验客户信用额度是否充足
     *
     * <p>校验规则：amount + 客户已用额度 ≤ 信用额度。若客户未设置信用额度（≤0），视为无限额度，不校验。
     *
     * @param customerId 客户编号
     * @param amount 本次占用金额
     */
    void validateCredit(Long customerId, BigDecimal amount);

    /**
     * 锁定信用额度（已用额度 += amount）
     *
     * @param customerId 客户编号
     * @param amount 锁定金额
     */
    void lockCredit(Long customerId, BigDecimal amount);

    /**
     * 释放信用额度（已用额度 -= amount，不低于 0）
     *
     * @param customerId 客户编号
     * @param amount 释放金额
     */
    void releaseCredit(Long customerId, BigDecimal amount);

}