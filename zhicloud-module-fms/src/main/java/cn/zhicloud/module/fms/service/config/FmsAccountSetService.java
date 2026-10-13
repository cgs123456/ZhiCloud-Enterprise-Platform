package cn.zhicloud.module.fms.service.config;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsAccountBookRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsAccountSetInitializeReqVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsAccountSetSaveReqVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsAccountSetVoucherRespVO;
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

    /**
     * 分页查询账套下的 ERP 凭证流水（V193 只读；不建平行表，经 erp.api 边界包）
     *
     * @param accountSetId 账套编号
     * @param pageNo 页码
     * @param pageSize 每页条数
     * @return 凭证分页结果
     */
    PageResult<FmsAccountSetVoucherRespVO> getVoucherPage(Long accountSetId, Integer pageNo, Integer pageSize);

}
