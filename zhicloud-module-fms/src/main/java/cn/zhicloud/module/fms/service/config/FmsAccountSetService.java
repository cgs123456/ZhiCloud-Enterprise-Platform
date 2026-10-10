package cn.zhicloud.module.fms.service.config;

import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsAccountSetInitializeReqVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsAccountSetSaveReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsAccountSetDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * FMS 账套 Service 接口
 *
 * @author 智云
 */
public interface FmsAccountSetService {

    Long createAccountSet(Long userId, @Valid FmsAccountSetSaveReqVO createReqVO);

    void updateAccountSet(Long userId, @Valid FmsAccountSetSaveReqVO updateReqVO);

    void initializeAccountSet(@Valid FmsAccountSetInitializeReqVO reqVO);

    FmsAccountSetDO getAccountSet(Long id);

    List<FmsAccountSetDO> getAccountSetList(Long userId);

}
