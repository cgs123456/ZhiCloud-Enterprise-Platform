package cn.zhicloud.module.erp.service.purchase.inquiry;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.controller.admin.purchase.inquiry.vo.compare.ErpPurchaseComparePageReqVO;
import cn.zhicloud.module.erp.controller.admin.purchase.inquiry.vo.compare.ErpPurchaseCompareSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseCompareDO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseCompareLineDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * ERP 采购比价单 Service 接口
 *
 * @author 智云
 */
public interface ErpPurchaseCompareService {

    /**
     * 基于询价单 + 所有报价生成比价单，推荐最低价/最快交期供应商
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long generateCompare(@Valid ErpPurchaseCompareSaveReqVO createReqVO);

    /**
     * 获得比价单
     *
     * @param id 编号
     * @return 比价单
     */
    ErpPurchaseCompareDO getCompare(Long id);

    /**
     * 获得比价单分页
     *
     * @param pageReqVO 分页查询
     * @return 比价单分页
     */
    PageResult<ErpPurchaseCompareDO> getComparePage(ErpPurchaseComparePageReqVO pageReqVO);

    /**
     * 获得比价单明细行列表
     *
     * @param compareId 比价单编号
     * @return 比价单明细行列表
     */
    List<ErpPurchaseCompareLineDO> getCompareLineListByCompareId(Long compareId);

}
