package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.exchangerate.ErpExchangeRatePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.exchangerate.ErpExchangeRateSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpCurrencyDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpExchangeRateDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpExchangeRateMapper;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 汇率 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpExchangeRateServiceImpl implements ErpExchangeRateService {

    @Resource
    private ErpExchangeRateMapper exchangeRateMapper;
    /**
     * 延迟注入，避免循环依赖
     */
    @Resource
    @Lazy
    private ErpCurrencyService currencyService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createExchangeRate(ErpExchangeRateSaveReqVO createReqVO) {
        // 校验源币种与目标币种
        validateCurrency(createReqVO.getFromCurrencyId());
        validateCurrency(createReqVO.getToCurrencyId());
        // 不能为同币种
        if (Objects.equals(createReqVO.getFromCurrencyId(), createReqVO.getToCurrencyId())) {
            throw exception(EXCHANGE_RATE_NOT_FOUND, createReqVO.getFromCurrencyId(), createReqVO.getToCurrencyId());
        }
        // 插入
        ErpExchangeRateDO rate = BeanUtils.toBean(createReqVO, ErpExchangeRateDO.class);
        exchangeRateMapper.insert(rate);
        return rate.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateExchangeRate(ErpExchangeRateSaveReqVO updateReqVO) {
        // 校验存在
        validateExchangeRateExists(updateReqVO.getId());
        // 校验源币种与目标币种
        validateCurrency(updateReqVO.getFromCurrencyId());
        validateCurrency(updateReqVO.getToCurrencyId());
        // 更新
        ErpExchangeRateDO updateObj = BeanUtils.toBean(updateReqVO, ErpExchangeRateDO.class);
        exchangeRateMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteExchangeRate(Long id) {
        // 校验存在
        validateExchangeRateExists(id);
        // 删除
        exchangeRateMapper.deleteById(id);
    }

    @Override
    public ErpExchangeRateDO getExchangeRate(Long id) {
        return exchangeRateMapper.selectById(id);
    }

    @Override
    public PageResult<ErpExchangeRateDO> getExchangeRatePage(ErpExchangeRatePageReqVO pageReqVO) {
        return exchangeRateMapper.selectPage(pageReqVO);
    }

    @Override
    public ErpExchangeRateDO getLatestRate(Long fromId, Long toId, LocalDate date) {
        if (fromId == null || toId == null) {
            return null;
        }
        return exchangeRateMapper.selectLatestRate(fromId, toId, date);
    }

    private void validateExchangeRateExists(Long id) {
        if (exchangeRateMapper.selectById(id) == null) {
            throw exception(EXCHANGE_RATE_NOT_EXISTS);
        }
    }

    private void validateCurrency(Long currencyId) {
        ErpCurrencyDO currency = currencyService.getCurrency(currencyId);
        if (currency == null) {
            throw exception(CURRENCY_NOT_EXISTS);
        }
    }

}
