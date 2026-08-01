package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.fachange.ErpFaChangePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.fachange.ErpFaChangeSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFaChangeDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * ERP 固定资产变动 Service 接口
 *
 * <p>提供固定资产变动申请的 CRUD + 审核/驳回。
 * 审核通过后，根据变动类型实际更新 {@link cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFixedAssetDO} 对应字段。
 *
 * @author 芋道源码
 */
public interface ErpFaChangeService {

    /**
     * 创建资产变动申请（状态=待审核）
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createFaChange(@Valid ErpFaChangeSaveReqVO createReqVO);

    /**
     * 更新资产变动申请（仅待审核状态可更新）
     *
     * @param updateReqVO 更新信息
     */
    void updateFaChange(@Valid ErpFaChangeSaveReqVO updateReqVO);

    /**
     * 删除资产变动申请（仅待审核状态可删除）
     *
     * @param id 编号
     */
    void deleteFaChange(Long id);

    /**
     * 获取资产变动申请
     *
     * @param id 编号
     * @return 变动记录
     */
    ErpFaChangeDO getFaChange(Long id);

    /**
     * 分页查询资产变动申请
     *
     * @param pageReqVO 分页查询
     * @return 分页结果
     */
    PageResult<ErpFaChangeDO> getFaChangePage(ErpFaChangePageReqVO pageReqVO);

    /**
     * 审核通过资产变动申请
     *
     * <p>审核通过后，根据变动类型实际更新固定资产对应字段：
     * <ul>
     *   <li>10 部门转移：更新 departmentId</li>
     *   <li>20 状态变动：更新 status</li>
     *   <li>30 原值调整：更新 originalValue，并重算 netBookValue</li>
     *   <li>40 使用年限调整：更新 usefulLifeMonths</li>
     *   <li>50 残值调整：更新 salvageValue</li>
     *   <li>60 折旧方法变更：更新 depreciationMethod</li>
     * </ul>
     *
     * @param id 编号
     */
    void approveFaChange(Long id);

    /**
     * 驳回资产变动申请
     *
     * @param id     编号
     * @param reason 驳回原因
     */
    void rejectFaChange(Long id, String reason);

    /**
     * 查询某资产的所有变动历史
     *
     * @param assetId 固定资产编号
     * @return 变动记录列表
     */
    List<ErpFaChangeDO> listByAssetId(Long assetId);

}
