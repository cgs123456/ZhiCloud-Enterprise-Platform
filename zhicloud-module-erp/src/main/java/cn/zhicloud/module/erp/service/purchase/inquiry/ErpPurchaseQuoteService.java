package cn.zhicloud.module.erp.service.purchase.inquiry;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.controller.admin.purchase.inquiry.vo.quote.ErpPurchaseQuotePageReqVO;
import cn.zhicloud.module.erp.controller.admin.purchase.inquiry.vo.quote.ErpPurchaseQuoteSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseQuoteDO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseQuoteItemDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;

/**
 * ERP 采购报价单 Service 接口
 *
 * @author 智云
 */
public interface ErpPurchaseQuoteService {

    /**
     * 供应商报价，创建报价单
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createQuote(@Valid ErpPurchaseQuoteSaveReqVO createReqVO);

    /**
     * 更新报价单
     *
     * @param updateReqVO 更新信息
     */
    void updateQuote(@Valid ErpPurchaseQuoteSaveReqVO updateReqVO);

    /**
     * 删除报价单
     *
     * @param ids 编号数组
     */
    void deleteQuote(List<Long> ids);

    /**
     * 获得报价单
     *
     * @param id 编号
     * @return 报价单
     */
    ErpPurchaseQuoteDO getQuote(Long id);

    /**
     * 获得报价单分页
     *
     * @param pageReqVO 分页查询
     * @return 报价单分页
     */
    PageResult<ErpPurchaseQuoteDO> getQuotePage(ErpPurchaseQuotePageReqVO pageReqVO);

    /**
     * 获得报价单明细列表
     *
     * @param quoteId 报价单编号
     * @return 报价单明细列表
     */
    List<ErpPurchaseQuoteItemDO> getQuoteItemListByQuoteId(Long quoteId);

    /**
     * 获得报价单明细列表
     *
     * @param quoteIds 报价单编号数组
     * @return 报价单明细列表
     */
    List<ErpPurchaseQuoteItemDO> getQuoteItemListByQuoteIds(Collection<Long> quoteIds);

    /**
     * 比价时拉取询价单下所有已报价的报价单
     *
     * @param inquiryId 询价单编号
     * @return 报价单列表
     */
    List<ErpPurchaseQuoteDO> getQuoteListByInquiryId(Long inquiryId);

}
