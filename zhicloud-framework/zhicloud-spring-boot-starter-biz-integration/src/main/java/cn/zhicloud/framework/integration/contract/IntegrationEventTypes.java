package cn.zhicloud.framework.integration.contract;

/**
 * 跨模块事件类型登记表（V101）
 *
 * <p>为什么放在集成 Starter 而不是某个业务模块的 api 包：
 * 事件契约的双方是「生产者模块」和「消费者模块」，把契约放在任一侧都会逼另一侧依赖它
 * （ERP -> WMS 已经需要 WMS 读 ERP 的契约；下一步 WMS 出库完成回写 ERP 又会需要反向依赖），
 * 双向依赖会形成模块环。仓库里 erp.api 被 mes 依赖的既有模式适用于「单向 SPI」，
 * 不适用于双向事件协同，因此事件契约统一登记在集成层。
 *
 * <p>改名等于换契约：消费者按事件类型匹配，旧事件不会被重新消费，新事件不会被旧消费者处理。
 *
 * @author 智云集成治理
 */
public final class IntegrationEventTypes {

    private IntegrationEventTypes() {
    }

    /**
     * ERP 销售出库审核通过（V101 首条链路）
     */
    public static final String ERP_SALE_OUT_APPROVED = "erp.sale-out.approved.v1";

    /**
     * CRM 回款审批通过（D1 链路，§12.267）：CRM 回款 → ERP 收款单
     */
    public static final String CRM_RECEIVABLE_APPROVED = "crm.receivable.approved.v1";

}