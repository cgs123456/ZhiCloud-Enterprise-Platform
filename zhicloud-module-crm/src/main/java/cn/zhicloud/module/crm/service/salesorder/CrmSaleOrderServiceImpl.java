package cn.zhicloud.module.crm.service.salesorder;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjUtil;
import cn.zhicloud.framework.common.exception.ErrorCode;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.number.MoneyUtils;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.crm.controller.admin.salesorder.vo.CrmSaleOrderPageReqVO;
import cn.zhicloud.module.crm.controller.admin.salesorder.vo.CrmSaleOrderSaveReqVO;
import cn.zhicloud.module.crm.dal.dataobject.salesorder.CrmSaleOrderDO;
import cn.zhicloud.module.crm.dal.dataobject.salesorder.CrmSaleOrderItemDO;
import cn.zhicloud.module.crm.dal.mysql.salesorder.CrmSaleOrderItemMapper;
import cn.zhicloud.module.crm.dal.mysql.salesorder.CrmSaleOrderMapper;
import cn.zhicloud.module.crm.dal.redis.no.CrmNoRedisDAO;
import cn.zhicloud.module.crm.enums.salesorder.CrmSaleOrderDeliveryStatusEnum;
import cn.zhicloud.module.crm.enums.salesorder.CrmSaleOrderPaymentStatusEnum;
import cn.zhicloud.module.crm.enums.salesorder.CrmSaleOrderStatusEnum;
import cn.zhicloud.module.crm.service.contract.CrmContractService;
import cn.zhicloud.module.crm.service.customer.CrmCustomerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertList;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.getSumValue;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.*;

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
        // 只有草稿状态，可以确认（CAS 条件更新，防并发覆盖/复活已取消订单）
        transitionStatus(id, SALE_ORDER_CONFIRM_FAIL_NOT_DRAFT,
                new CrmSaleOrderDO().setStatus(CrmSaleOrderStatusEnum.CONFIRMED.getStatus()),
                CrmSaleOrderStatusEnum.DRAFT.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitSaleOrder(Long id, Long userId) {
        // 暂不接入 BPM，直接更新为已确认（与 confirm 同一迁移，保留独立端点供后续接流程；CAS 同 confirm）
        transitionStatus(id, SALE_ORDER_SUBMIT_FAIL_NOT_DRAFT,
                new CrmSaleOrderDO().setStatus(CrmSaleOrderStatusEnum.CONFIRMED.getStatus()),
                CrmSaleOrderStatusEnum.DRAFT.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipSaleOrder(Long id) {
        // 已确认 -> 已发货；发货状态同步为「已发货」（全量发货，无部分量跟踪）
        transitionStatus(id, SALE_ORDER_SHIP_FAIL_NOT_CONFIRMED,
                new CrmSaleOrderDO().setStatus(CrmSaleOrderStatusEnum.SHIPPED.getStatus())
                        .setDeliveryStatus(CrmSaleOrderDeliveryStatusEnum.SHIPPED.getStatus()),
                CrmSaleOrderStatusEnum.CONFIRMED.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeSaleOrder(Long id) {
        // 已发货 -> 已完成；发货状态同步为「已签收」（完成即签收闭环）
        transitionStatus(id, SALE_ORDER_COMPLETE_FAIL_NOT_SHIPPED,
                new CrmSaleOrderDO().setStatus(CrmSaleOrderStatusEnum.COMPLETED.getStatus())
                        .setDeliveryStatus(CrmSaleOrderDeliveryStatusEnum.SIGNED.getStatus()),
                CrmSaleOrderStatusEnum.SHIPPED.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelSaleOrder(Long id) {
        // 草稿/已确认 -> 已取消；发货后不允许取消（货已出，退货不在本项）
        transitionStatus(id, SALE_ORDER_CANCEL_FAIL,
                new CrmSaleOrderDO().setStatus(CrmSaleOrderStatusEnum.CANCELED.getStatus()),
                CrmSaleOrderStatusEnum.DRAFT.getStatus(), CrmSaleOrderStatusEnum.CONFIRMED.getStatus());
    }

    /**
     * 状态流转统一实现：以「来源状态集合」做条件更新（CAS），并发下只有一个动作能生效
     *
     * <p>affected rows = 0 时回查一次给出精确原因（订单不存在 / 当前状态不允许该动作）。
     * 不用「先查后改」是为了避免两个并发动作各自通过校验后互相覆盖（伪成功）。
     */
    private void transitionStatus(Long id, ErrorCode errorCode, CrmSaleOrderDO updateObj, Integer... fromStatuses) {
        int updated = saleOrderMapper.updateByIdAndStatus(id, Arrays.asList(fromStatuses), updateObj);
        if (updated == 0) {
            CrmSaleOrderDO order = saleOrderMapper.selectById(id);
            if (order == null) {
                throw exception(SALE_ORDER_NOT_EXISTS);
            }
            throw exception(errorCode);
        }
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
