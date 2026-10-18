package cn.zhicloud.module.crm.service.receivable;

import cn.zhicloud.framework.common.util.collection.CollectionUtils;
import cn.zhicloud.framework.integration.api.EventAppendReq;
import cn.zhicloud.framework.integration.contract.CrmReceivableApprovedEvent;
import cn.zhicloud.framework.integration.contract.IntegrationEventTypes;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.module.crm.dal.dataobject.contract.CrmContractDO;
import cn.zhicloud.module.crm.dal.dataobject.receivable.CrmReceivableDO;
import cn.zhicloud.module.crm.dal.mysql.receivable.CrmReceivableMapper;
import cn.zhicloud.module.crm.dal.mysql.salesorder.CrmSaleOrderMapper;
import cn.zhicloud.module.crm.enums.common.CrmAuditStatusEnum;
import cn.zhicloud.module.crm.util.CrmPaymentStatusUtils;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.framework.integration.enums.ErrorCodeConstants.EVENT_TENANT_MISMATCH;
import static cn.zhicloud.framework.integration.enums.ErrorCodeConstants.EVENT_TENANT_REQUIRED;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.*;

/**
 * CRM 回款审批通过的联动支撑：合同订单付款状态投影（§12.260 D1/D2a）+ 跨域事件登记（§12.267 D1）。
 *
 * <p>从 {@link CrmReceivableServiceImpl} 抽出（§12.267 D1）：审批通过的两项事务内联动集中于此，
 * Impl 只保留编排；同时把 Impl 控制在 500 行结构红线内。
 *
 * <p>刻意保持无状态静态（不参与 Spring 装配）：Mapper 由调用方传入；事务边界留在 Impl 的
 * updateReceivableAuditStatus（@Transactional）内，本类不持有事务语义。
 */
@Slf4j
final class CrmReceivableApprovalSupport {

    private CrmReceivableApprovalSupport() {
    }

    /**
     * 回款审批通过后，重算该合同下全部未取消销售订单的付款状态（§12.260 D2a/D4）
     *
     * <p>已审批回款必须锁读（FOR UPDATE）：RR 快照下普通读看不到先提交者的审批结果，与合同行锁配合
     * 才能拿到「最新已提交」的收款进度（§12.233.12 同款教训）。
     *
     * <p>合同行理论上必存在（存在任何回款行的合同不可删除），缺失只可能是数据异常：记日志并跳过投影，
     * 不让审批本身失败——BPM 已判完成而 CRM 回滚会造「已审批未生效」的更坏不一致（§12.260 D7d）。
     */
    static void syncSaleOrderPaymentStatusOnApproved(CrmReceivableMapper receivableMapper,
                                                     CrmSaleOrderMapper saleOrderMapper,
                                                     CrmContractDO contract) {
        if (contract == null) {
            log.error("[syncSaleOrderPaymentStatusOnApproved][回款所属合同不存在，跳过订单付款状态投影]");
            return;
        }
        List<CrmReceivableDO> approvedReceivables = receivableMapper.selectListByContractIdAndStatusForUpdate(
                contract.getId(), List.of(CrmAuditStatusEnum.APPROVE.getStatus()));
        BigDecimal receivedPrice = CollectionUtils.getSumValue(approvedReceivables, CrmReceivableDO::getPrice,
                BigDecimal::add, BigDecimal.ZERO);
        Integer paymentStatus = CrmPaymentStatusUtils.resolve(receivedPrice, contract.getTotalPrice());
        saleOrderMapper.updatePaymentStatusByContractId(contract.getId(), paymentStatus);
    }

    /**
     * 构造「CRM 回款审批通过」事件的登记请求（§12.267 D1）。
     *
     * <p>eventId 由业务主键推导（{@code crm-receivable-{id}-approved}）而不是随机数：
     * 同一回款重复触发时得到相同 eventId，消费侧才能靠 event_inbox 唯一键去重。
     *
     * <p>payload 只含下游建单必需字段：账户与财务人员维度都不下传——账户由 ERP 侧建单时
     * 按「租户默认账户」补齐（§12.267 D1 修订），财务人员留空、由财务在审核过账前补选。
     */
    static EventAppendReq buildReceivableApprovedEventAppendReq(
            CrmReceivableDO receivable, Long tenantId) {
        CrmReceivableApprovedEvent event = CrmReceivableApprovedEvent.builder()
                .no(receivable.getNo())
                .receivableId(receivable.getId())
                .customerId(receivable.getCustomerId())
                .returnTime(receivable.getReturnTime())
                .price(receivable.getPrice())
                .build();
        return EventAppendReq.builder()
                .eventId(CrmReceivableApprovedEvent.eventIdOf(receivable.getId()))
                .tenantId(tenantId)
                .aggregateType(CrmReceivableApprovedEvent.AGGREGATE_TYPE)
                .aggregateId(String.valueOf(receivable.getId()))
                .eventType(IntegrationEventTypes.CRM_RECEIVABLE_APPROVED)
                .schemaVersion("v1")
                .payload(event.toJson())
                .occurredAt(receivable.getReturnTime() == null ? LocalDateTime.now() : receivable.getReturnTime())
                .build();
    }

    /**
     * 取当前租户编号：跨域事件登记必须租户明确，禁止静默回退租户 0（§12.267 D7，对齐 ERP 出库事件先例）。
     */
    static Long requireCurrentTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw exception(EVENT_TENANT_REQUIRED);
        }
        return tenantId;
    }

    /**
     * 当前操作只能登记自己租户单据的事件，缺失或冲突时在事件登记前拒绝。
     */
    static void validateReceivableTenant(CrmReceivableDO receivable, Long tenantId) {
        if (receivable.getTenantId() == null || !tenantId.equals(receivable.getTenantId())) {
            throw exception(EVENT_TENANT_MISMATCH);
        }
    }
}
