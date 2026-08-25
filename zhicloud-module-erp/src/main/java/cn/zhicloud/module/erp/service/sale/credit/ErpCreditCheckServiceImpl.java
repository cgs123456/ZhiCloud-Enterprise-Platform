package cn.zhicloud.module.erp.service.sale.credit;

import cn.zhicloud.module.erp.dal.dataobject.sale.credit.ErpCreditLimitDO;
import cn.zhicloud.module.erp.dal.mysql.sale.credit.ErpCreditLimitMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.CREDIT_LIMIT_FROZEN;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.CREDIT_LIMIT_NOT_ENOUGH;

/**
 * ERP 客户信用检查 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
@Slf4j
public class ErpCreditCheckServiceImpl implements ErpCreditCheckService {

    /**
     * 状态：30 冻结
     */
    private static final int STATUS_FROZEN = 30;
    /**
     * 状态：20 预警
     */
    private static final int STATUS_WARNING = 20;
    /**
     * 状态：10 正常
     */
    private static final int STATUS_NORMAL = 10;

    @Resource
    private ErpCreditLimitMapper creditLimitMapper;

    @Override
    public void checkCredit(Long customerId, BigDecimal amount) {
        if (customerId == null || amount == null) {
            return;
        }
        ErpCreditLimitDO creditLimit = creditLimitMapper.selectByCustomerId(customerId);
        // 未配置信用额度记录，视为不校验（兼容旧逻辑）
        if (creditLimit == null) {
            return;
        }
        // 冻结状态拒绝
        if (STATUS_FROZEN == creditLimit.getStatus()) {
            throw exception(CREDIT_LIMIT_FROZEN, customerId);
        }
        BigDecimal limit = nullToZero(creditLimit.getCreditLimit());
        // 未设置额度（<=0）视为无限额度
        if (limit.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal used = nullToZero(creditLimit.getUsedAmount());
        BigDecimal available = limit.subtract(used);
        if (amount.compareTo(available) > 0) {
            throw exception(CREDIT_LIMIT_NOT_ENOUGH, customerId, available);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void freezeCredit(Long customerId, BigDecimal amount) {
        if (customerId == null || amount == null) {
            return;
        }
        ErpCreditLimitDO creditLimit = creditLimitMapper.selectByCustomerId(customerId);
        if (creditLimit == null) {
            log.warn("[freezeCredit][客户({})未配置信用额度记录，跳过冻结]", customerId);
            return;
        }
        // 原子更新：used_amount += amount, available_amount -= amount，带额度校验；避免并发超额
        int updateCount = creditLimitMapper.updateUsedAmountIncrement(creditLimit.getId(), amount);
        if (updateCount == 0) {
            BigDecimal available = calcAvailable(creditLimit.getCreditLimit(), creditLimit.getUsedAmount());
            throw exception(CREDIT_LIMIT_NOT_ENOUGH, customerId, available);
        }
        // 更新状态（基于原子更新后的最新记录，状态仅用于显示，不影响金额正确性）
        refreshStatus(creditLimit.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseCredit(Long customerId, BigDecimal amount) {
        if (customerId == null || amount == null) {
            return;
        }
        ErpCreditLimitDO creditLimit = creditLimitMapper.selectByCustomerId(customerId);
        if (creditLimit == null) {
            log.warn("[releaseCredit][客户({})未配置信用额度记录，跳过释放]", customerId);
            return;
        }
        // 原子更新：used_amount -= amount, available_amount += amount，且 used_amount 不低于 0
        creditLimitMapper.updateUsedAmountDecrement(creditLimit.getId(), amount);
        // 更新状态
        refreshStatus(creditLimit.getId());
    }

    /**
     * 刷新信用额度状态（基于最新 used_amount 重算 status，冻结状态保持）
     * 状态字段仅用于展示，并发场景下短暂不一致可接受
     */
    private void refreshStatus(Long id) {
        ErpCreditLimitDO latest = creditLimitMapper.selectById(id);
        if (latest == null) {
            return;
        }
        int newStatus = determineStatus(latest, nullToZero(latest.getUsedAmount()));
        if (newStatus != latest.getStatus()) {
            ErpCreditLimitDO statusUpdate = new ErpCreditLimitDO();
            statusUpdate.setId(id);
            statusUpdate.setStatus(newStatus);
            creditLimitMapper.updateById(statusUpdate);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 根据已用额度判定状态（冻结状态保持冻结，否则按预警比例切换正常/预警）
     */
    private int determineStatus(ErpCreditLimitDO creditLimit, BigDecimal used) {
        // 冻结状态保持
        if (STATUS_FROZEN == creditLimit.getStatus()) {
            return STATUS_FROZEN;
        }
        BigDecimal limit = nullToZero(creditLimit.getCreditLimit());
        if (limit.compareTo(BigDecimal.ZERO) <= 0) {
            return STATUS_NORMAL;
        }
        BigDecimal ratio = creditLimit.getWarningRatio() == null
                ? new BigDecimal("80") : creditLimit.getWarningRatio();
        // 已用比例 = used / limit * 100
        BigDecimal usedPercent = used.multiply(new BigDecimal("100"))
                .divide(limit, 2, BigDecimal.ROUND_HALF_UP);
        return usedPercent.compareTo(ratio) >= 0 ? STATUS_WARNING : STATUS_NORMAL;
    }

    private BigDecimal calcAvailable(BigDecimal limit, BigDecimal used) {
        BigDecimal available = nullToZero(limit).subtract(nullToZero(used));
        return available.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : available;
    }

    private static BigDecimal nullToZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

}
