package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.controller.admin.finance.vo.consolidation.ErpConsolidationEntryPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.consolidation.ErpConsolidationEntrySaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpConsolidationEntryDO;
import jakarta.validation.Valid;

/**
 * ERP 合并报表抵消分录 Service 接口（P0-14）
 *
 * <p>提供集团内关联交易抵消分录的 CRUD + 审核功能。
 *
 * @author 智云
 */
public interface ErpConsolidationEntryService {

    /**
     * 创建抵消分录
     *
     * <p>校验借贷平衡、科目不能同时为空，初始化状态为草稿。
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createConsolidationEntry(@Valid ErpConsolidationEntrySaveReqVO createReqVO);

    /**
     * 更新抵消分录（仅草稿状态可改）
     *
     * @param updateReqVO 更新信息
     */
    void updateConsolidationEntry(@Valid ErpConsolidationEntrySaveReqVO updateReqVO);

    /**
     * 删除抵消分录（仅草稿可删）
     *
     * @param id 编号
     */
    void deleteConsolidationEntry(Long id);

    /**
     * 获取抵消分录
     *
     * @param id 编号
     * @return 抵消分录
     */
    ErpConsolidationEntryDO getConsolidationEntry(Long id);

    /**
     * 分页查询抵消分录
     *
     * @param pageReqVO 分页查询
     * @return 分页结果
     */
    PageResult<ErpConsolidationEntryDO> getConsolidationEntryPage(ErpConsolidationEntryPageReqVO pageReqVO);

    /**
     * 审核抵消分录（草稿 → 已审核）
     *
     * @param id 编号
     */
    void approveConsolidationEntry(Long id);

}
