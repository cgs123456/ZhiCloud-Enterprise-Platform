package cn.zhicloud.module.fms.service.config;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.fms.controller.admin.config.vo.financeindicator.FmsFinanceIndicatorSaveReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsFinanceIndicatorDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * FMS 财务指标 Service 接口
 *
 * @author 智云
 */
public interface FmsFinanceIndicatorService {

    Long createFinanceIndicator(@Valid FmsFinanceIndicatorSaveReqVO createReqVO);

    void updateFinanceIndicator(@Valid FmsFinanceIndicatorSaveReqVO updateReqVO);

    void deleteFinanceIndicator(Long accountSetId, Long id);

    FmsFinanceIndicatorDO getFinanceIndicator(Long accountSetId, Long id);

    List<FmsFinanceIndicatorDO> getFinanceIndicatorList(Long accountSetId);

}
