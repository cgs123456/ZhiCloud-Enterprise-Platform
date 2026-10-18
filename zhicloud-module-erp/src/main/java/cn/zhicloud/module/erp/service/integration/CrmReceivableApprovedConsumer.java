package cn.zhicloud.module.erp.service.integration;

import cn.zhicloud.framework.integration.api.EventConsumer;
import cn.zhicloud.framework.integration.contract.CrmReceivableApprovedEvent;
import cn.zhicloud.framework.integration.contract.IntegrationEventTypes;
import cn.zhicloud.framework.integration.dal.dataobject.EventOutboxDO;
import cn.zhicloud.framework.mdm.enums.MdmConstants;
import cn.zhicloud.framework.mdm.service.MdmCrossRefService;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
import cn.zhicloud.module.erp.service.finance.ErpFinanceReceiptService;
import cn.zhicloud.module.erp.service.finance.bo.ErpFinanceReceiptCrmCreateReqBO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.framework.integration.enums.ErrorCodeConstants.EVENT_TENANT_MISMATCH;
import static cn.zhicloud.framework.integration.enums.ErrorCodeConstants.EVENT_TENANT_REQUIRED;

/**
 * 「CRM 回款审批通过」事件消费者：生成 ERP 收款单（待审核）（D1/§12.267）
 *
 * <p>三段式处理顺序是刻意的，顺序错了会留下不一致数据：
 * <ol>
 *   <li>业务级查重：先用回款主键推导的幂等键查有没有建过收款单。幂等必须放在最前面，
 *       否则「至少一次投递」的重试会建出第二张单。</li>
 *   <li>MDM 主数据映射：CRM 客户编号在 ERP 里是另一套主键，必须经对照表翻译。
 *       映射缺失抛异常而不是跳过：跳过会让收款单挂到错误客户上且事后不可见，
 *       抛异常则由可靠集成数据面（V100）重试并最终进死信告警。</li>
 *   <li>建单：走 ERP 收款单的集成入口（{@code createFinanceReceiptFromCrm}），不绕过客户与幂等校验。
 *       本消费者只建「待审核」单据，不自动审核——过账属财务权责（§12.267 D8）。</li>
 * </ol>
 *
 * @author 智云集成治理
 */
@Component
@Slf4j
public class CrmReceivableApprovedConsumer implements EventConsumer {

    /**
     * 消费者名称：会写进 {@code event_inbox} 的去重唯一键，改名等于换了一个消费者，
     * 历史事件会被重新消费一次，务必谨慎。
     */
    public static final String NAME = "erp-finance-receipt-creator";

    /**
     * 收款单幂等键前缀：同一 CRM 回款审批通过只产生一张 ERP 收款单
     */
    static final String RECEIPT_IDEMPOTENCY_KEY_PREFIX = "crm-receivable-";

    @Resource
    private ErpFinanceReceiptService financeReceiptService;
    @Resource
    private ErpFinanceReceiptMapper financeReceiptMapper;
    @Resource
    private MdmCrossRefService mdmCrossRefService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean supports(String eventType) {
        return IntegrationEventTypes.CRM_RECEIVABLE_APPROVED.equals(eventType);
    }

    @Override
    public void handle(EventOutboxDO event) throws Exception {
        Long eventTenantId = requireEventTenantId(event);
        Long oldTenantId = TenantContextHolder.getTenantId();
        boolean oldIgnore = TenantContextHolder.isIgnore();
        try {
            // 消费者也可能被直接调用；不能依赖中继框架替它建立租户上下文。
            TenantContextHolder.setTenantId(eventTenantId);
            TenantContextHolder.setIgnore(false);
            handleInTenant(event, eventTenantId);
        } finally {
            TenantContextHolder.setTenantId(oldTenantId);
            TenantContextHolder.setIgnore(oldIgnore);
        }
    }

    /**
     * 校验事件租户并拒绝借用调用线程的租户上下文。
     */
    private Long requireEventTenantId(EventOutboxDO event) {
        Long eventTenantId = event == null ? null : event.getTenantId();
        if (eventTenantId == null) {
            throw exception(EVENT_TENANT_REQUIRED);
        }
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId != null && !currentTenantId.equals(eventTenantId)) {
            throw exception(EVENT_TENANT_MISMATCH);
        }
        return eventTenantId;
    }

    private void handleInTenant(EventOutboxDO event, Long tenantId) {
        CrmReceivableApprovedEvent payload = CrmReceivableApprovedEvent.fromJson(event.getPayload());

        // 1. 业务级幂等：同一 CRM 回款只建一张 ERP 收款单。必须放在最前面：
        //    「至少一次投递」的重试会重复消费，重试时既不重复解析映射也不重复建单。
        String idempotencyKey = buildReceiptIdempotencyKey(payload.getReceivableId());
        if (financeReceiptMapper.selectByIdempotencyKey(idempotencyKey) != null) {
            log.info("[CRM->ERP] 收款单已存在，跳过建单 tenantId={} receivableId={}",
                    tenantId, payload.getReceivableId());
            return;
        }

        // 2. 主数据映射：CRM 客户编号 -> ERP 客户编号（关键字段，缺映射抛异常进死信）
        Long customerId = mdmCrossRefService.mapTargetIdAsLong(tenantId,
                MdmConstants.SYSTEM_CRM, MdmConstants.ENTITY_CUSTOMER, String.valueOf(payload.getCustomerId()),
                MdmConstants.SYSTEM_ERP, MdmConstants.ENTITY_CUSTOMER);

        // 3. 建单：走收款单集成入口，保留客户与幂等校验
        Long id = financeReceiptService.createFinanceReceiptFromCrm(ErpFinanceReceiptCrmCreateReqBO.builder()
                .idempotencyKey(idempotencyKey)
                .receivableId(payload.getReceivableId())
                .receivableNo(payload.getNo())
                .customerId(customerId)
                .receiptTime(payload.getReturnTime() == null ? LocalDateTime.now() : payload.getReturnTime())
                .price(payload.getPrice())
                .build());
        log.info("[CRM->ERP] 已生成收款单 id={} tenantId={} receivableNo={}", id, tenantId, payload.getNo());
    }

    /**
     * 收款单幂等键：由回款主键推导，业务侧可复算，人工可反查。
     */
    static String buildReceiptIdempotencyKey(Long receivableId) {
        return RECEIPT_IDEMPOTENCY_KEY_PREFIX + receivableId;
    }

}
