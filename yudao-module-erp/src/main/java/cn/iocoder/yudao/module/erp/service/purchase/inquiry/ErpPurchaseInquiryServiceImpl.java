package cn.iocoder.yudao.module.erp.service.purchase.inquiry;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.inquiry.vo.ErpPurchaseInquiryPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.inquiry.vo.ErpPurchaseInquirySaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseInquiryDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseInquiryItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseQuoteItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.inquiry.ErpPurchaseInquiryItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.inquiry.ErpPurchaseInquiryMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseOrderService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 采购询价单 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpPurchaseInquiryServiceImpl implements ErpPurchaseInquiryService {

    @Resource
    private ErpPurchaseInquiryMapper purchaseInquiryMapper;
    @Resource
    private ErpPurchaseInquiryItemMapper purchaseInquiryItemMapper;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpProductService productService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpPurchaseOrderService purchaseOrderService;
    @Resource
    private ErpPurchaseQuoteService quoteService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createInquiry(ErpPurchaseInquirySaveReqVO createReqVO) {
        // 1.1 校验供应商
        validateSuppliers(createReqVO.getSupplierIds());
        // 1.2 校验询价明细
        List<ErpPurchaseInquiryItemDO> inquiryItems = validateInquiryItems(createReqVO.getItems());
        // 1.3 生成询价单号，并校验唯一性
        String no = noRedisDAO.generate(ErpNoRedisDAO.PURCHASE_INQUIRY_NO_PREFIX);
        if (purchaseInquiryMapper.selectByNo(no) != null) {
            throw exception(PURCHASE_INQUIRY_NO_EXISTS);
        }

        // 2.1 插入询价单
        ErpPurchaseInquiryDO inquiry = BeanUtils.toBean(createReqVO, ErpPurchaseInquiryDO.class, in -> in
                .setNo(no).setStatus(ErpPurchaseInquiryDO.STATUS_DRAFT));
        calculateTotalAmount(inquiry, inquiryItems);
        purchaseInquiryMapper.insert(inquiry);
        // 2.2 插入询价明细
        inquiryItems.forEach(item -> item.setInquiryId(inquiry.getId()));
        purchaseInquiryItemMapper.insertBatch(inquiryItems);
        return inquiry.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInquiry(ErpPurchaseInquirySaveReqVO updateReqVO) {
        // 1.1 校验存在
        ErpPurchaseInquiryDO inquiry = validateInquiryExists(updateReqVO.getId());
        // 1.2 校验状态：仅草稿可修改
        if (!ErpPurchaseInquiryDO.STATUS_DRAFT.equals(inquiry.getStatus())) {
            throw exception(PURCHASE_INQUIRY_UPDATE_FAIL, inquiry.getNo());
        }
        // 1.3 校验供应商
        validateSuppliers(updateReqVO.getSupplierIds());
        // 1.4 校验询价明细
        List<ErpPurchaseInquiryItemDO> inquiryItems = validateInquiryItems(updateReqVO.getItems());

        // 2.1 更新询价单
        ErpPurchaseInquiryDO updateObj = BeanUtils.toBean(updateReqVO, ErpPurchaseInquiryDO.class);
        calculateTotalAmount(updateObj, inquiryItems);
        purchaseInquiryMapper.updateById(updateObj);
        // 2.2 更新询价明细
        updateInquiryItemList(updateReqVO.getId(), inquiryItems);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInquiry(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        List<ErpPurchaseInquiryDO> inquiries = purchaseInquiryMapper.selectByIds(ids);
        if (CollUtil.isEmpty(inquiries)) {
            return;
        }
        inquiries.forEach(inquiry -> {
            // 校验状态：仅草稿可删除
            if (!ErpPurchaseInquiryDO.STATUS_DRAFT.equals(inquiry.getStatus())) {
                throw exception(PURCHASE_INQUIRY_DELETE_FAIL, inquiry.getNo());
            }
            // 删除询价单
            purchaseInquiryMapper.deleteById(inquiry.getId());
            // 删除询价明细
            purchaseInquiryItemMapper.deleteByInquiryId(inquiry.getId());
        });
    }

    @Override
    public ErpPurchaseInquiryDO getInquiry(Long id) {
        return purchaseInquiryMapper.selectById(id);
    }

    @Override
    public PageResult<ErpPurchaseInquiryDO> getInquiryPage(ErpPurchaseInquiryPageReqVO pageReqVO) {
        return purchaseInquiryMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpPurchaseInquiryItemDO> getInquiryItemListByInquiryId(Long inquiryId) {
        return purchaseInquiryItemMapper.selectListByInquiryId(inquiryId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitInquiry(Long id) {
        // 1. 校验存在
        ErpPurchaseInquiryDO inquiry = validateInquiryExists(id);
        // 2. 校验状态：仅草稿可提交发布
        if (!ErpPurchaseInquiryDO.STATUS_DRAFT.equals(inquiry.getStatus())) {
            throw exception(PURCHASE_INQUIRY_SUBMIT_FAIL, inquiry.getNo());
        }
        // 3. 更新状态为已发布
        purchaseInquiryMapper.updateById(new ErpPurchaseInquiryDO().setId(id)
                .setStatus(ErpPurchaseInquiryDO.STATUS_PUBLISHED));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeInquiry(Long id) {
        // 1. 校验存在
        ErpPurchaseInquiryDO inquiry = validateInquiryExists(id);
        // 2. 校验状态：仅已发布可关闭
        if (!ErpPurchaseInquiryDO.STATUS_PUBLISHED.equals(inquiry.getStatus())) {
            throw exception(PURCHASE_INQUIRY_CLOSE_FAIL, inquiry.getNo());
        }
        // 3. 更新状态为已关闭
        purchaseInquiryMapper.updateById(new ErpPurchaseInquiryDO().setId(id)
                .setStatus(ErpPurchaseInquiryDO.STATUS_CLOSED));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long convertToPurchaseOrder(Long inquiryId, Long supplierId) {
        // 1.1 校验询价单存在且为已比价状态
        ErpPurchaseInquiryDO inquiry = validateInquiryExists(inquiryId);
        if (!ErpPurchaseInquiryDO.STATUS_COMPARED.equals(inquiry.getStatus())) {
            throw exception(PURCHASE_INQUIRY_CONVERT_FAIL_STATUS, inquiry.getNo());
        }
        // 1.2 校验供应商
        supplierService.validateSupplier(supplierId);

        // 2. 拉取该询价单下所有已报价的报价单，定位指定供应商的报价单
        List<ErpPurchaseQuoteDO> quotes = quoteService.getQuoteListByInquiryId(inquiryId);
        ErpPurchaseQuoteDO targetQuote = quotes.stream()
                .filter(q -> supplierId.equals(q.getSupplierId()))
                .findFirst()
                .orElse(null);
        if (targetQuote == null) {
            throw exception(PURCHASE_INQUIRY_CONVERT_FAIL_QUOTE, inquiry.getNo(), supplierId);
        }

        // 3. 获取报价明细，构建采购订单项
        List<ErpPurchaseQuoteItemDO> quoteItems = quoteService.getQuoteItemListByQuoteId(targetQuote.getId());
        Map<Long, ErpProductDO> productMap = productService.validProductList(
                convertSet(quoteItems, ErpPurchaseQuoteItemDO::getProductId)).stream()
                .collect(Collectors.toMap(ErpProductDO::getId, p -> p));
        List<ErpPurchaseOrderSaveReqVO.Item> orderItems = convertList(quoteItems, qi -> {
            ErpPurchaseOrderSaveReqVO.Item orderItem = new ErpPurchaseOrderSaveReqVO.Item();
            orderItem.setProductId(qi.getProductId());
            ErpProductDO product = productMap.get(qi.getProductId());
            orderItem.setProductUnitId(product != null ? product.getUnitId() : null);
            orderItem.setProductPrice(qi.getUnitPrice());
            orderItem.setCount(qi.getQuantity());
            return orderItem;
        });

        // 4. 创建采购订单
        ErpPurchaseOrderSaveReqVO orderReqVO = new ErpPurchaseOrderSaveReqVO();
        orderReqVO.setSupplierId(supplierId);
        orderReqVO.setOrderTime(LocalDateTime.now());
        orderReqVO.setItems(orderItems);
        Long orderId = purchaseOrderService.createPurchaseOrder(orderReqVO);

        // 5. 更新询价单状态为已转采购订单
        purchaseInquiryMapper.updateById(new ErpPurchaseInquiryDO().setId(inquiryId)
                .setStatus(ErpPurchaseInquiryDO.STATUS_CONVERTED));
        return orderId;
    }

    private void validateSuppliers(String supplierIds) {
        List<Long> ids = StrUtil.split(supplierIds, ",")
                .stream().map(String::trim).filter(StrUtil::isNotBlank)
                .map(Long::parseLong).distinct().collect(Collectors.toList());
        if (CollUtil.isEmpty(ids)) {
            throw exception(SUPPLIER_NOT_EXISTS);
        }
        if (supplierService.getSupplierList(ids).size() != ids.size()) {
            throw exception(SUPPLIER_NOT_EXISTS);
        }
    }

    private List<ErpPurchaseInquiryItemDO> validateInquiryItems(List<ErpPurchaseInquirySaveReqVO.Item> list) {
        // 1. 校验产品存在
        List<ErpProductDO> productList = productService.validProductList(
                convertSet(list, ErpPurchaseInquirySaveReqVO.Item::getProductId));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        // 2. 转化为询价明细，并冗余产品名称
        return convertList(list, o -> BeanUtils.toBean(o, ErpPurchaseInquiryItemDO.class, item -> {
            ErpProductDO product = productMap.get(item.getProductId());
            if (product != null) {
                item.setProductName(product.getName());
            }
        }));
    }

    private void calculateTotalAmount(ErpPurchaseInquiryDO inquiry, List<ErpPurchaseInquiryItemDO> items) {
        inquiry.setTotalAmount(getSumValue(items, item ->
                MoneyUtils.priceMultiply(item.getUnitPrice(), item.getQuantity()), BigDecimal::add, BigDecimal.ZERO));
    }

    private void updateInquiryItemList(Long inquiryId, List<ErpPurchaseInquiryItemDO> newList) {
        // 第一步，对比新老数据，获得添加、修改、删除的列表
        List<ErpPurchaseInquiryItemDO> oldList = purchaseInquiryItemMapper.selectListByInquiryId(inquiryId);
        List<List<ErpPurchaseInquiryItemDO>> diffList = diffList(oldList, newList,
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));

        // 第二步，批量添加、修改、删除
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setInquiryId(inquiryId));
            purchaseInquiryItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            purchaseInquiryItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            purchaseInquiryItemMapper.deleteByIds(convertList(diffList.get(2), ErpPurchaseInquiryItemDO::getId));
        }
    }

    private ErpPurchaseInquiryDO validateInquiryExists(Long id) {
        ErpPurchaseInquiryDO inquiry = purchaseInquiryMapper.selectById(id);
        if (inquiry == null) {
            throw exception(PURCHASE_INQUIRY_NOT_EXISTS);
        }
        return inquiry;
    }

}
