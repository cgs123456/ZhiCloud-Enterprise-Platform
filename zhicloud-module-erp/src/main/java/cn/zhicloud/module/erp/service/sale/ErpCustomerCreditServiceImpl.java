package cn.zhicloud.module.erp.service.sale;

import cn.zhicloud.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.zhicloud.module.erp.dal.mysql.sale.ErpCustomerMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.CUSTOMER_CREDIT_NOT_ENOUGH;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;

/**
 * ERP 客户信用 Service 实现类（P0-2 客户信用控制）
 *
 * @author 智云
 */
@Service
@Validated
public class ErpCustomerCreditServiceImpl implements ErpCustomerCreditService {

    @Resource
    private ErpCustomerMapper customerMapper;

    @Override
    public void validateCredit(Long customerId, BigDecimal amount) {
        ErpCustomerDO customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        BigDecimal creditLimit = nullToZero(customer.getCreditLimit());
        // 未设置信用额度（<=0）则不校验，视为无限额度
        if (creditLimit.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal usedCredit = nullToZero(customer.getUsedCredit());
        BigDecimal available = creditLimit.subtract(usedCredit);
        if (amount.compareTo(available) > 0) {
            throw exception(CUSTOMER_CREDIT_NOT_ENOUGH, customer.getName(), available);
        }
    }

    @Override
    public void lockCredit(Long customerId, BigDecimal amount) {
        ErpCustomerDO customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        // 原子更新：used_credit += amount，仅当未超出 credit_limit 时成功；避免并发超额
        int updateCount = customerMapper.updateUsedCreditIncrement(customerId, nullToZero(amount));
        if (updateCount == 0) {
            BigDecimal creditLimit = nullToZero(customer.getCreditLimit());
            BigDecimal available = creditLimit.compareTo(BigDecimal.ZERO) > 0
                    ? creditLimit.subtract(nullToZero(customer.getUsedCredit())) : BigDecimal.ZERO;
            throw exception(CUSTOMER_CREDIT_NOT_ENOUGH, customer.getName(), available);
        }
    }

    @Override
    public void releaseCredit(Long customerId, BigDecimal amount) {
        ErpCustomerDO customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        // 原子更新：used_credit -= amount，且不低于 0
        customerMapper.updateUsedCreditDecrement(customerId, nullToZero(amount));
    }

    private static BigDecimal nullToZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

}