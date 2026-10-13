package cn.zhicloud.module.fms.service.config;

import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsAccountBookRespVO;
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

    /**
     * 获得账套关联的 ERP 账簿列表（V191 打通「账套 → 账簿」层级后的跨模块只读查询）
     *
     * @param accountSetId 账套编号
     * @return 账簿列表，无则空列表
     */
    List<FmsAccountBookRespVO> getAccountBookList(Long accountSetId);

}
