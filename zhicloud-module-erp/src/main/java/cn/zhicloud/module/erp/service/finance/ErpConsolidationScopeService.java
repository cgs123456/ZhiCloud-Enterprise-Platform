package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.controller.admin.finance.vo.consolidation.ErpConsolidationScopePageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.consolidation.ErpConsolidationScopeSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpConsolidationScopeDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * ERP 合并范围 Service 接口（P1-合并报表引擎）
 *
 * @author 智云
 */
public interface ErpConsolidationScopeService {

    /**
     * 创建合并范围
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createScope(@Valid ErpConsolidationScopeSaveReqVO createReqVO);

    /**
     * 更新合并范围
     *
     * @param updateReqVO 更新信息
     */
    void updateScope(@Valid ErpConsolidationScopeSaveReqVO updateReqVO);

    /**
     * 删除合并范围
     *
     * @param id 编号
     */
    void deleteScope(Long id);

    /**
     * 获取合并范围
     *
     * @param id 编号
     * @return 合并范围
     */
    ErpConsolidationScopeDO getScope(Long id);

    /**
     * 分页查询合并范围
     *
     * @param pageReqVO 分页查询
     * @return 分页结果
     */
    PageResult<ErpConsolidationScopeDO> getScopePage(ErpConsolidationScopePageReqVO pageReqVO);

    /**
     * 按母公司查询启用的合并范围列表（用于抵消引擎）
     *
     * @param parentCompanyId 母公司编号
     * @return 合并范围列表
     */
    List<ErpConsolidationScopeDO> getEnabledScopeListByParent(Long parentCompanyId);

    /**
     * 查询所有启用的合并范围列表
     *
     * @return 合并范围列表
     */
    List<ErpConsolidationScopeDO> getEnabledScopeList();

}
