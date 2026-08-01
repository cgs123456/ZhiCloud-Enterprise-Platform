package cn.iocoder.yudao.module.erp.domain.saleorder;

import lombok.Getter;

/**
 * 销售订单状态枚举（DDD 试点）
 *
 * <p>领域层的状态值对象，与持久层的 {@code Integer status} 字段对应。
 * 状态值与 {@link cn.iocoder.yudao.module.erp.enums.ErpAuditStatus} 对齐，
 * 在此基础上扩展 {@link #CANCELED} 取消状态，体现 DDD 对业务概念的演进能力。
 *
 * <h3>状态机</h3>
 * <ul>
 *     <li>{@link #DRAFT}（10，草稿）→ 可 {@code audit()} 审批 / {@code cancel()} 取消</li>
 *     <li>{@link #AUDITED}（20，已审批）→ 可出库 / 退货 / 取消（需无出库）</li>
 *     <li>{@link #CANCELED}（99，已取消）→ 终态</li>
 * </ul>
 *
 * @author DDD 试点
 */
@Getter
public enum SaleOrderStatus {

    /**
     * 草稿（未审批），对应 {@code ErpAuditStatus.PROCESS}
     */
    DRAFT(10, "草稿"),
    /**
     * 已审批，对应 {@code ErpAuditStatus.APPROVE}
     */
    AUDITED(20, "已审批"),
    /**
     * 已取消（领域扩展状态）
     */
    CANCELED(99, "已取消");

    private final int code;
    private final String name;

    SaleOrderStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 从持久层的 Integer 状态值转换为枚举
     *
     * @param code 状态码
     * @return 对应枚举，未匹配时返回 {@link #DRAFT}
     */
    public static SaleOrderStatus of(Integer code) {
        if (code == null) {
            return DRAFT;
        }
        for (SaleOrderStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return DRAFT;
    }

    /**
     * 转换为持久层的 Integer 状态值
     *
     * @return 状态码
     */
    public int toCode() {
        return code;
    }
}
