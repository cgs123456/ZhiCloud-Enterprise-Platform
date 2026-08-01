package cn.iocoder.yudao.module.erp.service.purchase.inquiry;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.inquiry.vo.ErpPurchaseInquiryPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.inquiry.vo.ErpPurchaseInquirySaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseInquiryDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseInquiryItemDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * ERP 采购询价单 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpPurchaseInquiryService {

    /**
     * 创建询价单
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createInquiry(@Valid ErpPurchaseInquirySaveReqVO createReqVO);

    /**
     * 更新询价单
     *
     * @param updateReqVO 更新信息
     */
    void updateInquiry(@Valid ErpPurchaseInquirySaveReqVO updateReqVO);

    /**
     * 删除询价单
     *
     * @param ids 编号数组
     */
    void deleteInquiry(List<Long> ids);

    /**
     * 获得询价单
     *
     * @param id 编号
     * @return 询价单
     */
    ErpPurchaseInquiryDO getInquiry(Long id);

    /**
     * 获得询价单分页
     *
     * @param pageReqVO 分页查询
     * @return 询价单分页
     */
    PageResult<ErpPurchaseInquiryDO> getInquiryPage(ErpPurchaseInquiryPageReqVO pageReqVO);

    /**
     * 获得询价单明细列表
     *
     * @param inquiryId 询价单编号
     * @return 询价单明细列表
     */
    List<ErpPurchaseInquiryItemDO> getInquiryItemListByInquiryId(Long inquiryId);

    /**
     * 提交发布询价单
     *
     * @param id 编号
     */
    void submitInquiry(Long id);

    /**
     * 关闭询价单
     *
     * @param id 编号
     */
    void closeInquiry(Long id);

    /**
     * 询价单转采购订单
     *
     * @param inquiryId 询价单编号
     * @param supplierId 中标供应商编号
     * @return 采购订单编号
     */
    Long convertToPurchaseOrder(Long inquiryId, Long supplierId);

}
