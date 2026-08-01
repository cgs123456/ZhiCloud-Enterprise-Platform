package cn.iocoder.yudao.module.erp.service.purchase.inquiry;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.inquiry.vo.quote.ErpPurchaseQuotePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.inquiry.vo.quote.ErpPurchaseQuoteSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseInquiryDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseQuoteItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.inquiry.ErpPurchaseQuoteItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.inquiry.ErpPurchaseQuoteMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 采购报价单 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpPurchaseQuoteServiceImpl implements ErpPurchaseQuoteService {

    @Resource
    private ErpPurchaseQuoteMapper purchaseQuoteMapper;
    @Resource
    private ErpPurchaseQuoteItemMapper purchaseQuoteItemMapper;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpSupplierService supplierService;
    @Resource
    @Lazy
    private ErpPurchaseInquiryService inquiryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createQuote(ErpPurchaseQuoteSaveReqVO createReqVO) {
        // 1.1 校验询价单存在且为已发布状态
        ErpPurchaseInquiryDO inquiry = inquiryService.getInquiry(createReqVO.getInquiryId());
        if (inquiry == null) {
            throw exception(PURCHASE_INQUIRY_NOT_EXISTS);
        }
        // 1.2 校验供应商
        supplierService.validateSupplier(createReqVO.getSupplierId());
        // 1.3 校验同一询价单下同一供应商不重复报价
        List<ErpPurchaseQuoteDO> existQuotes = purchaseQuoteMapper.selectListByInquiryId(createReqVO.getInquiryId());
        if (existQuotes.stream().anyMatch(q -> createReqVO.getSupplierId().equals(q.getSupplierId()))) {
            throw exception(PURCHASE_QUOTE_SUPPLIER_DUPLICATE, createReqVO.getInquiryId(), createReqVO.getSupplierId());
        }
        // 1.4 校验并转化报价明细
        List<ErpPurchaseQuoteItemDO> quoteItems = validateAndCalcQuoteItems(createReqVO.getItems());
        // 1.5 生成报价单号，并校验唯一性
        String no = noRedisDAO.generate(ErpNoRedisDAO.PURCHASE_QUOTE_NO_PREFIX);
        if (purchaseQuoteMapper.selectByNo(no) != null) {
            throw exception(PURCHASE_QUOTE_NO_EXISTS);
        }

        // 2.1 插入报价单（供应商报价即提交，状态=已报价）
        ErpPurchaseQuoteDO quote = BeanUtils.toBean(createReqVO, ErpPurchaseQuoteDO.class, q -> q
                .setNo(no).setStatus(ErpPurchaseQuoteDO.STATUS_QUOTED)
                .setQuoteDate(createReqVO.getQuoteDate() != null ? createReqVO.getQuoteDate() : LocalDateTime.now()));
        calculateTotalAmount(quote, quoteItems);
        purchaseQuoteMapper.insert(quote);
        // 2.2 插入报价明细
        quoteItems.forEach(item -> item.setQuoteId(quote.getId()));
        purchaseQuoteItemMapper.insertBatch(quoteItems);
        return quote.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuote(ErpPurchaseQuoteSaveReqVO updateReqVO) {
        // 1.1 校验存在
        ErpPurchaseQuoteDO quote = validateQuoteExists(updateReqVO.getId());
        // 1.2 校验状态：已采纳或已拒绝不可修改
        if (ErpPurchaseQuoteDO.STATUS_ADOPTED.equals(quote.getStatus())
                || ErpPurchaseQuoteDO.STATUS_REJECTED.equals(quote.getStatus())) {
            throw exception(PURCHASE_QUOTE_UPDATE_FAIL, quote.getNo());
        }
        // 1.3 校验供应商
        supplierService.validateSupplier(updateReqVO.getSupplierId());
        // 1.4 校验并转化报价明细
        List<ErpPurchaseQuoteItemDO> quoteItems = validateAndCalcQuoteItems(updateReqVO.getItems());

        // 2.1 更新报价单
        ErpPurchaseQuoteDO updateObj = BeanUtils.toBean(updateReqVO, ErpPurchaseQuoteDO.class);
        calculateTotalAmount(updateObj, quoteItems);
        purchaseQuoteMapper.updateById(updateObj);
        // 2.2 更新报价明细
        updateQuoteItemList(updateReqVO.getId(), quoteItems);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteQuote(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        List<ErpPurchaseQuoteDO> quotes = purchaseQuoteMapper.selectByIds(ids);
        if (CollUtil.isEmpty(quotes)) {
            return;
        }
        quotes.forEach(quote -> {
            // 校验状态：已采纳或已拒绝不可删除
            if (ErpPurchaseQuoteDO.STATUS_ADOPTED.equals(quote.getStatus())
                    || ErpPurchaseQuoteDO.STATUS_REJECTED.equals(quote.getStatus())) {
                throw exception(PURCHASE_QUOTE_DELETE_FAIL, quote.getNo());
            }
            // 删除报价单
            purchaseQuoteMapper.deleteById(quote.getId());
            // 删除报价明细
            purchaseQuoteItemMapper.deleteByQuoteId(quote.getId());
        });
    }

    @Override
    public ErpPurchaseQuoteDO getQuote(Long id) {
        return purchaseQuoteMapper.selectById(id);
    }

    @Override
    public PageResult<ErpPurchaseQuoteDO> getQuotePage(ErpPurchaseQuotePageReqVO pageReqVO) {
        return purchaseQuoteMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpPurchaseQuoteItemDO> getQuoteItemListByQuoteId(Long quoteId) {
        return purchaseQuoteItemMapper.selectListByQuoteId(quoteId);
    }

    @Override
    public List<ErpPurchaseQuoteItemDO> getQuoteItemListByQuoteIds(Collection<Long> quoteIds) {
        if (CollUtil.isEmpty(quoteIds)) {
            return Collections.emptyList();
        }
        return purchaseQuoteItemMapper.selectListByQuoteIds(quoteIds);
    }

    @Override
    public List<ErpPurchaseQuoteDO> getQuoteListByInquiryId(Long inquiryId) {
        List<ErpPurchaseQuoteDO> quotes = purchaseQuoteMapper.selectListByInquiryId(inquiryId);
        // 仅返回已报价及以上的报价单（排除草稿）
        return quotes.stream()
                .filter(q -> !ErpPurchaseQuoteDO.STATUS_DRAFT.equals(q.getStatus()))
                .toList();
    }

    private List<ErpPurchaseQuoteItemDO> validateAndCalcQuoteItems(List<ErpPurchaseQuoteSaveReqVO.Item> list) {
        if (CollUtil.isEmpty(list)) {
            throw exception(PURCHASE_QUOTE_NOT_EXISTS);
        }
        // 转化为报价明细，并计算报价金额 amount = unitPrice * quantity
        return convertList(list, o -> BeanUtils.toBean(o, ErpPurchaseQuoteItemDO.class, item -> {
            if (item.getUnitPrice() != null && item.getQuantity() != null) {
                item.setAmount(MoneyUtils.priceMultiply(item.getUnitPrice(), item.getQuantity()));
            }
        }));
    }

    private void calculateTotalAmount(ErpPurchaseQuoteDO quote, List<ErpPurchaseQuoteItemDO> items) {
        quote.setTotalAmount(getSumValue(items, ErpPurchaseQuoteItemDO::getAmount, BigDecimal::add, BigDecimal.ZERO));
    }

    private void updateQuoteItemList(Long quoteId, List<ErpPurchaseQuoteItemDO> newList) {
        // 第一步，对比新老数据，获得添加、修改、删除的列表
        List<ErpPurchaseQuoteItemDO> oldList = purchaseQuoteItemMapper.selectListByQuoteId(quoteId);
        List<List<ErpPurchaseQuoteItemDO>> diffList = diffList(oldList, newList,
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));

        // 第二步，批量添加、修改、删除
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setQuoteId(quoteId));
            purchaseQuoteItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            purchaseQuoteItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            purchaseQuoteItemMapper.deleteByIds(convertList(diffList.get(2), ErpPurchaseQuoteItemDO::getId));
        }
    }

    private ErpPurchaseQuoteDO validateQuoteExists(Long id) {
        ErpPurchaseQuoteDO quote = purchaseQuoteMapper.selectById(id);
        if (quote == null) {
            throw exception(PURCHASE_QUOTE_NOT_EXISTS);
        }
        return quote;
    }

}
