package cn.zhicloud.module.erp.service.sale;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.number.MoneyUtils;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.framework.integration.api.EventAppendReq;
import cn.zhicloud.framework.integration.contract.ErpSaleOutApprovedEvent;
import cn.zhicloud.framework.integration.contract.IntegrationEventTypes;
import cn.zhicloud.framework.integration.service.EventOutboxService;
import cn.zhicloud.module.erp.controller.admin.sale.vo.out.ErpSaleOutPageReqVO;
import cn.zhicloud.module.erp.controller.admin.sale.vo.out.ErpSaleOutSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.product.ErpProductDO;
import cn.zhicloud.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.zhicloud.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.zhicloud.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.zhicloud.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.zhicloud.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.zhicloud.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.zhicloud.module.erp.enums.ErpAuditStatus;
import cn.zhicloud.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.zhicloud.module.erp.service.accounting.ErpAccountingEventService;
import cn.zhicloud.module.erp.service.accounting.bo.ErpAccountingEventCreateReqBO;
import cn.zhicloud.module.erp.service.accounting.bo.ErpAccountingEventReverseReqBO;
import cn.zhicloud.module.erp.service.finance.ErpAccountService;
import cn.zhicloud.module.erp.service.product.ErpProductService;
import cn.zhicloud.module.erp.service.stock.ErpStockRecordService;
import cn.zhicloud.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.zhicloud.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.*;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.*;
import static cn.zhicloud.framework.integration.enums.ErrorCodeConstants.EVENT_TENANT_MISMATCH;
import static cn.zhicloud.framework.integration.enums.ErrorCodeConstants.EVENT_TENANT_REQUIRED;

// 操作日志已通过 @OperateLog 注解自动记录（bizlog-sdk 通过 AOP 自动记录）

/**
 * ERP 销售出库 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class ErpSaleOutServiceImpl implements ErpSaleOutService {

    @Resource
    private ErpSaleOutMapper saleOutMapper;
    @Resource
    private ErpSaleOutItemMapper saleOutItemMapper;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpProductService productService;
    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private ErpSaleOrderService saleOrderService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private ErpStockRecordService stockRecordService;
    @Resource
    private ErpAccountingEventService accountingEventService;

    /**
     * V101：跨域事件发件箱。审核通过时与业务同事务登记事件（业务回滚则事件一并消失），
     * 投递由集成 Starter 的中继任务异步完成，本方法不做任何跨模块远程调用。
     */
    @Resource
    private EventOutboxService eventOutboxService;

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSaleOut(ErpSaleOutSaveReqVO createReqVO) {
        // 1.1 校验销售订单已审核
        ErpSaleOrderDO saleOrder = saleOrderService.validateSaleOrder(createReqVO.getOrderId());
        // 1.2 校验出库项的有效性
        List<ErpSaleOutItemDO> saleOutItems = validateSaleOutItems(createReqVO.getItems());
        // 1.3 校验结算账户
        accountService.validateAccount(createReqVO.getAccountId());
        // 1.4 校验销售人员
        if (createReqVO.getSaleUserId() != null) {
            adminUserApi.validateUser(createReqVO.getSaleUserId());
        }
        // 1.5 生成出库单号，并校验唯一性
        String no = noRedisDAO.generate(ErpNoRedisDAO.SALE_OUT_NO_PREFIX);
        if (saleOutMapper.selectByNo(no) != null) {
            throw exception(SALE_OUT_NO_EXISTS);
        }

        // 2.1 插入出库
        ErpSaleOutDO saleOut = BeanUtils.toBean(createReqVO, ErpSaleOutDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus()))
                .setOrderNo(saleOrder.getNo()).setCustomerId(saleOrder.getCustomerId());
        calculateTotalPrice(saleOut, saleOutItems);
        saleOutMapper.insert(saleOut);
        // 2.2 插入出库项
        saleOutItems.forEach(o -> o.setOutId(saleOut.getId()));
        saleOutItemMapper.insertBatch(saleOutItems);

        // 3. 更新销售订单的出库数量
        updateSaleOrderOutCount(createReqVO.getOrderId());
        return saleOut.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleOut(ErpSaleOutSaveReqVO updateReqVO) {
        // 1.1 校验存在
        ErpSaleOutDO saleOut = validateSaleOutExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(saleOut.getStatus())) {
            throw exception(SALE_OUT_UPDATE_FAIL_APPROVE, saleOut.getNo());
        }
        // 1.2 校验销售订单已审核
        ErpSaleOrderDO saleOrder = saleOrderService.validateSaleOrder(updateReqVO.getOrderId());
        // 1.3 校验结算账户
        accountService.validateAccount(updateReqVO.getAccountId());
        // 1.4 校验销售人员
        if (updateReqVO.getSaleUserId() != null) {
            adminUserApi.validateUser(updateReqVO.getSaleUserId());
        }
        // 1.5 校验订单项的有效性
        List<ErpSaleOutItemDO> saleOutItems = validateSaleOutItems(updateReqVO.getItems());

        // 2.1 更新出库
        ErpSaleOutDO updateObj = BeanUtils.toBean(updateReqVO, ErpSaleOutDO.class)
                .setOrderNo(saleOrder.getNo()).setCustomerId(saleOrder.getCustomerId());
        calculateTotalPrice(updateObj, saleOutItems);
        saleOutMapper.updateById(updateObj);
        // 2.2 更新出库项
        updateSaleOutItemList(updateReqVO.getId(), saleOutItems);

        // 3.1 更新销售订单的出库数量
        updateSaleOrderOutCount(updateObj.getOrderId());
        // 3.2 注意：如果销售订单编号变更了，需要更新“老”销售订单的出库数量
        if (ObjectUtil.notEqual(saleOut.getOrderId(), updateObj.getOrderId())) {
            updateSaleOrderOutCount(saleOut.getOrderId());
        }
    }

    private void calculateTotalPrice(ErpSaleOutDO saleOut, List<ErpSaleOutItemDO> saleOutItems) {
        saleOut.setTotalCount(getSumValue(saleOutItems, ErpSaleOutItemDO::getCount, BigDecimal::add));
        saleOut.setTotalProductPrice(getSumValue(saleOutItems, ErpSaleOutItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO));
        saleOut.setTotalTaxPrice(getSumValue(saleOutItems, ErpSaleOutItemDO::getTaxPrice, BigDecimal::add, BigDecimal.ZERO));
        saleOut.setTotalPrice(saleOut.getTotalProductPrice().add(saleOut.getTotalTaxPrice()));
        // 计算优惠价格
        if (saleOut.getDiscountPercent() == null) {
            saleOut.setDiscountPercent(BigDecimal.ZERO);
        }
        // otherPrice（其他费用）缺省按 0 处理：不传该字段时直接 add(null) 会 NPE（真机踩过），
        // 与上面 discountPercent 的缺省口径保持一致
        if (saleOut.getOtherPrice() == null) {
            saleOut.setOtherPrice(BigDecimal.ZERO);
        }
        saleOut.setDiscountPrice(MoneyUtils.priceMultiplyPercent(saleOut.getTotalPrice(), saleOut.getDiscountPercent()));
        saleOut.setTotalPrice(saleOut.getTotalPrice().subtract(saleOut.getDiscountPrice().add(saleOut.getOtherPrice())));
    }

    private void updateSaleOrderOutCount(Long orderId) {
        // 1.1 查询销售订单对应的销售出库单列表
        List<ErpSaleOutDO> saleOuts = saleOutMapper.selectListByOrderId(orderId);
        // 1.2 查询对应的销售订单项的出库数量
        Map<Long, BigDecimal> returnCountMap = saleOutItemMapper.selectOrderItemCountSumMapByOutIds(
                convertList(saleOuts, ErpSaleOutDO::getId));
        // 2. 更新销售订单的出库数量
        saleOrderService.updateSaleOrderOutCount(orderId, returnCountMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleOutStatus(Long id, Integer status) {
        if (!ErpAuditStatus.isValid(status)) {
            throw exception(ERP_AUDIT_STATUS_INVALID, status);
        }
        boolean approve = ErpAuditStatus.APPROVE.getStatus().equals(status);
        // 1.1 当前操作必须租户明确，且只能操作当前租户自己的单据；缺失或冲突时在副作用前拒绝。
        Long tenantId = requireCurrentTenantId();
        // 1.2 校验存在
        ErpSaleOutDO saleOut = validateSaleOutExists(id);
        validateSaleOutTenant(saleOut, tenantId);
        // 1.3 幂等处理：目标状态与当前状态一致时，返回同一业务结果
        if (status.equals(saleOut.getStatus())) {
            return;
        }
        // 1.4 校验已退款
        if (!approve && saleOut.getReceiptPrice().compareTo(BigDecimal.ZERO) > 0) {
            throw exception(SALE_OUT_PROCESS_FAIL_EXISTS_RECEIPT);
        }

        // 2. 更新状态
        int updateCount = saleOutMapper.updateByIdAndStatus(id, saleOut.getStatus(),
                new ErpSaleOutDO().setStatus(status));
        if (updateCount == 0) {
            ErpSaleOutDO latest = saleOutMapper.selectOneForUpdate(ErpSaleOutDO::getId, id);
            if (latest != null && status.equals(latest.getStatus())) {
                return;
            }
            throw exception(approve ? SALE_OUT_APPROVE_FAIL : SALE_OUT_PROCESS_FAIL);
        }

        // 3. 变更库存：按 (productId, warehouseId) 归并数量后逐组创建，减少库存更新往返
        List<ErpSaleOutItemDO> saleOutItems = saleOutItemMapper.selectListByOutId(id);
        Integer bizType = approve ? ErpStockRecordBizTypeEnum.SALE_OUT.getType()
                : ErpStockRecordBizTypeEnum.SALE_OUT_CANCEL.getType();
        Map<List<Long>, BigDecimal> mergedCountMap = new LinkedHashMap<>();
        Map<List<Long>, Long> firstItemIdMap = new LinkedHashMap<>();
        saleOutItems.forEach(saleOutItem -> {
            List<Long> key = List.of(saleOutItem.getProductId(), saleOutItem.getWarehouseId());
            BigDecimal count = approve ? saleOutItem.getCount().negate() : saleOutItem.getCount();
            mergedCountMap.merge(key, count, BigDecimal::add);
            firstItemIdMap.putIfAbsent(key, saleOutItem.getId());
        });
        mergedCountMap.forEach((key, count) -> stockRecordService.createStockRecord(new ErpStockRecordCreateReqBO(
                key.get(0), key.get(1), count,
                bizType, id, firstItemIdMap.get(key), saleOut.getNo())));

        // 4. 审核通过时生成销售出库收入与成本结转会计事件；事件与库存流水处于同一事务。
        // 反审核只对已审核单据生成冲销事件，避免从未过账的单据在驳回时被误冲销。
        if (approve) {
            accountingEventService.postEvent(buildSaleShipmentEvent(saleOut, saleOutItems));
        } else if (ErpAuditStatus.APPROVE.getStatus().equals(saleOut.getStatus())) {
            accountingEventService.reverseEvent(buildSaleShipmentReversalRequest(saleOut));
        }

        // 5. 审核通过时登记跨域事件（与库存流水、会计事件同事务）。
        //    为什么放在事务内而不是提交后：提交后登记会留下「业务成功但事件没登记」的窗口；
        //    同一事务内登记则随业务回滚一起消失，宁可少发也不要发出未发生的业务事实。
        //    这里只登记不投递：投递由可靠集成数据面（V100）的中继任务异步完成，失败重试并最终进死信。
        if (approve) {
            eventOutboxService.append(buildSaleOutApprovedEventAppendReq(saleOut, saleOutItems, tenantId));
        }
    }

    /**
     * 构造「ERP 销售出库审核通过」事件的登记请求。
     *
     * <p>eventId 由业务主键推导（{@code erp-sale-out-{id}-approved}）而不是随机数：
     * 同一单据重复触发时得到相同 eventId，消费侧才能靠 event_inbox 唯一键去重。
     *
     * <p>tenantId 使用审核时验证过的当前租户，禁止静默回退租户 0。
     */
    private EventAppendReq buildSaleOutApprovedEventAppendReq(ErpSaleOutDO saleOut,
                                                            List<ErpSaleOutItemDO> saleOutItems,
                                                            Long tenantId) {
        ErpSaleOutApprovedEvent event = ErpSaleOutApprovedEvent.builder()
                .no(saleOut.getNo())
                .saleOutId(saleOut.getId())
                .customerId(saleOut.getCustomerId())
                .outTime(saleOut.getOutTime())
                .totalPrice(saleOut.getTotalPrice())
                .items(convertList(saleOutItems, item -> ErpSaleOutApprovedEvent.Item.builder()
                        .productId(item.getProductId())
                        .warehouseId(item.getWarehouseId())
                        .count(item.getCount())
                        .price(item.getProductPrice())
                        .build()))
                .build();
        return EventAppendReq.builder()
                .eventId(ErpSaleOutApprovedEvent.eventIdOf(saleOut.getId()))
                .tenantId(tenantId)
                .aggregateType(ErpSaleOutApprovedEvent.AGGREGATE_TYPE)
                .aggregateId(String.valueOf(saleOut.getId()))
                .eventType(IntegrationEventTypes.ERP_SALE_OUT_APPROVED)
                .schemaVersion("v1")
                .payload(event.toJson())
                .occurredAt(saleOut.getOutTime() == null ? LocalDateTime.now() : saleOut.getOutTime())
                .build();
    }

    private Long requireCurrentTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw exception(EVENT_TENANT_REQUIRED);
        }
        return tenantId;
    }

    private void validateSaleOutTenant(ErpSaleOutDO saleOut, Long tenantId) {
        if (saleOut.getTenantId() == null || !tenantId.equals(saleOut.getTenantId())) {
            throw exception(EVENT_TENANT_MISMATCH);
        }
    }

    private ErpAccountingEventCreateReqBO buildSaleShipmentEvent(ErpSaleOutDO saleOut,
                                                                 List<ErpSaleOutItemDO> saleOutItems) {
        LocalDate eventDate = saleOut.getOutTime() == null
                ? LocalDate.now() : saleOut.getOutTime().toLocalDate();
        BigDecimal amountWithTax = defaultZero(saleOut.getTotalPrice());
        BigDecimal taxAmount = defaultZero(saleOut.getTotalTaxPrice());
        // 折扣和其他费用会改变价税合计，但当前单据不单独拆分其税额影响，先以最终价税合计倒算不含税金额
        BigDecimal amountWithoutTax = amountWithTax.subtract(taxAmount);
        return ErpAccountingEventCreateReqBO.builder()
                .eventType("SALES_SHIPMENT_POSTED")
                .sourceDocType("SALE_OUT")
                .sourceDocId(saleOut.getId())
                .sourceDocNo(saleOut.getNo())
                .sourceDocLineId(0L)
                .sourceRevision(1)
                .eventDate(eventDate)
                .accountingDate(eventDate)
                .amountWithoutTax(amountWithoutTax)
                .taxAmount(taxAmount)
                .amountWithTax(amountWithTax)
                .docTotalAmount(defaultZero(saleOut.getTotalPrice()))
                .costAmount(calculateStandardCost(saleOutItems))
                .currency(null)
                .exchangeRate(BigDecimal.ONE)
                .reversalOfEventId(0L)
                .occurredAt(saleOut.getOutTime() == null ? LocalDateTime.now() : saleOut.getOutTime())
                .build();
    }

    private ErpAccountingEventReverseReqBO buildSaleShipmentReversalRequest(ErpSaleOutDO saleOut) {
        LocalDate eventDate = saleOut.getOutTime() == null
                ? LocalDate.now() : saleOut.getOutTime().toLocalDate();
        return ErpAccountingEventReverseReqBO.builder()
                .eventType("SALES_SHIPMENT_POSTED")
                .sourceDocType("SALE_OUT")
                .sourceDocId(saleOut.getId())
                .sourceDocLineId(0L)
                .sourceRevision(1)
                .eventDate(eventDate)
                // 冲销记在当前会计期，不回写原会计期
                .accountingDate(LocalDate.now())
                .occurredAt(LocalDateTime.now())
                .build();
    }

    /**
     * 按产品采购价计算出库成本。
     *
     * <p>这是标准成本占位实现：项目当前没有存货计价（移动加权平均或批次成本）底座，
     * 只能用产品主数据的采购价近似。上线前必须替换为存货计价结果，否则销售成本与
     * 存货账面价值不可用于正式报表。
     */
    private BigDecimal calculateStandardCost(List<ErpSaleOutItemDO> saleOutItems) {
        if (CollUtil.isEmpty(saleOutItems)) {
            return BigDecimal.ZERO;
        }
        BigDecimal cost = BigDecimal.ZERO;
        for (ErpSaleOutItemDO item : saleOutItems) {
            BigDecimal count = defaultZero(item.getCount());
            if (count.compareTo(BigDecimal.ZERO) == 0 || item.getProductId() == null) {
                continue;
            }
            ErpProductDO product = productService.getProduct(item.getProductId());
            BigDecimal unitCost = product == null ? BigDecimal.ZERO : defaultZero(product.getPurchasePrice());
            cost = cost.add(count.multiply(unitCost));
        }
        return cost;
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    @Override
    public void updateSaleInReceiptPrice(Long id, BigDecimal receiptPrice) {
        ErpSaleOutDO saleOut = saleOutMapper.selectById(id);
        // receipt_price 建单从不写（V141 时代的列无默认值），首次回款时为 null——
        // 按未回款(0)处理，否则第一笔回款就在这里 NPE（真机踩过）
        if (defaultZero(saleOut.getReceiptPrice()).equals(receiptPrice)) {
            return;
        }
        if (receiptPrice.compareTo(saleOut.getTotalPrice()) > 0) {
            throw exception(SALE_OUT_FAIL_RECEIPT_PRICE_EXCEED, receiptPrice,  saleOut.getTotalPrice());
        }
        saleOutMapper.updateById(new ErpSaleOutDO().setId(id).setReceiptPrice(receiptPrice));
    }

    private List<ErpSaleOutItemDO> validateSaleOutItems(List<ErpSaleOutSaveReqVO.Item> list) {
        // 1. 校验产品存在
        List<ErpProductDO> productList = productService.validProductList(
                convertSet(list, ErpSaleOutSaveReqVO.Item::getProductId));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        // 2. 转化为 ErpSaleOutItemDO 列表
        return convertList(list, o -> BeanUtils.toBean(o, ErpSaleOutItemDO.class, item -> {
            item.setProductUnitId(productMap.get(item.getProductId()).getUnitId());
            item.setTotalPrice(MoneyUtils.priceMultiply(item.getProductPrice(), item.getCount()));
            if (item.getTotalPrice() == null) {
                return;
            }
            if (item.getTaxPercent() != null) {
                item.setTaxPrice(MoneyUtils.priceMultiplyPercent(item.getTotalPrice(), item.getTaxPercent()));
            }
        }));
    }

    private void updateSaleOutItemList(Long id, List<ErpSaleOutItemDO> newList) {
        // 第一步，对比新老数据，获得添加、修改、删除的列表
        List<ErpSaleOutItemDO> oldList = saleOutItemMapper.selectListByOutId(id);
        List<List<ErpSaleOutItemDO>> diffList = diffList(oldList, newList, // id 不同，就认为是不同的记录
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));

        // 第二步，批量添加、修改、删除
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setOutId(id));
            saleOutItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            saleOutItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            saleOutItemMapper.deleteByIds(convertList(diffList.get(2), ErpSaleOutItemDO::getId));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSaleOut(List<Long> ids) {
        // 1. 校验不处于已审批
        List<ErpSaleOutDO> saleOuts = saleOutMapper.selectByIds(ids);
        if (CollUtil.isEmpty(saleOuts)) {
            return;
        }
        saleOuts.forEach(saleOut -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(saleOut.getStatus())) {
                throw exception(SALE_OUT_DELETE_FAIL_APPROVE, saleOut.getNo());
            }
        });

        // 2. 遍历删除，并记录操作日志
        saleOuts.forEach(saleOut -> {
            // 2.1 删除订单
            saleOutMapper.deleteById(saleOut.getId());
            // 2.2 删除订单项
            saleOutItemMapper.deleteByOutId(saleOut.getId());

            // 2.3 更新销售订单的出库数量
            updateSaleOrderOutCount(saleOut.getOrderId());
        });

    }

    private ErpSaleOutDO validateSaleOutExists(Long id) {
        ErpSaleOutDO saleOut = saleOutMapper.selectById(id);
        if (saleOut == null) {
            throw exception(SALE_OUT_NOT_EXISTS);
        }
        return saleOut;
    }

    @Override
    public ErpSaleOutDO getSaleOut(Long id) {
        return saleOutMapper.selectById(id);
    }

    @Override
    public ErpSaleOutDO validateSaleOut(Long id) {
        ErpSaleOutDO saleOut = validateSaleOutExists(id);
        if (ObjectUtil.notEqual(saleOut.getStatus(), ErpAuditStatus.APPROVE.getStatus())) {
            throw exception(SALE_OUT_NOT_APPROVE);
        }
        return saleOut;
    }

    @Override
    public PageResult<ErpSaleOutDO> getSaleOutPage(ErpSaleOutPageReqVO pageReqVO) {
        return saleOutMapper.selectPage(pageReqVO);
    }

    // ==================== 销售出库项 ====================

    @Override
    public List<ErpSaleOutItemDO> getSaleOutItemListByOutId(Long outId) {
        return saleOutItemMapper.selectListByOutId(outId);
    }

    @Override
    public List<ErpSaleOutItemDO> getSaleOutItemListByOutIds(Collection<Long> outIds) {
        if (CollUtil.isEmpty(outIds)) {
            return Collections.emptyList();
        }
        return saleOutItemMapper.selectListByOutIds(outIds);
    }

}
