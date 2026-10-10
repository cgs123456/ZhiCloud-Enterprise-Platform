package cn.zhicloud.module.fms.service.config;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.fms.controller.admin.config.vo.currency.FmsCurrencySaveReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsCurrencyDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * FMS 币别 Service 接口
 *
 * @author 智云
 */
public interface FmsCurrencyService {

    Long createCurrency(@Valid FmsCurrencySaveReqVO createReqVO);

    void updateCurrency(@Valid FmsCurrencySaveReqVO updateReqVO);

    void deleteCurrency(Long accountSetId, Long id);

    List<FmsCurrencyDO> getCurrencyList(Long accountSetId);

}
