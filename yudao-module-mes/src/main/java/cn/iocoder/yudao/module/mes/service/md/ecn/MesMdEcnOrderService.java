package cn.iocoder.yudao.module.mes.service.md.ecn;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.md.ecn.vo.MesMdEcnOrderPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.ecn.vo.MesMdEcnOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.ecn.MesMdEcnOrderDO;
import jakarta.validation.Valid;

/**
 * MES ECN 工程变更单 Service 接口
 *
 * <p>支持创建/更新/删除 ECN 单据，以及提交审核、审核、执行变更的流程。
 * 执行变更（{@link #executeEcnOrder}）时根据 {@code changeType} 对 BOM 主数据执行对应操作。
 *
 * @author 芋道源码
 */
public interface MesMdEcnOrderService {

    /**
     * 创建 ECN 工程变更单
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createEcnOrder(@Valid MesMdEcnOrderSaveReqVO createReqVO);

    /**
     * 更新 ECN 工程变更单（仅草稿状态可改）
     *
     * @param updateReqVO 更新信息
     */
    void updateEcnOrder(@Valid MesMdEcnOrderSaveReqVO updateReqVO);

    /**
     * 删除 ECN 工程变更单（仅草稿状态可删）
     *
     * @param id 编号
     */
    void deleteEcnOrder(Long id);

    /**
     * 获得 ECN 工程变更单（含变更明细）
     *
     * @param id 编号
     * @return ECN 单
     */
    MesMdEcnOrderDO getEcnOrder(Long id);

    /**
     * 校验 ECN 单存在
     *
     * @param id 编号
     * @return ECN 单
     */
    MesMdEcnOrderDO validateEcnOrderExists(Long id);

    /**
     * 获得 ECN 工程变更单分页
     *
     * @param pageReqVO 分页查询
     * @return 分页结果
     */
    PageResult<MesMdEcnOrderDO> getEcnOrderPage(MesMdEcnOrderPageReqVO pageReqVO);

    /**
     * 提交审核（草稿 → 审核中）
     *
     * @param id 编号
     */
    void submitEcnOrder(Long id);

    /**
     * 审核 ECN 单（审核中 → 已批准/已驳回）
     *
     * @param id            编号
     * @param approved      true=批准 false=驳回
     * @param approveUserId 审批人
     */
    void approveEcnOrder(Long id, boolean approved, Long approveUserId);

    /**
     * 执行变更（已批准 → 已执行）
     *
     * <p>根据 {@code changeType} 对 BOM 主数据执行对应操作：
     * <ul>
     *   <li>10 新增 BOM：启用新 BOM</li>
     *   <li>20 修改 BOM：停用原 BOM、启用新 BOM</li>
     *   <li>30 删除 BOM：删除原 BOM</li>
     *   <li>40 替换物料：将原 BOM 明细按变更项更新到新值</li>
     * </ul>
     *
     * @param id 编号
     */
    void executeEcnOrder(Long id);

}
