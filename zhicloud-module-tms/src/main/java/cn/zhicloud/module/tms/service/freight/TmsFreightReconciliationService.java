package cn.zhicloud.module.tms.service.freight;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.tms.controller.admin.freight.vo.TmsFreightReconciliationPageReqVO;
import cn.zhicloud.module.tms.controller.admin.freight.vo.TmsFreightReconciliationSaveReqVO;
import cn.zhicloud.module.tms.dal.dataobject.freight.TmsFreightReconciliationDO;
import jakarta.validation.Valid;

/**
 * TMS 运费对账 Service 接口
 *
 * @author 智云
 */
public interface TmsFreightReconciliationService {

    /**
     * 创建运费对账单
     */
    Long createReconciliation(@Valid TmsFreightReconciliationSaveReqVO createReqVO);

    /**
     * 更新运费对账单
     */
    void updateReconciliation(@Valid TmsFreightReconciliationSaveReqVO updateReqVO);

    /**
     * 删除运费对账单
     */
    void deleteReconciliation(Long id);

    /**
     * 获取运费对账单
     */
    TmsFreightReconciliationDO getReconciliation(Long id);

    /**
     * 获取运费对账分页
     */
    PageResult<TmsFreightReconciliationDO> getReconciliationPage(TmsFreightReconciliationPageReqVO pageReqVO);

    /**
     * 执行对账（计算差异金额）
     */
    void doReconcile(Long id);

    /**
     * 确认对账
     */
    void confirmReconciliation(Long id);

    /**
     * 驳回对账
     */
    void rejectReconciliation(Long id, String reason);

}
