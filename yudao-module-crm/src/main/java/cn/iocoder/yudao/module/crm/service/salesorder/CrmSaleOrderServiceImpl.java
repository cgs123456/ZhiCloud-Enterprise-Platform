package cn.iocoder.yudao.module.crm.service.salesorder;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.crm.controller.admin.salesorder.vo.CrmSaleOrderPageReqVO;
import cn.iocoder.yudao.module.crm.controller.admin.salesorder.vo.CrmSaleOrderSaveReqVO;
import cn.iocoder.yudao.module.crm.dal.dataobject.salesorder.CrmSaleOrderDO;
import cn.iocoder.yudao.module.crm.dal.dataobject.salesorder.CrmSaleOrderItemDO;
import cn.iocoder.yudao.module.crm.dal.mysql.salesorder.CrmSaleOrderItemMapper;
import cn.iocoder.yudao.module.crm.dal.mysql.salesorder.CrmSaleOrderMapper;
import cn.iocoder.yudao.module.crm.dal.redis.no.CrmNoRedisDAO;
import cn.iocoder.yudao.module.crm.enums.salesorder.CrmSaleOrderDeliveryStatusEnum;
import cn.iocoder.yudao.module.crm.enums.salesorder.CrmSaleOrderPaymentStatusEnum;
import cn.iocoder.yudao.module.crm.enums.salesorder.CrmSaleOrderStatusEnum;
import cn.iocoder.yudao.module.crm.service.contract.CrmContractService;
import cn.iocoder.yudao.module.crm.service.customer.CrmCustomerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.getSumValue;
import static cn.iocoder.yudao.module.crm.enums.ErrorCodeConstants.*;

/**
 * CRM 销售订单 Service 实现类
 *
 * @author dhb52
 */
@Service
@Validated
@Slf4j
public class CrmSaleOrderServiceImpl implements CrmSaleOrderService {

    @Resource
    private CrmSaleOrderMapper saleOrderMapper;
    @Resource
    private CrmSaleOrderItemMapper saleOrderItemMapper;

    @Resource
    private CrmNoRedisDAO noRedisDAO;

    @Resource
    @Lazy
    private CrmCustomerService customerService;
    @Resource
    @Lazy
    private CrmContractService contractService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSaleOrder(CrmSaleOrderSaveReqVO createReqVO, Long userId) {
        // 1.1 校验关联数据
        validateRelationDataExists(createReqVO);
        // 1.2 生成序号
        String no = noRedisDAO.generate(CrmNoRedisDAO.SALE_ORDER_NO_PREFIX);
        if (saleOrderMapper.selectByNo(no) != null) {
            throw exception(SALE_ORDER_NO_EXISTS);
        }

        // 2.1 计算订单明细金额
        List<CrmSaleOrderItemDO> items = calculateOrderItems(createReqVO.getItems());
        // 2.2 插入订单
        CrmSaleOrderDO order = BeanUtils.toBean(createReqVO, CrmSaleOrderDO.class).setNo(no);
        calculateOrderAmount(order, items);
        order.setStatus(CrmSaleOrderStatusEnum.DRAFT.getStatus())
                .setPaymentStatus(CrmSaleOrderPaymentStatusEnum.UNPAID.getStatus())
                .setDeliveryStatus(CrmSaleOrderDeliveryStatusEnum.UN_SHIPPED.getStatus());
        saleOrderMapper.insert(order);
        // 2.3 插入订单明细
        if (CollUtil.isNotEmpty(items)) {
            items.forEach(item -> item.setOrderId(order.getId()));
            saleOrderItemMapper.insertBatch(items);
        }
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleOrder(CrmSaleOrderSaveReqVO updateReqVO) {
        Assert.notNull(updateReqVO.getId(), "销售订单编号不能为空");
        // 1.1 校验存在
        CrmSaleOrderDO oldOrder = validateSaleOrderExists(updateReqVO.getId());
        // 1.2 只有草稿状态，可以编辑
        if (ObjUtil.notEqual(oldOrder.getStatus(), CrmSaleOrderStatusEnum.DRAFT.getStatus())) {
            throw exception(SALE_ORDER_UPDATE_FAIL_NOT_DRAFT);
        }
        // 1.3 校验关联数据
        validateRelationDataExists(updateReqVO);

        // 2.1 计算订单明细金额
        List<CrmSaleOrderItemDO> items = calculateOrderItems(updateReqVO.getItems());
        // 2.2 更新订单
        CrmSaleOrderDO updateObj = BeanUtils.toBean(updateReqVO, CrmSaleOrderDO.class);
        calculateOrderAmount(updateObj, items);
        saleOrderMapper.updateById(updateObj);
        // 2.3 更新订单明细：先删后插
        saleOrderItemMapper.deleteByOrderId(updateReqVO.getId());
        if (CollUtil.isNotEmpty(items)) {
            items.forEach(item -> item.setOrderId(updateReqVO.getId()));
            saleOrderItemMapper.insertBatch(items);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSaleOrder(Long id) {
        // 1.1 校验存在
        CrmSaleOrderDO order = validateSaleOrderExists(id);
        // 1.2 只有草稿状态，可以删除
        if (ObjUtil.notEqual(order.getStatus(), CrmSaleOrderStatusEnum.DRAFT.getStatus())) {
            throw exception(SALE_ORDER_DELETE_FAIL);
        }
        // 2.1 删除订单
        saleOrderMapper.deleteById(id);
        // 2.2 删除订单明细
        saleOrderItemMapper.deleteByOrderId(id);
    }

    private CrmSaleOrderDO validateSaleOrderExists(Long id) {
        CrmSaleOrderDO order = saleOrderMapper.selectById(id);
        if (order == null) {
            throw exception(SALE_ORDER_NOT_EXISTS);
        }
        return order;
    }

    /**
     * 校验关联数据是否存在
     */
    private void validateRelationDataExists(CrmSaleOrderSaveReqVO reqVO) {
        // 1. 校验客户
        if (reqVO.getCustomerId() != null) {
            customerService.validateCustomer(reqVO.getCustomerId());
        }
        // 2. 如果有关联合同，则需要校验存在
        if (reqVO.getContractId() != null) {
            contractService.validateContract(reqVO.getContractId());
        }
    }

    /**
     * 计算订单明细金额：amount = (unitPrice * quantity) - discount；taxAmount = amount * taxRate
     */
    private List<CrmSaleOrderItemDO> calculateOrderItems(List<CrmSaleOrderSaveReqVO.Item> list) {
        if (CollUtil.isEmpty(list)) {
            return List.of();
        }
        return convertList(list, o -> {
            CrmSaleOrderItemDO item = BeanUtils.toBean(o, CrmSaleOrderItemDO.class);
            // 金额 = 单价 * 数量 - 折扣
            BigDecimal baseAmount = MoneyUtils.priceMultiply(item.getUnitPrice(), item.getQuantity());
            BigDecimal discount = item.getDiscount() == null ? BigDecimal.ZERO : item.getDiscount();
            BigDecimal amount = baseAmount.subtract(discount);
            item.setAmount(amount);
            // 税额 = 金额 * 税率
            if (item.getTaxRate() != null) {
                item.setTaxAmount(MoneyUtils.priceMultiply(amount, item.getTaxRate()));
            }
            return item;
        });
    }

    /**
     * 计算订单金额：totalAmount = sum(items.amount)；finalAmount = totalAmount - discountAmount
     */
    private void calculateOrderAmount(CrmSaleOrderDO order, List<CrmSaleOrderItemDO> items) {
        BigDecimal totalAmount = getSumValue(items, CrmSaleOrderItemDO::getAmount, BigDecimal::add, BigDecimal.ZERO);
        order.setTotalAmount(totalAmount);
        BigDecimal discountAmount = order.getDiscountAmount() == null ? BigDecimal.ZERO : order.getDiscountAmount();
        order.setFinalAmount(totalAmount.subtract(discountAmount));
    }

    @Override
    public CrmSaleOrderDO getSaleOrder(Long id) {
        return saleOrderMapper.selectById(id);
    }

    @Override
    public CrmSaleOrderDO validateSaleOrder(Long id) {
        return validateSaleOrderExists(id);
    }

    @Override
    public PageResult<CrmSaleOrderDO> getSaleOrderPage(CrmSaleOrderPageReqVO pageReqVO) {
        return saleOrderMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmSaleOrder(Long id) {
        // 1. 校验存在
        CrmSaleOrderDO order = validateSaleOrderExists(id);
        // 2. 只有草稿状态，可以确认
        if (ObjUtil.notEqual(order.getStatus(), CrmSaleOrderStatusEnum.DRAFT.getStatus())) {
            throw exception(SALE_ORDER_CONFIRM_FAIL_NOT_DRAFT);
        }
        // 3. 更新状态为已确认
        saleOrderMapper.updateById(new CrmSaleOrderDO().setId(id)
                .setStatus(CrmSaleOrderStatusEnum.CONFIRMED.getStatus()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitSaleOrder(Long id, Long userId) {
        // 1. 校验存在
        CrmSaleOrderDO order = validateSaleOrderExists(id);
        // 2. 只有草稿状态，可以提交审核（暂不接入 BPM，直接更新为已确认）
        if (ObjUtil.notEqual(order.getStatus(), CrmSaleOrderStatusEnum.DRAFT.getStatus())) {
            throw exception(SALE_ORDER_SUBMIT_FAIL_NOT_DRAFT);
        }
        // 3. 更新状态为已确认
        saleOrderMapper.updateById(new CrmSaleOrderDO().setId(id)
                .setStatus(CrmSaleOrderStatusEnum.CONFIRMED.getStatus()));
    }

    @Override
    public PageResult<CrmSaleOrderDO> getSaleOrderPageByContractId(CrmSaleOrderPageReqVO pageReqVO) {
        Assert.notNull(pageReqVO.getContractId(), "合同编号不能为空");
        return saleOrderMapper.selectPageByContractId(pageReqVO);
    }

    @Override
    public PageResult<CrmSaleOrderDO> getSaleOrderPageByCustomerId(CrmSaleOrderPageReqVO pageReqVO) {
        Assert.notNull(pageReqVO.getCustomerId(), "客户编号不能为空");
        return saleOrderMapper.selectPageByCustomerId(pageReqVO);
    }

    @Override
    public List<CrmSaleOrderItemDO> getSaleOrderItemListByOrderId(Long orderId) {
        return saleOrderItemMapper.selectListByOrderId(orderId);
    }

    @Override
    public Map<Long, BigDecimal> getSaleOrderPriceMapByContractId(Collection<Long> contractIds) {
        return saleOrderMapper.selectSaleOrderPriceMapByContractId(contractIds);
    }

}
