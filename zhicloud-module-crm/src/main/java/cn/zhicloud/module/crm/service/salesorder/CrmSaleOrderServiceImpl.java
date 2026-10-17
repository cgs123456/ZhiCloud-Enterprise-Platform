package cn.zhicloud.module.crm.service.salesorder;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjUtil;
import cn.zhicloud.framework.common.exception.ErrorCode;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.number.MoneyUtils;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.common.util.object.ObjectUtils;
import cn.zhicloud.module.bpm.api.task.BpmProcessInstanceApi;
import cn.zhicloud.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.zhicloud.module.crm.controller.admin.salesorder.vo.CrmSaleOrderPageReqVO;
import cn.zhicloud.module.crm.controller.admin.salesorder.vo.CrmSaleOrderSaveReqVO;
import cn.zhicloud.module.crm.dal.dataobject.contract.CrmContractDO;
import cn.zhicloud.module.crm.dal.dataobject.receivable.CrmReceivableDO;
import cn.zhicloud.module.crm.dal.dataobject.salesorder.CrmSaleOrderDO;
import cn.zhicloud.module.crm.dal.dataobject.salesorder.CrmSaleOrderItemDO;
import cn.zhicloud.module.crm.dal.mysql.receivable.CrmReceivableMapper;
import cn.zhicloud.module.crm.dal.mysql.salesorder.CrmSaleOrderItemMapper;
import cn.zhicloud.module.crm.dal.mysql.salesorder.CrmSaleOrderMapper;
import cn.zhicloud.module.crm.dal.redis.no.CrmNoRedisDAO;
import cn.zhicloud.module.crm.enums.common.CrmAuditStatusEnum;
import cn.zhicloud.module.crm.enums.salesorder.CrmSaleOrderDeliveryStatusEnum;
import cn.zhicloud.module.crm.enums.salesorder.CrmSaleOrderPaymentStatusEnum;
import cn.zhicloud.module.crm.enums.salesorder.CrmSaleOrderStatusEnum;
import cn.zhicloud.module.crm.service.contract.CrmContractService;
import cn.zhicloud.module.crm.service.customer.CrmCustomerService;
import cn.zhicloud.module.crm.util.CrmPaymentStatusUtils;
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
import static cn.zhicloud.module.crm.util.CrmAuditStatusUtils.convertBpmResultToAuditStatus;

/**
 * CRM 销售订单 Service 实现类
 *
 * @author dhb52
 */
@Service
@Validated
@Slf4j
public class CrmSaleOrderServiceImpl implements CrmSaleOrderService {

    /**
     * 销售订单审批流程定义的标识（§12.261，与 contract/receivable/invoice 同范式）
     */
    public static final String BPM_PROCESS_DEFINITION_KEY = "crm-sale-order-audit";

    @Resource
    private CrmSaleOrderMapper saleOrderMapper;
    @Resource
    private CrmSaleOrderItemMapper saleOrderItemMapper;

    @Resource
    private CrmReceivableMapper receivableMapper;

    @Resource
    private CrmNoRedisDAO noRedisDAO;

    @Resource
    private BpmProcessInstanceApi bpmProcessInstanceApi;

    @Resource
    @Lazy
    private CrmCustomerService customerService;
    @Resource
    @Lazy
    private CrmContractService contractService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSaleOrder(CrmSaleOrderSaveReqVO createReqVO, Long userId) {
        // 1.1 校验关联数据（返回关联合同，供付款状态初始化）
        CrmContractDO contract = validateRelationDataExists(createReqVO);
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
                // audit_status 列没有库级默认值（对齐 crm_contract 口径）：不在此落 DRAFT 的话，新建订单永远提交不了审批
                .setAuditStatus(CrmAuditStatusEnum.DRAFT.getStatus())
                .setPaymentStatus(resolveInitialPaymentStatus(contract))
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
        // 1.3 审批维度放行：未提交、审批中可以编辑（镜像回款编辑语义；驳回/撤回后不可编辑）
        if (!ObjectUtils.equalsAny(oldOrder.getAuditStatus(), CrmAuditStatusEnum.DRAFT.getStatus(),
                CrmAuditStatusEnum.PROCESS.getStatus())) {
            throw exception(SALE_ORDER_UPDATE_FAIL_EDITING_PROHIBITED);
        }
        // 1.4 校验关联数据
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
        // 1.3 审批中订单不可删除：删除在途订单会遗留运行实例并让回写永久失败（回款链允许删除属既有差异，本链不复制）
        if (ObjUtil.equal(order.getAuditStatus(), CrmAuditStatusEnum.PROCESS.getStatus())) {
            throw exception(SALE_ORDER_DELETE_FAIL_AUDIT_PROCESS);
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
     * 校验关联数据是否存在，并返回关联合同（无合同时返回 null）
     *
     * <p>返回值供 {@link #resolveInitialPaymentStatus} 使用；更新路径忽略返回值。
     */
    private CrmContractDO validateRelationDataExists(CrmSaleOrderSaveReqVO reqVO) {
        // 1. 校验客户
        if (reqVO.getCustomerId() != null) {
            customerService.validateCustomer(reqVO.getCustomerId());
        }
        // 2. 如果有关联合同，则需要校验存在
        if (reqVO.getContractId() != null) {
            return contractService.validateContract(reqVO.getContractId());
        }
        return null;
    }

    /**
     * 订单创建时初始化付款状态：付款状态 = 合同收款进度投影（§12.260 D1/D2b）
     *
     * <p>「先收齐后下单」的订单必须按当前进度出生：额度已满后不会再有审批事件来纠正，普通读会留下永久陈旧值。
     * 已审批回款必须锁读（FOR UPDATE），与回款审批路径的回款行锁互相串行——create 先拿到行锁则审批的订单写扫描
     * 会包含本新订单；审批先提交则本锁读看到新进度。两方向终态都等于「最新已提交进度」（时序推演见 §12.260 §3，
     * 禁止降级为普通读）。
     */
    private Integer resolveInitialPaymentStatus(CrmContractDO contract) {
        if (contract == null) {
            return CrmSaleOrderPaymentStatusEnum.UNPAID.getStatus();
        }
        List<CrmReceivableDO> approvedReceivables = receivableMapper.selectListByContractIdAndStatusForUpdate(
                contract.getId(), List.of(CrmAuditStatusEnum.APPROVE.getStatus()));
        BigDecimal receivedPrice = getSumValue(approvedReceivables, CrmReceivableDO::getPrice,
                BigDecimal::add, BigDecimal.ZERO);
        return CrmPaymentStatusUtils.resolve(receivedPrice, contract.getTotalPrice());
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
        // 只有「草稿 + 未提交」可以确认（CAS 条件更新，防并发覆盖/复活已取消订单；已提交过审批的订单由流程定局，不能手工确认）
        transitionStatusWithAudit(id, SALE_ORDER_CONFIRM_FAIL_NOT_DRAFT, SALE_ORDER_CONFIRM_FAIL_AUDIT_STATUS,
                new CrmSaleOrderDO().setStatus(CrmSaleOrderStatusEnum.CONFIRMED.getStatus()),
                List.of(CrmSaleOrderStatusEnum.DRAFT.getStatus()), List.of(CrmAuditStatusEnum.DRAFT.getStatus()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitSaleOrder(Long id, Long userId) {
        // 1. 先 CAS 抢占「未提交 -> 审批中」；失败时绝不调用 BPM，避免重复流程实例（镜像回款 submit 口径）
        transitionStatusWithAudit(id, SALE_ORDER_SUBMIT_FAIL_STATUS_NOT_DRAFT, SALE_ORDER_SUBMIT_FAIL_NOT_DRAFT,
                new CrmSaleOrderDO().setAuditStatus(CrmAuditStatusEnum.PROCESS.getStatus()),
                List.of(CrmSaleOrderStatusEnum.DRAFT.getStatus()), List.of(CrmAuditStatusEnum.DRAFT.getStatus()));

        // 2. 创建审批流程实例。业务键用于把审批结果还原回本订单，数据库 CAS 只负责本地唯一抢先
        String processInstanceId = bpmProcessInstanceApi.createProcessInstance(userId, new BpmProcessInstanceCreateReqDTO()
                .setProcessDefinitionKey(BPM_PROCESS_DEFINITION_KEY).setBusinessKey(String.valueOf(id)));

        // 3. 回写工作流编号；CAS 条件不变（status=10 且 audit=10），失败抛乐观锁并整体回滚（含已抢占的审批状态）
        int updated = saleOrderMapper.updateByIdAndStatusAndAuditStatus(id,
                List.of(CrmSaleOrderStatusEnum.DRAFT.getStatus()), List.of(CrmAuditStatusEnum.PROCESS.getStatus()),
                new CrmSaleOrderDO().setProcessInstanceId(processInstanceId));
        if (updated == 0) {
            throw exception(CRM_OPTIMISTIC_LOCK_FAIL);
        }
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
        // 草稿/已确认 -> 已取消；发货后不允许取消（货已出，退货不在本项）；审批中不可取消（须先撤回流程）
        transitionStatusWithAudit(id, SALE_ORDER_CANCEL_FAIL, SALE_ORDER_CANCEL_FAIL_AUDIT_PROCESS,
                new CrmSaleOrderDO().setStatus(CrmSaleOrderStatusEnum.CANCELED.getStatus()),
                List.of(CrmSaleOrderStatusEnum.DRAFT.getStatus(), CrmSaleOrderStatusEnum.CONFIRMED.getStatus()),
                List.of(CrmAuditStatusEnum.DRAFT.getStatus(), CrmAuditStatusEnum.APPROVE.getStatus(),
                        CrmAuditStatusEnum.REJECT.getStatus(), CrmAuditStatusEnum.CANCEL.getStatus()));
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

    /**
     * 状态流转统一实现（带审批状态条件）：以「来源状态集合 + 来源审批状态集合」做条件更新（CAS）
     *
     * <p>affected rows = 0 时回查一次给出精确原因（订单不存在 / 业务状态不允许 / 审批状态不允许）。
     */
    private void transitionStatusWithAudit(Long id, ErrorCode statusErrorCode, ErrorCode auditErrorCode,
                                           CrmSaleOrderDO updateObj, List<Integer> fromStatuses,
                                           List<Integer> fromAuditStatuses) {
        int updated = saleOrderMapper.updateByIdAndStatusAndAuditStatus(id, fromStatuses, fromAuditStatuses, updateObj);
        if (updated == 0) {
            CrmSaleOrderDO order = saleOrderMapper.selectById(id);
            if (order == null) {
                throw exception(SALE_ORDER_NOT_EXISTS);
            }
            if (!fromStatuses.contains(order.getStatus())) {
                throw exception(statusErrorCode);
            }
            throw exception(auditErrorCode);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleOrderAuditStatus(Long id, Integer bpmResult) {
        // 1.1 校验订单是否存在
        CrmSaleOrderDO order = validateSaleOrderExists(id);
        // 1.2 只有审批中，可以更新审批结果
        if (ObjUtil.notEqual(order.getAuditStatus(), CrmAuditStatusEnum.PROCESS.getStatus())) {
            log.error("[updateSaleOrderAuditStatus][order({}) 不处于审批中，无法更新审批结果({})]",
                    order.getId(), bpmResult);
            throw exception(SALE_ORDER_UPDATE_AUDIT_STATUS_FAIL_NOT_PROCESS);
        }

        // 2. 更新审批结果：审批通过同时把订单流转为已确认（审批=确认）；驳回/撤回只更新审批状态
        Integer auditStatus = convertBpmResultToAuditStatus(bpmResult);
        CrmSaleOrderDO updateObj = new CrmSaleOrderDO().setAuditStatus(auditStatus);
        if (ObjUtil.equal(auditStatus, CrmAuditStatusEnum.APPROVE.getStatus())) {
            updateObj.setStatus(CrmSaleOrderStatusEnum.CONFIRMED.getStatus());
        }
        int updated = saleOrderMapper.updateByIdAndStatusAndAuditStatus(id,
                List.of(CrmSaleOrderStatusEnum.DRAFT.getStatus()), List.of(CrmAuditStatusEnum.PROCESS.getStatus()),
                updateObj);
        if (updated == 0) {
            throw exception(CRM_OPTIMISTIC_LOCK_FAIL);
        }
    }

}
