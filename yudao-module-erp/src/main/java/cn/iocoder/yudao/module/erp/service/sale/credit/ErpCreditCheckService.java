package cn.iocoder.yudao.module.erp.service.sale.credit;

import java.math.BigDecimal;

/**
 * ERP 客户信用检查 Service 接口
 *
 * <p>提供客户信用额度的校验、冻结与释放能力，供销售订单创建/审核调用。
 *
 * @author 芋道源码
 */
public interface ErpCreditCheckService {

    /**
     * 检查客户信用额度是否充足
     *
     * <p>校验规则：amount + 客户已用额度 ≤ 信用额度。若客户未配置信用额度记录，视为不校验。
     * 若客户状态为冻结，直接拒绝。
     *
     * @param customerId 客户编号
     * @param amount 本次占用金额
     */
    void checkCredit(Long customerId, BigDecimal amount);

    /**
     * 冻结信用额度（已用额度 += amount，可用额度 -= amount）
     *
     * @param customerId 客户编号
     * @param amount 冻结金额
     */
    void freezeCredit(Long customerId, BigDecimal amount);

    /**
     * 释放冻结额度（已用额度 -= amount，不低于 0）
     *
     * @param customerId 客户编号
     * @param amount 释放金额
     */
    void releaseCredit(Long customerId, BigDecimal amount);

}
