package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.common.enums.CommonStatusEnum;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.finance.vo.currency.ErpCurrencyPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.currency.ErpCurrencySaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpCurrencyDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpCurrencyMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 币种 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class ErpCurrencyServiceImpl implements ErpCurrencyService {

    @Resource
    private ErpCurrencyMapper currencyMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCurrency(ErpCurrencySaveReqVO createReqVO) {
        // 校验编码唯一
        validateCodeUnique(null, createReqVO.getCode());
        // 校验本位币唯一
        validateBaseUnique(null, createReqVO.getIsBase());
        // 插入
        ErpCurrencyDO currency = BeanUtils.toBean(createReqVO, ErpCurrencyDO.class);
        if (currency.getEnabled() == null) {
            currency.setEnabled(CommonStatusEnum.ENABLE.getStatus());
        }
        if (currency.getIsBase() == null) {
            currency.setIsBase(false);
        }
        currencyMapper.insert(currency);
        return currency.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCurrency(ErpCurrencySaveReqVO updateReqVO) {
        // 校验存在
        validateCurrencyExists(updateReqVO.getId());
        // 校验编码唯一
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getCode());
        // 校验本位币唯一
        validateBaseUnique(updateReqVO.getId(), updateReqVO.getIsBase());
        // 更新
        ErpCurrencyDO updateObj = BeanUtils.toBean(updateReqVO, ErpCurrencyDO.class);
        currencyMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCurrency(Long id) {
        // 校验存在
        validateCurrencyExists(id);
        // 删除
        currencyMapper.deleteById(id);
    }

    @Override
    public ErpCurrencyDO getCurrency(Long id) {
        return currencyMapper.selectById(id);
    }

    @Override
    public PageResult<ErpCurrencyDO> getCurrencyPage(ErpCurrencyPageReqVO pageReqVO) {
        return currencyMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpCurrencyDO> getEnabledCurrencyList() {
        return currencyMapper.selectListByEnabled(CommonStatusEnum.ENABLE.getStatus());
    }

    private void validateCurrencyExists(Long id) {
        if (currencyMapper.selectById(id) == null) {
            throw exception(CURRENCY_NOT_EXISTS);
        }
    }

    private void validateCodeUnique(Long id, String code) {
        ErpCurrencyDO existing = currencyMapper.selectByCode(code);
        if (existing != null && !Objects.equals(existing.getId(), id)) {
            throw exception(CURRENCY_CODE_DUPLICATE, code);
        }
    }

    /**
     * 校验本位币唯一性：系统中最多一个本位币
     */
    private void validateBaseUnique(Long id, Boolean isBase) {
        if (!Boolean.TRUE.equals(isBase)) {
            return;
        }
        ErpCurrencyDO base = currencyMapper.selectByBase();
        if (base != null && !Objects.equals(base.getId(), id)) {
            throw exception(CURRENCY_BASE_EXISTS, base.getCode());
        }
    }

}
