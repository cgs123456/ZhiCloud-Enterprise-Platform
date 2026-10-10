package cn.zhicloud.module.fms.service.config;

import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.fms.controller.admin.config.vo.currency.FmsCurrencySaveReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsCurrencyDO;
import cn.zhicloud.module.fms.dal.mysql.config.FmsCurrencyMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.fms.enums.ErrorCodeConstants.CURRENCY_NOT_EXISTS;
import static cn.zhicloud.module.fms.enums.ErrorCodeConstants.CURRENCY_CODE_DUPLICATE;
import static cn.zhicloud.module.fms.enums.ErrorCodeConstants.CURRENCY_STANDARD_EXISTS;

/**
 * FMS 币别 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class FmsCurrencyServiceImpl implements FmsCurrencyService {

    @Resource
    private FmsCurrencyMapper currencyMapper;

    @Override
    public Long createCurrency(FmsCurrencySaveReqVO createReqVO) {
        // 校验币别编码在账套内唯一
        if (currencyMapper.selectByAccountSetAndUnique(createReqVO.getAccountSetId(), createReqVO.getCode()) != null) {
            throw exception(CURRENCY_CODE_DUPLICATE);
        }
        // 本位币唯一：若设置本位币，先清除同账套旧本位币
        if (Boolean.TRUE.equals(createReqVO.getStandard())) {
            currencyMapper.selectListByAccountSet(createReqVO.getAccountSetId()).stream()
                    .filter(item -> Boolean.TRUE.equals(item.getStandard()))
                    .forEach(item -> { item.setStandard(false); currencyMapper.updateById(item); });
        }
        FmsCurrencyDO entity = BeanUtils.toBean(createReqVO, FmsCurrencyDO.class);
        currencyMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void updateCurrency(FmsCurrencySaveReqVO updateReqVO) {
        validateExists(updateReqVO.getAccountSetId(), updateReqVO.getId());
        FmsCurrencyDO same = currencyMapper.selectByAccountSetAndUnique(updateReqVO.getAccountSetId(), updateReqVO.getCode());
        if (same != null && !same.getId().equals(updateReqVO.getId())) {
            throw exception(CURRENCY_CODE_DUPLICATE);
        }
        if (Boolean.TRUE.equals(updateReqVO.getStandard())) {
            currencyMapper.selectListByAccountSet(updateReqVO.getAccountSetId()).stream()
                    .filter(item -> Boolean.TRUE.equals(item.getStandard()) && !item.getId().equals(updateReqVO.getId()))
                    .forEach(item -> { item.setStandard(false); currencyMapper.updateById(item); });
        }
        FmsCurrencyDO updateObj = BeanUtils.toBean(updateReqVO, FmsCurrencyDO.class);
        currencyMapper.updateById(updateObj);
    }

    @Override
    public void deleteCurrency(Long accountSetId, Long id) {
        validateExists(accountSetId, id);
        currencyMapper.deleteById(id);
    }

    @Override
    public List<FmsCurrencyDO> getCurrencyList(Long accountSetId) {
        return currencyMapper.selectListByAccountSet(accountSetId);
    }

    private void validateExists(Long accountSetId, Long id) {
        if (currencyMapper.selectByAccountSetAndId(accountSetId, id) == null) {
            throw exception(CURRENCY_NOT_EXISTS);
        }
    }

}
