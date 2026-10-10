package cn.zhicloud.module.fms.service.config;

import cn.zhicloud.module.fms.controller.admin.config.vo.financeparameter.FmsFinanceParameterRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.financeparameter.FmsFinanceParameterSaveReqVO;

/**
 * FMS 账套参数 Service 接口
 *
 * @author 智云
 */
public interface FmsFinanceParameterService {

    FmsFinanceParameterRespVO getFinanceParameter(Long accountSetId);

    void updateFinanceParameter(FmsFinanceParameterSaveReqVO reqVO);

}
