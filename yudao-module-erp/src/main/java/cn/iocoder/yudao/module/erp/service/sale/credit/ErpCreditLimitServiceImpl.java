package cn.iocoder.yudao.module.erp.service.sale.credit;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.credit.vo.ErpCreditLimitPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.credit.vo.ErpCreditLimitSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.credit.ErpCreditLimitDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.credit.ErpCreditLimitMapper;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CREDIT_LIMIT_CUSTOMER_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CREDIT_LIMIT_NOT_EXISTS;

/**
 * ERP 客户信用额度 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpCreditLimitServiceImpl implements ErpCreditLimitService {

    /**
     * 状态：10 正常
     */
    private static final int STATUS_NORMAL = 10;
    /**
     * 默认预警比例 80%
     */
    private static final BigDecimal DEFAULT_WARNING_RATIO = new BigDecimal("80");

    @Resource
    private ErpCreditLimitMapper creditLimitMapper;
    @Resource
    private ErpCustomerService customerService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCreditLimit(ErpCreditLimitSaveReqVO createReqVO) {
        // 校验客户存在
        customerService.validateCustomer(createReqVO.getCustomerId());
        // 校验客户信用额度不重复
        validateCustomerUnique(null, createReqVO.getCustomerId());
        // 插入
        ErpCreditLimitDO creditLimit = BeanUtils.toBean(createReqVO, ErpCreditLimitDO.class);
        // 初始化默认值
        if (creditLimit.getUsedAmount() == null) {
            creditLimit.setUsedAmount(BigDecimal.ZERO);
        }
        if (creditLimit.getOverdueAmount() == null) {
            creditLimit.setOverdueAmount(BigDecimal.ZERO);
        }
        if (creditLimit.getWarningRatio() == null) {
            creditLimit.setWarningRatio(DEFAULT_WARNING_RATIO);
        }
        if (creditLimit.getStatus() == null) {
            creditLimit.setStatus(STATUS_NORMAL);
        }
        // 计算可用额度
        creditLimit.setAvailableAmount(calcAvailable(creditLimit));
        creditLimitMapper.insert(creditLimit);
        return creditLimit.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCreditLimit(ErpCreditLimitSaveReqVO updateReqVO) {
        ErpCreditLimitDO exist = validateCreditLimit(updateReqVO.getId());
        // 校验客户存在
        customerService.validateCustomer(updateReqVO.getCustomerId());
        // 校验客户信用额度不重复
        validateCustomerUnique(updateReqVO.getId(), updateReqVO.getCustomerId());
        ErpCreditLimitDO updateObj = BeanUtils.toBean(updateReqVO, ErpCreditLimitDO.class);
        // 保留已用额度（不允许通过修改覆盖）
        updateObj.setUsedAmount(exist.getUsedAmount());
        // 重新计算可用额度
        updateObj.setAvailableAmount(calcAvailable(updateObj));
        creditLimitMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCreditLimit(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        for (Long id : ids) {
            validateCreditLimit(id);
            creditLimitMapper.deleteById(id);
        }
    }

    @Override
    public ErpCreditLimitDO getCreditLimit(Long id) {
        return creditLimitMapper.selectById(id);
    }

    @Override
    public ErpCreditLimitDO getCreditLimitByCustomerId(Long customerId) {
        if (customerId == null) {
            return null;
        }
        return creditLimitMapper.selectByCustomerId(customerId);
    }

    @Override
    public PageResult<ErpCreditLimitDO> getCreditLimitPage(ErpCreditLimitPageReqVO pageReqVO) {
        return creditLimitMapper.selectPage(pageReqVO);
    }

    @Override
    public ErpCreditLimitDO validateCreditLimit(Long id) {
        ErpCreditLimitDO creditLimit = creditLimitMapper.selectById(id);
        if (creditLimit == null) {
            throw exception(CREDIT_LIMIT_NOT_EXISTS);
        }
        return creditLimit;
    }

    // ==================== 私有方法 ====================

    private void validateCustomerUnique(Long id, Long customerId) {
        if (customerId == null) {
            return;
        }
        ErpCreditLimitDO creditLimit = creditLimitMapper.selectByCustomerId(customerId);
        if (creditLimit == null) {
            return;
        }
        if (id == null || !ObjUtil.equal(creditLimit.getId(), id)) {
            throw exception(CREDIT_LIMIT_CUSTOMER_DUPLICATE, customerId);
        }
    }

    /**
     * 计算可用额度 = 信用额度 - 已用额度
     */
    private BigDecimal calcAvailable(ErpCreditLimitDO creditLimit) {
        BigDecimal limit = nullToZero(creditLimit.getCreditLimit());
        BigDecimal used = nullToZero(creditLimit.getUsedAmount());
        BigDecimal available = limit.subtract(used);
        return available.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : available;
    }

    private static BigDecimal nullToZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

}
