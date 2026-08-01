package cn.iocoder.yudao.module.erp.domain.sale.aggregate;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 销售订单聚合根（DDD 试点）
 *
 * <p>聚合根封装一张销售订单及其明细，内聚状态流转、出库校验、金额计算等业务规则。
 * 外部只能通过聚合根的方法改变订单状态，不允许直接 set 字段，从而保证业务不变式。
 *
 * <p>本类为纯 Java 类，不依赖 Spring / MyBatis 等框架（仅使用 Jakarta validation 注解），
 * 体现领域层的隔离性与可测试性。持久化由应用服务负责，通过 {@link #reconstitute} 从 DO 重建聚合根。
 *
 * <h3>状态机</h3>
 * <ul>
 *     <li>{@link #STATUS_DRAFT}（10，未审核）→ 可 {@link #confirm()} 确认 / {@link #cancel()} 取消</li>
 *     <li>{@link #STATUS_CONFIRMED}（20，已审核）→ 可 {@link #ship(BigDecimal[])} 出库 / {@link #cancel()} 取消（需无出库）</li>
 *     <li>{@link #STATUS_CANCELLED}（30，已取消）→ 终态</li>
 * </ul>
 *
 * <p>注：{@link #STATUS_DRAFT} / {@link #STATUS_CONFIRMED} 与
 * {@link cn.iocoder.yudao.module.erp.enums.ErpAuditStatus} 对齐；
 * {@link #STATUS_CANCELLED} 为领域层扩展状态，演示 DDD 对业务概念的演进能力。
 *
 * @author DDD 试点
 */
public class SaleOrderAggregate {

    // ==================== 状态常量 ====================

    /**
     * 草稿（未审核），对应 {@code ErpAuditStatus.PROCESS}
     */
    public static final int STATUS_DRAFT = 10;
    /**
     * 已确认（已审核），对应 {@code ErpAuditStatus.APPROVE}
     */
    public static final int STATUS_CONFIRMED = 20;
    /**
     * 已取消（领域扩展状态）
     */
    public static final int STATUS_CANCELLED = 30;

    // ==================== 字段 ====================

    /**
     * 订单编号
     */
    @NotNull
    private Long orderId;
    /**
     * 销售订单号
     */
    private String orderNo;
    /**
     * 客户编号
     */
    @NotNull
    private Long customerId;
    /**
     * 下单时间
     */
    private LocalDateTime orderDate;
    /**
     * 总金额
     */
    private BigDecimal totalAmount;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 租户编号（用于领域事件透传）
     */
    private Long tenantId;
    /**
     * 订单明细列表（不可变）
     */
    private List<OrderItem> items;

    /**
     * 私有构造，强制通过 {@link #reconstitute} 创建聚合根
     */
    private SaleOrderAggregate() {
    }

    // ==================== 业务方法 ====================

    /**
     * 审核确认订单
     *
     * <p>业务规则：只有 {@link #STATUS_DRAFT} 状态的订单才能确认。
     *
     * @throws IllegalStateException 状态不满足确认条件
     */
    public void confirm() {
        if (!Integer.valueOf(STATUS_DRAFT).equals(status)) {
            throw new IllegalStateException(
                    "只有未审核（草稿）状态的销售订单才能审核确认，当前状态：" + status);
        }
        this.status = STATUS_CONFIRMED;
    }

    /**
     * 出库
     *
     * <p>业务规则：
     * <ul>
     *     <li>订单必须处于 {@link #STATUS_CONFIRMED} 状态</li>
     *     <li>每个明细的累计出库数量不能超过订单数量</li>
     *     <li>出库数量不能为负</li>
     * </ul>
     *
     * @param quantities 各明细的累计出库数量（与 {@link #items} 顺序一致）
     * @throws IllegalStateException  订单状态不允许出库
     * @throws IllegalArgumentException 数组长度与明细数量不一致或数量非法
     */
    public void ship(BigDecimal[] quantities) {
        if (!Integer.valueOf(STATUS_CONFIRMED).equals(status)) {
            throw new IllegalStateException("只有已审核的销售订单才能出库，当前状态：" + status);
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("销售订单明细为空，无法出库");
        }
        if (quantities == null || quantities.length != items.size()) {
            throw new IllegalArgumentException("出库数量数组长度与订单明细数量不一致");
        }
        // 1. 校验出库数量
        for (int i = 0; i < items.size(); i++) {
            BigDecimal qty = quantities[i] == null ? BigDecimal.ZERO : quantities[i];
            if (qty.signum() < 0) {
                throw new IllegalArgumentException("出库数量不能为负，明细序号：" + i);
            }
            OrderItem item = items.get(i);
            if (qty.compareTo(item.quantity()) > 0) {
                throw new IllegalStateException(
                        "出库数量超过订单数量，产品：" + item.productName()
                                + "，订单数量：" + item.quantity() + "，出库数量：" + qty);
            }
        }
        // 2. 应用变更：值对象不可变，生成新的明细列表
        List<OrderItem> newItems = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            BigDecimal qty = quantities[i] == null ? BigDecimal.ZERO : quantities[i];
            OrderItem item = items.get(i);
            newItems.add(new OrderItem(item.itemId(), item.productId(), item.productName(),
                    item.quantity(), item.unitPrice(), item.amount(),
                    qty, item.returnCount()));
        }
        this.items = Collections.unmodifiableList(newItems);
    }

    /**
     * 取消订单
     *
     * <p>业务规则：
     * <ul>
     *     <li>已取消的订单无需重复取消</li>
     *     <li>已确认且已有出库的订单不允许取消（需先处理出库）</li>
     *     <li>草稿或已确认（无出库）状态可取消</li>
     * </ul>
     *
     * @throws IllegalStateException 当前状态不允许取消
     */
    public void cancel() {
        if (Integer.valueOf(STATUS_CANCELLED).equals(status)) {
            throw new IllegalStateException("销售订单已取消，无需重复取消");
        }
        if (Integer.valueOf(STATUS_CONFIRMED).equals(status)) {
            BigDecimal totalOut = getTotalOutCount();
            if (totalOut.signum() > 0) {
                throw new IllegalStateException("已出库的销售订单无法取消，请先处理出库记录");
            }
        }
        if (!Integer.valueOf(STATUS_DRAFT).equals(status)
                && !Integer.valueOf(STATUS_CONFIRMED).equals(status)) {
            throw new IllegalStateException("当前状态的销售订单无法取消，状态：" + status);
        }
        this.status = STATUS_CANCELLED;
    }

    /**
     * 计算订单总金额（= 各明细金额之和）
     *
     * <p>金额计算内聚到聚合根，避免散落到 Service 层。
     *
     * @return 总金额
     */
    public BigDecimal calculateTotal() {
        if (items == null || items.isEmpty()) {
            this.totalAmount = BigDecimal.ZERO;
            return this.totalAmount;
        }
        BigDecimal total = items.stream()
                .map(OrderItem::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalAmount = total;
        return total;
    }

    /**
     * 是否已全部出库
     *
     * @return true 表示所有明细均已出库完成
     */
    public boolean isFullyShipped() {
        if (items == null || items.isEmpty()) {
            return false;
        }
        return items.stream().allMatch(item -> item.getRemainingCount().signum() == 0);
    }

    // ==================== 重建 ====================

    /**
     * 从持久化数据重建聚合根
     *
     * <p>应用服务加载 DO 后，通过此方法重建聚合根实例，再调用业务方法完成状态变更。
     *
     * @param orderId      订单编号
     * @param orderNo      销售订单号
     * @param customerId   客户编号
     * @param orderDate    下单时间
     * @param totalAmount  总金额
     * @param status       当前状态
     * @param tenantId     租户编号
     * @param items        订单明细列表
     * @return 聚合根实例
     */
    public static SaleOrderAggregate reconstitute(Long orderId, String orderNo, Long customerId,
                                                  LocalDateTime orderDate, BigDecimal totalAmount,
                                                  Integer status, Long tenantId, List<OrderItem> items) {
        SaleOrderAggregate aggregate = new SaleOrderAggregate();
        aggregate.orderId = orderId;
        aggregate.orderNo = orderNo;
        aggregate.customerId = customerId;
        aggregate.orderDate = orderDate;
        aggregate.totalAmount = totalAmount;
        aggregate.status = status;
        aggregate.tenantId = tenantId;
        aggregate.items = items == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(items));
        return aggregate;
    }

    // ==================== 派生查询 ====================

    /**
     * 获取累计出库数量（所有明细之和）
     *
     * @return 累计出库数量
     */
    public BigDecimal getTotalOutCount() {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(OrderItem::outCount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ==================== Getter ====================

    public Long getOrderId() {
        return orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Integer getStatus() {
        return status;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public List<OrderItem> getItems() {
        return items;
    }
}
