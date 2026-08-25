package cn.zhicloud.module.erp.service.purchase.inquiry;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.purchase.inquiry.vo.compare.ErpPurchaseComparePageReqVO;
import cn.zhicloud.module.erp.controller.admin.purchase.inquiry.vo.compare.ErpPurchaseCompareSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseCompareDO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseCompareLineDO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseInquiryDO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseInquiryItemDO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseQuoteDO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseQuoteItemDO;
import cn.zhicloud.module.erp.dal.mysql.purchase.inquiry.ErpPurchaseCompareLineMapper;
import cn.zhicloud.module.erp.dal.mysql.purchase.inquiry.ErpPurchaseCompareMapper;
import cn.zhicloud.module.erp.dal.mysql.purchase.inquiry.ErpPurchaseInquiryMapper;
import cn.zhicloud.module.erp.dal.mysql.purchase.inquiry.ErpPurchaseQuoteMapper;
import cn.zhicloud.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.zhicloud.module.erp.service.purchase.ErpSupplierService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.*;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 采购比价单 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class ErpPurchaseCompareServiceImpl implements ErpPurchaseCompareService {

    @Resource
    private ErpPurchaseCompareMapper purchaseCompareMapper;
    @Resource
    private ErpPurchaseCompareLineMapper purchaseCompareLineMapper;
    @Resource
    private ErpPurchaseInquiryMapper purchaseInquiryMapper;
    @Resource
    private ErpPurchaseQuoteMapper purchaseQuoteMapper;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpPurchaseInquiryService inquiryService;
    @Resource
    private ErpPurchaseQuoteService quoteService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateCompare(ErpPurchaseCompareSaveReqVO createReqVO) {
        Long inquiryId = createReqVO.getInquiryId();
        // 1. 校验询价单存在
        ErpPurchaseInquiryDO inquiry = inquiryService.getInquiry(inquiryId);
        if (inquiry == null) {
            throw exception(PURCHASE_INQUIRY_NOT_EXISTS);
        }
        // 2. 校验尚未生成比价单
        if (purchaseCompareMapper.selectByInquiryId(inquiryId) != null) {
            throw exception(PURCHASE_COMPARE_FAIL_EXISTS, inquiryId);
        }
        // 3. 拉取已报价的报价单
        List<ErpPurchaseQuoteDO> quotes = quoteService.getQuoteListByInquiryId(inquiryId);
        if (CollUtil.isEmpty(quotes)) {
            throw exception(PURCHASE_COMPARE_FAIL_NO_QUOTE, inquiryId);
        }
        // 4. 拉取所有报价明细、询价明细
        List<ErpPurchaseQuoteItemDO> quoteItems = quoteService.getQuoteItemListByQuoteIds(
                convertSet(quotes, ErpPurchaseQuoteDO::getId));
        List<ErpPurchaseInquiryItemDO> inquiryItems = inquiryService.getInquiryItemListByInquiryId(inquiryId);

        // 5. 生成比价明细行，并按询价明细找出最低价推荐
        Map<Long, Long> quoteSupplierMap = quotes.stream()
                .collect(Collectors.toMap(ErpPurchaseQuoteDO::getId, ErpPurchaseQuoteDO::getSupplierId));
        Map<Long, List<ErpPurchaseQuoteItemDO>> inquiryItemQuoteMap = convertMultiMap(
                quoteItems, ErpPurchaseQuoteItemDO::getInquiryItemId);

        List<ErpPurchaseCompareLineDO> lines = new ArrayList<>();
        for (ErpPurchaseInquiryItemDO inquiryItem : inquiryItems) {
            List<ErpPurchaseQuoteItemDO> itemQuotes = inquiryItemQuoteMap.getOrDefault(
                    inquiryItem.getId(), Collections.emptyList());
            // 找最低价（推荐该供应商）
            ErpPurchaseQuoteItemDO minPriceItem = itemQuotes.stream()
                    .filter(i -> i.getUnitPrice() != null)
                    .min(Comparator.comparing(ErpPurchaseQuoteItemDO::getUnitPrice))
                    .orElse(null);
            for (ErpPurchaseQuoteItemDO qi : itemQuotes) {
                lines.add(ErpPurchaseCompareLineDO.builder()
                        .inquiryItemId(inquiryItem.getId())
                        .productId(qi.getProductId())
                        .supplierId(quoteSupplierMap.get(qi.getQuoteId()))
                        .quoteItemId(qi.getId())
                        .unitPrice(qi.getUnitPrice())
                        .amount(qi.getAmount())
                        .deliveryDate(qi.getDeliveryDate())
                        .isRecommended(minPriceItem != null && qi.getId().equals(minPriceItem.getId()))
                        .build());
            }
        }

        // 6. 推荐供应商：总报价金额最低，金额相同取交期最早（最快交期）
        Long recommendSupplierId = calcRecommendSupplier(quotes, quoteItems, quoteSupplierMap);
        String recommendReason = recommendSupplierId != null
                ? "总价最低且交期最优的供应商" : null;

        // 7. 生成比价单号，并校验唯一性
        String no = noRedisDAO.generate(ErpNoRedisDAO.PURCHASE_COMPARE_NO_PREFIX);
        if (purchaseCompareMapper.selectByNo(no) != null) {
            throw exception(PURCHASE_COMPARE_NO_EXISTS);
        }

        // 8. 插入比价单主表
        ErpPurchaseCompareDO compare = BeanUtils.toBean(createReqVO, ErpPurchaseCompareDO.class, c -> c
                .setNo(no).setStatus(ErpPurchaseCompareDO.STATUS_FINISHED)
                .setRecommendSupplierId(recommendSupplierId)
                .setRecommendReason(recommendReason)
                .setTotalQuoteCount(quotes.size()));
        purchaseCompareMapper.insert(compare);
        // 9. 插入比价明细行
        lines.forEach(line -> line.setCompareId(compare.getId()));
        purchaseCompareLineMapper.insertBatch(lines);

        // 10. 更新询价单状态为已比价
        purchaseInquiryMapper.updateById(new ErpPurchaseInquiryDO().setId(inquiryId)
                .setStatus(ErpPurchaseInquiryDO.STATUS_COMPARED));
        // 11. 标记推荐供应商的报价单为已采纳
        if (recommendSupplierId != null) {
            quotes.stream()
                    .filter(q -> recommendSupplierId.equals(q.getSupplierId()))
                    .findFirst()
                    .ifPresent(q -> purchaseQuoteMapper.updateById(new ErpPurchaseQuoteDO().setId(q.getId())
                            .setStatus(ErpPurchaseQuoteDO.STATUS_ADOPTED)));
        }
        return compare.getId();
    }

    /**
     * 计算推荐供应商：按总报价金额升序，金额相同取交期最早（最快交期）
     *
     * @param quotes 报价单列表
     * @param quoteItems 所有报价明细
     * @param quoteSupplierMap 报价单 -> 供应商 映射
     * @return 推荐供应商编号
     */
    private Long calcRecommendSupplier(List<ErpPurchaseQuoteDO> quotes, List<ErpPurchaseQuoteItemDO> quoteItems,
                                       Map<Long, Long> quoteSupplierMap) {
        // 每个供应商的总金额
        Map<Long, BigDecimal> supplierTotalMap = new HashMap<>();
        // 每个供应商的最晚交货日（整体交期）
        Map<Long, LocalDate> supplierDeliveryMap = new HashMap<>();
        for (ErpPurchaseQuoteItemDO qi : quoteItems) {
            Long supplierId = quoteSupplierMap.get(qi.getQuoteId());
            if (supplierId == null) {
                continue;
            }
            if (qi.getAmount() != null) {
                supplierTotalMap.merge(supplierId, qi.getAmount(), BigDecimal::add);
            }
            if (qi.getDeliveryDate() != null) {
                supplierDeliveryMap.merge(supplierId, qi.getDeliveryDate(),
                        (a, b) -> a.isAfter(b) ? a : b);
            }
        }
        return quotes.stream()
                .map(ErpPurchaseQuoteDO::getSupplierId)
                .distinct()
                .min((s1, s2) -> {
                    // 1. 按总报价金额升序
                    int cmp = supplierTotalMap.getOrDefault(s1, BigDecimal.ZERO)
                            .compareTo(supplierTotalMap.getOrDefault(s2, BigDecimal.ZERO));
                    if (cmp != 0) {
                        return cmp;
                    }
                    // 2. 金额相同取交期最早（最快交期）；null 交期排最后
                    LocalDate d1 = supplierDeliveryMap.get(s1);
                    LocalDate d2 = supplierDeliveryMap.get(s2);
                    if (d1 == null && d2 == null) {
                        return 0;
                    }
                    if (d1 == null) {
                        return 1;
                    }
                    if (d2 == null) {
                        return -1;
                    }
                    return d1.compareTo(d2);
                })
                .orElse(null);
    }

    @Override
    public ErpPurchaseCompareDO getCompare(Long id) {
        return purchaseCompareMapper.selectById(id);
    }

    @Override
    public PageResult<ErpPurchaseCompareDO> getComparePage(ErpPurchaseComparePageReqVO pageReqVO) {
        return purchaseCompareMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpPurchaseCompareLineDO> getCompareLineListByCompareId(Long compareId) {
        return purchaseCompareLineMapper.selectListByCompareId(compareId);
    }

}
