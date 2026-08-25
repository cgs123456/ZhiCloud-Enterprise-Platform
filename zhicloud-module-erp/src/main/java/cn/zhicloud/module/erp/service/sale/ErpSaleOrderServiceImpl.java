package cn.zhicloud.module.erp.service.sale;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.number.MoneyUtils;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.sale.vo.order.ErpSaleOrderPageReqVO;
import cn.zhicloud.module.erp.controller.admin.sale.vo.order.ErpSaleOrderSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.product.ErpProductDO;
import cn.zhicloud.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.zhicloud.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.zhicloud.module.erp.dal.dataobject.sale.ErpSaleOrderItemDO;
import cn.zhicloud.module.erp.dal.mysql.sale.ErpSaleOrderItemMapper;
import cn.zhicloud.module.erp.dal.mysql.sale.ErpSaleOrderMapper;
import cn.zhicloud.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.zhicloud.module.erp.enums.ErpAuditStatus;
import cn.zhicloud.module.erp.service.finance.ErpAccountService;
import cn.zhicloud.module.erp.service.finance.ErpMultiCurrencyService;
import cn.zhicloud.module.erp.service.product.ErpProductService;
import cn.zhicloud.module.erp.service.sale.credit.ErpCreditCheckService;
import cn.zhicloud.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.*;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.*;

// 操作日志已通过 @OperateLog 注解自动记录（bizlog-sdk 通过 AOP 自动记录）

/**
 * ERP 销售订单 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class ErpSaleOrderServiceImpl implements ErpSaleOrderService {

    @Resource
    private ErpSaleOrderMapper saleOrderMapper;
    @Resource
    private ErpSaleOrderItemMapper saleOrderItemMapper;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpProductService productService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpCustomerCreditService customerCreditService;
    @Resource
    private ErpCreditCheckService creditCheckService;
    @Resource
    private ErpAccountService accountService;
    @Resource
    private ErpMultiCurrencyService multiCurrencyService;

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSaleOrder(ErpSaleOrderSaveReqVO createReqVO) {
        // 1.1 校验订单项的有效性
        List<ErpSaleOrderItemDO> saleOrderItems = validateSaleOrderItems(createReqVO.getItems());
        // 1.2 校验客户
        customerService.validateCustomer(createReqVO.getCustomerId());
        // 1.3 校验结算账户
        if (createReqVO.getAccountId() != null) {
            accountService.validateAccount(createReqVO.getAccountId());
        }
        // 1.4 校验销售人员
        if (createReqVO.getSaleUserId() != null) {
            adminUserApi.validateUser(createReqVO.getSaleUserId());
        }
        // 1.5 生成订单号，并校验唯一性
        String no = noRedisDAO.generate(ErpNoRedisDAO.SALE_ORDER_NO_PREFIX);
        if (saleOrderMapper.selectByNo(no) != null) {
            throw exception(SALE_ORDER_NO_EXISTS);
        }

        // 2.1 插入订单
        ErpSaleOrderDO saleOrder = BeanUtils.toBean(createReqVO, ErpSaleOrderDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus()));
        calculateTotalPrice(saleOrder, saleOrderItems);
        // 2.1.1 P0-2 销售信用控制：调用 ErpCreditCheckService 校验客户信用额度是否充足
        BigDecimal orderAmount = saleOrder.getTotalPrice() == null ? BigDecimal.ZERO : saleOrder.getTotalPrice();
        creditCheckService.checkCredit(createReqVO.getCustomerId(), orderAmount);
        saleOrderMapper.insert(saleOrder);
        // 2.2 插入订单项
        saleOrderItems.forEach(o -> o.setOrderId(saleOrder.getId()));
        saleOrderItemMapper.insertBatch(saleOrderItems);
        return saleOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleOrder(ErpSaleOrderSaveReqVO updateReqVO) {
        // 1.1 校验存在
        ErpSaleOrderDO saleOrder = validateSaleOrderExists(updateReqVO.getId());
        if (ErpAuditStatus.APPROVE.getStatus().equals(saleOrder.getStatus())) {
            throw exception(SALE_ORDER_UPDATE_FAIL_APPROVE, saleOrder.getNo());
        }
        // 1.2 校验客户
        customerService.validateCustomer(updateReqVO.getCustomerId());
        // 1.3 校验结算账户
        if (updateReqVO.getAccountId() != null) {
            accountService.validateAccount(updateReqVO.getAccountId());
        }
        // 1.4 校验销售人员
        if (updateReqVO.getSaleUserId() != null) {
            adminUserApi.validateUser(updateReqVO.getSaleUserId());
        }
        // 1.5 校验订单项的有效性
        List<ErpSaleOrderItemDO> saleOrderItems = validateSaleOrderItems(updateReqVO.getItems());

        // 2.1 更新订单
        ErpSaleOrderDO updateObj = BeanUtils.toBean(updateReqVO, ErpSaleOrderDO.class);
        calculateTotalPrice(updateObj, saleOrderItems);
        saleOrderMapper.updateById(updateObj);
        // 2.2 更新订单项
        updateSaleOrderItemList(updateReqVO.getId(), saleOrderItems);
    }

    private void calculateTotalPrice(ErpSaleOrderDO saleOrder, List<ErpSaleOrderItemDO> saleOrderItems) {
        saleOrder.setTotalCount(getSumValue(saleOrderItems, ErpSaleOrderItemDO::getCount, BigDecimal::add));
        saleOrder.setTotalProductPrice(getSumValue(saleOrderItems, ErpSaleOrderItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO));
        saleOrder.setTotalTaxPrice(getSumValue(saleOrderItems, ErpSaleOrderItemDO::getTaxPrice, BigDecimal::add, BigDecimal.ZERO));
        saleOrder.setTotalPrice(saleOrder.getTotalProductPrice().add(saleOrder.getTotalTaxPrice()));
        // 计算优惠价格
        if (saleOrder.getDiscountPercent() == null) {
            saleOrder.setDiscountPercent(BigDecimal.ZERO);
        }
        saleOrder.setDiscountPrice(MoneyUtils.priceMultiplyPercent(saleOrder.getTotalPrice(), saleOrder.getDiscountPercent()));
        saleOrder.setTotalPrice(saleOrder.getTotalPrice().subtract(saleOrder.getDiscountPrice()));
        // P0-8：多币种折算
        applyMultiCurrency(saleOrder);
    }

    /**
     * P0-8：根据币种 ID 查询汇率并计算本位币金额
     *
     * <p>若 currencyId 为空，自动填充为本位币 ID，汇率固定为 1；
     * 若 currencyId 与本位币相同，汇率固定为 1；
     * 若 currencyId 为外币，查询 erp_exchange_rate 取最新有效汇率，找不到则抛业务异常。
     *
     * @param order 销售订单
     */
    private void applyMultiCurrency(ErpSaleOrderDO order) {
        Long baseCurrencyId = multiCurrencyService.getBaseCurrencyId();
        if (order.getCurrencyId() == null) {
            order.setCurrencyId(baseCurrencyId);
        }
        BigDecimal rate;
        if (baseCurrencyId != null && Objects.equals(order.getCurrencyId(), baseCurrencyId)) {
            rate = BigDecimal.ONE;
        } else {
            rate = multiCurrencyService.getEffectiveRate(order.getCurrencyId(), LocalDate.now());
            if (rate == null) {
                throw exception(EXCHANGE_RATE_NOT_FOUND, order.getCurrencyId(), baseCurrencyId, LocalDate.now());
            }
        }
        order.setExchangeRate(rate);
        if (order.getTotalPrice() != null) {
            order.setBaseCurrencyTotalPrice(MoneyUtils.priceMultiply(order.getTotalPrice(), rate));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleOrderStatus(Long id, Integer status) {
        boolean approve = ErpAuditStatus.APPROVE.getStatus().equals(status);
        // 1.1 校验存在
        ErpSaleOrderDO saleOrder = validateSaleOrderExists(id);
        // 1.2 校验状态
        if (saleOrder.getStatus().equals(status)) {
            throw exception(approve ? SALE_ORDER_APPROVE_FAIL : SALE_ORDER_PROCESS_FAIL);
        }
        // 1.3 存在销售出库单，无法反审核
        if (!approve && saleOrder.getOutCount().compareTo(BigDecimal.ZERO) > 0) {
            throw exception(SALE_ORDER_PROCESS_FAIL_EXISTS_OUT);
        }
        // 1.4 存在销售退货单，无法反审核
        if (!approve && saleOrder.getReturnCount().compareTo(BigDecimal.ZERO) > 0) {
            throw exception(SALE_ORDER_PROCESS_FAIL_EXISTS_RETURN);
        }
        // 1.5 P0-2 信用额度校验（审核前）：订单总金额 + 客户已用额度 ≤ 信用额度
        if (approve) {
            validateSaleOrderCredit(saleOrder);
        }

        // 2. 更新状态
        int updateCount = saleOrderMapper.updateByIdAndStatus(id, saleOrder.getStatus(),
                new ErpSaleOrderDO().setStatus(status));
        if (updateCount == 0) {
            throw exception(approve ? SALE_ORDER_APPROVE_FAIL : SALE_ORDER_PROCESS_FAIL);
        }

        // 3. P0-2 信用额度锁定/释放（审核通过锁定，反审核释放）
        BigDecimal creditAmount = saleOrder.getTotalPrice() == null ? BigDecimal.ZERO : saleOrder.getTotalPrice();
        if (approve) {
            customerCreditService.lockCredit(saleOrder.getCustomerId(), creditAmount);
        } else {
            customerCreditService.releaseCredit(saleOrder.getCustomerId(), creditAmount);
        }
    }

    /**
     * P0-2 校验销售订单信用额度：订单总金额 + 客户已用额度 ≤ 信用额度，超出抛 SALE_ORDER_CREDIT_EXCEED
     *
     * @param saleOrder 销售订单
     */
    private void validateSaleOrderCredit(ErpSaleOrderDO saleOrder) {
        ErpCustomerDO customer = customerService.getCustomer(saleOrder.getCustomerId());
        if (customer == null) {
            return;
        }
        BigDecimal creditLimit = customer.getCreditLimit() == null ? BigDecimal.ZERO : customer.getCreditLimit();
        // 未设置信用额度（<=0）视为无限额度，不校验
        if (creditLimit.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal usedCredit = customer.getUsedCredit() == null ? BigDecimal.ZERO : customer.getUsedCredit();
        BigDecimal orderAmount = saleOrder.getTotalPrice() == null ? BigDecimal.ZERO : saleOrder.getTotalPrice();
        BigDecimal total = usedCredit.add(orderAmount);
        if (total.compareTo(creditLimit) > 0) {
            throw exception(SALE_ORDER_CREDIT_EXCEED, saleOrder.getNo(), orderAmount,
                    customer.getName(), usedCredit, creditLimit);
        }
    }

    private List<ErpSaleOrderItemDO> validateSaleOrderItems(List<ErpSaleOrderSaveReqVO.Item> list) {
        // 1. 校验产品存在
        List<ErpProductDO> productList = productService.validProductList(
                convertSet(list, ErpSaleOrderSaveReqVO.Item::getProductId));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        // 2. 转化为 ErpSaleOrderItemDO 列表
        return convertList(list, o -> BeanUtils.toBean(o, ErpSaleOrderItemDO.class, item -> {
            ErpProductDO product = productMap.get(item.getProductId());
            item.setProductUnitId(product != null ? product.getUnitId() : null);
            item.setTotalPrice(MoneyUtils.priceMultiply(item.getProductPrice(), item.getCount()));
            if (item.getTotalPrice() == null) {
                return;
            }
            if (item.getTaxPercent() != null) {
                item.setTaxPrice(MoneyUtils.priceMultiplyPercent(item.getTotalPrice(), item.getTaxPercent()));
            }
        }));
    }

    private void updateSaleOrderItemList(Long id, List<ErpSaleOrderItemDO> newList) {
        // 第一步，对比新老数据，获得添加、修改、删除的列表
        List<ErpSaleOrderItemDO> oldList = saleOrderItemMapper.selectListByOrderId(id);
        List<List<ErpSaleOrderItemDO>> diffList = diffList(oldList, newList, // id 不同，就认为是不同的记录
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));

        // 第二步，批量添加、修改、删除
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setOrderId(id));
            saleOrderItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            saleOrderItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            saleOrderItemMapper.deleteByIds(convertList(diffList.get(2), ErpSaleOrderItemDO::getId));
        }
    }

    @Override
    // 原子性修复：循环内逐项 updateById，若中途抛 SALE_ORDER_ITEM_OUT_FAIL_PRODUCT_EXCEED，
    // 已更新的订单项不回滚且订单头 outCount 不写入 => 订单头与订单项出库数撕裂。
    // 现有调用方 ErpSaleOutServiceImpl 已开启事务，此处 REQUIRED 复用外层事务，属防御性加固。
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleOrderOutCount(Long id, Map<Long, BigDecimal> outCountMap) {
        List<ErpSaleOrderItemDO> orderItems = saleOrderItemMapper.selectListByOrderId(id);
        // 1. 更新每个销售订单项
        orderItems.forEach(item -> {
            BigDecimal outCount = outCountMap.getOrDefault(item.getId(), BigDecimal.ZERO);
            BigDecimal itemOutCount = ObjectUtil.defaultIfNull(item.getOutCount(), BigDecimal.ZERO);
            if (itemOutCount.equals(outCount)) {
                return;
            }
            if (outCount.compareTo(ObjectUtil.defaultIfNull(item.getCount(), BigDecimal.ZERO)) > 0) {
                ErpProductDO product = productService.getProduct(item.getProductId());
                throw exception(SALE_ORDER_ITEM_OUT_FAIL_PRODUCT_EXCEED,
                        product != null ? product.getName() : item.getProductId(), item.getCount());
            }
            saleOrderItemMapper.updateById(new ErpSaleOrderItemDO().setId(item.getId()).setOutCount(outCount));
        });
        // 2. 更新销售订单
        BigDecimal totalOutCount = getSumValue(outCountMap.values(), value -> value, BigDecimal::add, BigDecimal.ZERO);
        saleOrderMapper.updateById(new ErpSaleOrderDO().setId(id).setOutCount(totalOutCount));
    }

    @Override
    // 原子性修复：同 updateSaleOrderOutCount，避免订单项 returnCount 与订单头 returnCount 撕裂。
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleOrderReturnCount(Long orderId, Map<Long, BigDecimal> returnCountMap) {
        List<ErpSaleOrderItemDO> orderItems = saleOrderItemMapper.selectListByOrderId(orderId);
        // 1. 更新每个销售订单项
        orderItems.forEach(item -> {
            BigDecimal returnCount = returnCountMap.getOrDefault(item.getId(), BigDecimal.ZERO);
            BigDecimal itemReturnCount = ObjectUtil.defaultIfNull(item.getReturnCount(), BigDecimal.ZERO);
            if (itemReturnCount.equals(returnCount)) {
                return;
            }
            if (returnCount.compareTo(ObjectUtil.defaultIfNull(item.getOutCount(), BigDecimal.ZERO)) > 0) {
                ErpProductDO product = productService.getProduct(item.getProductId());
                throw exception(SALE_ORDER_ITEM_RETURN_FAIL_OUT_EXCEED,
                        product != null ? product.getName() : item.getProductId(), item.getOutCount());
            }
            saleOrderItemMapper.updateById(new ErpSaleOrderItemDO().setId(item.getId()).setReturnCount(returnCount));
        });
        // 2. 更新销售订单
        BigDecimal totalReturnCount = getSumValue(returnCountMap.values(), value -> value, BigDecimal::add, BigDecimal.ZERO);
        saleOrderMapper.updateById(new ErpSaleOrderDO().setId(orderId).setReturnCount(totalReturnCount));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSaleOrder(List<Long> ids) {
        // 1. 校验不处于已审批
        List<ErpSaleOrderDO> saleOrders = saleOrderMapper.selectByIds(ids);
        if (CollUtil.isEmpty(saleOrders)) {
            return;
        }
        saleOrders.forEach(saleOrder -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(saleOrder.getStatus())) {
                throw exception(SALE_ORDER_DELETE_FAIL_APPROVE, saleOrder.getNo());
            }
        });

        // 2. 遍历删除，并记录操作日志
        saleOrders.forEach(saleOrder -> {
            // 2.1 删除订单
            saleOrderMapper.deleteById(saleOrder.getId());
            // 2.2 删除订单项
            saleOrderItemMapper.deleteByOrderId(saleOrder.getId());
        });
    }

    private ErpSaleOrderDO validateSaleOrderExists(Long id) {
        ErpSaleOrderDO saleOrder = saleOrderMapper.selectById(id);
        if (saleOrder == null) {
            throw exception(SALE_ORDER_NOT_EXISTS);
        }
        return saleOrder;
    }

    @Override
    public ErpSaleOrderDO getSaleOrder(Long id) {
        return saleOrderMapper.selectById(id);
    }

    @Override
    public ErpSaleOrderDO validateSaleOrder(Long id) {
        ErpSaleOrderDO saleOrder = validateSaleOrderExists(id);
        if (ObjectUtil.notEqual(saleOrder.getStatus(), ErpAuditStatus.APPROVE.getStatus())) {
            throw exception(SALE_ORDER_NOT_APPROVE);
        }
        return saleOrder;
    }

    @Override
    public PageResult<ErpSaleOrderDO> getSaleOrderPage(ErpSaleOrderPageReqVO pageReqVO) {
        return saleOrderMapper.selectPage(pageReqVO);
    }

    // ==================== 订单项 ====================

    @Override
    public List<ErpSaleOrderItemDO> getSaleOrderItemListByOrderId(Long orderId) {
        return saleOrderItemMapper.selectListByOrderId(orderId);
    }

    @Override
    public List<ErpSaleOrderItemDO> getSaleOrderItemListByOrderIds(Collection<Long> orderIds) {
        if (CollUtil.isEmpty(orderIds)) {
            return Collections.emptyList();
        }
        return saleOrderItemMapper.selectListByOrderIds(orderIds);
    }

}
