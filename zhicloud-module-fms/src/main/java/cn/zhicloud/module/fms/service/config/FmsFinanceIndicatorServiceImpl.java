package cn.zhicloud.module.fms.service.config;

import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.fms.controller.admin.config.vo.financeindicator.FmsFinanceIndicatorSaveReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsFinanceIndicatorDO;
import cn.zhicloud.module.fms.dal.mysql.config.FmsFinanceIndicatorMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.fms.enums.ErrorCodeConstants.FINANCE_INDICATOR_NOT_EXISTS;

/**
 * FMS 财务指标 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class FmsFinanceIndicatorServiceImpl implements FmsFinanceIndicatorService {

    @Resource
    private FmsFinanceIndicatorMapper financeIndicatorMapper;

    @Override
    public Long createFinanceIndicator(FmsFinanceIndicatorSaveReqVO createReqVO) {
        FmsFinanceIndicatorDO entity = BeanUtils.toBean(createReqVO, FmsFinanceIndicatorDO.class);
        financeIndicatorMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void updateFinanceIndicator(FmsFinanceIndicatorSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getAccountSetId(), updateReqVO.getId());
        FmsFinanceIndicatorDO updateObj = BeanUtils.toBean(updateReqVO, FmsFinanceIndicatorDO.class);
        financeIndicatorMapper.updateById(updateObj);
    }

    @Override
    public void deleteFinanceIndicator(Long accountSetId, Long id) {
        validateExists(accountSetId, id);
        financeIndicatorMapper.deleteById(id);
    }

    @Override
    public FmsFinanceIndicatorDO getFinanceIndicator(Long accountSetId, Long id) {
        return financeIndicatorMapper.selectByAccountSetAndId(accountSetId, id);
    }

    @Override
    public List<FmsFinanceIndicatorDO> getFinanceIndicatorList(Long accountSetId) {
        return financeIndicatorMapper.selectListByAccountSet(accountSetId);
    }

    private void validateExists(Long accountSetId, Long id) {
        if (financeIndicatorMapper.selectByAccountSetAndId(accountSetId, id) == null) {
            throw exception(FINANCE_INDICATOR_NOT_EXISTS);
        }
    }

}
