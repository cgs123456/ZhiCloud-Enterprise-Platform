package cn.zhicloud.module.fms.service.config;

import cn.zhicloud.module.fms.controller.admin.config.vo.accountuser.FmsAccountUserRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountuser.FmsAccountUserUpdateReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsAccountUserDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * FMS 账套成员 Service 接口
 *
 * @author 智云
 */
public interface FmsAccountUserService {

    List<FmsAccountUserRespVO> getAccountUserList(Long accountSetId);

    FmsAccountUserDO getAccountUser(Long accountSetId, Long userId);

    void updateAccountUserList(@Valid FmsAccountUserUpdateReqVO reqVO);

    void updateDefaultStatus(Long userId, Long accountSetId);

}
