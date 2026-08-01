package cn.iocoder.yudao.module.erp.domain.saleorder;

import cn.iocoder.yudao.module.erp.domain.saleorder.event.SaleOrderAuditedEvent;
import cn.iocoder.yudao.module.erp.domain.saleorder.event.SaleOrderCreatedEvent;
import cn.iocoder.yudao.module.erp.domain.saleorder.event.SaleOrderOutCountUpdatedEvent;
import cn.iocoder.yudao.module.erp.domain.saleorder.event.SaleOrderReturnCountUpdatedEvent;
import cn.iocoder.yudao.module.erp.domain.saleorder.vo.CreateSaleOrderCommand;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ERP 销售订单聚合根（DDD 试点）
 *
 * <p>聚合边界：销售订单 + 订单明细。外部只能通过聚合根方法改变订单状态，
 * 不允许直接 set 字段，从而保证业务不变式。
 *
 * <p>本类为纯 Java 类，不依赖 Spring / MyBatis 等框架，体现领域层的隔离性与可测试性。
 * 持久化由仓储（{@code ErpSaleOrderRepository}）负责，通过 {@link #reconstitute} 从 DO 重建聚合根。
 *
 * <h3>不变式</h3>
 * <ul>
 *     <li>总价 = 明细小计之和 + 税额 - 优惠金额</li>
 *     <li>出库数量 ≤ 订单总数量</li>
 *     <li>退货数量 ≤ 出库数量</li>
 *     <li>审批后不可修改明细</li>
 * </ul>
 *
 * <h3>状态机</h3>
 * <ul>
 *     <li>{@link SaleOrderStatus#DRAFT}（10，草稿）→ 可 {@link #audit()} 审批 / {@link #cancel()} 取消</li>
 *     <li>{@link SaleOrderStatus#AUDITED}（20，已审批）→ 可出库 / 退货</li>
 *     <li>{@link SaleOrderStatus#CANCELED}（99，已取消）→ 终态</li>
 * </ul>
 *
 * @author DDD 试点
 */
@Getter
public class ErpSaleOrderAggregate {

    // ==================== 字段 ====================

    /**
     * 订单编号
     */
    private Long id;
    /**
     * 销售订单号
     */
    private String no;
    /**
     * 状态
     */
    private SaleOrderStatus status;
    /**
     * 客户编号
     */
    private Long customerId;
    /**
     * 结算账户编号
     */
    private Long accountId;
    /**
     * 销售员编号
     */
    private Long saleUserId;
    /**
     * 下单时间
     */
    private LocalDateTime orderTime;

    /**
     * 订单明细列表（不可变）
     */
    private List<SaleOrderItem> items;

    // ========== 金额（派生值，由 items 计算） ==========
    /**
     * 合计产品价格
     */
    private BigDecimal totalProductPrice;
    /**
     * 合计税额
     */
    private BigDecimal totalTaxPrice;
    /**
     * 优惠率，百分比
     */
    private BigDecimal discountPercent;
    /**
     * 优惠金额
     */
    private BigDecimal discountPrice;
    /**
     * 最终合计价格 = totalProductPrice + totalTaxPrice - discountPrice
     */
    private BigDecimal totalPrice;
    /**
     * 定金金额
     */
    private BigDecimal depositPrice;
    /**
     * 合计数量（派生值，由 items 计算）
     */
    private BigDecimal totalCount;

    // ========== 多币种 ==========
    /**
     * 币种编号
     */
    private Long currencyId;
    /**
     * 汇率（外币 → 本位币）
     */
    private BigDecimal exchangeRate;
    /**
     * 按本位币折算后的总金额 = totalPrice * exchangeRate
     */
    private BigDecimal baseCurrencyTotalPrice;

    // ========== 出库 / 退货跟踪 ==========
    /**
     * 累计出库数量
     */
    private BigDecimal outCount;
    /**
     * 累计退货数量
     */
    private BigDecimal returnCount;

    // ========== 其他 ==========
    /**
     * 附件地址
     */
    private String fileUrl;
    /**
     * 备注
     */
    private String remark;

    /**
     * 领域事件队列（聚合根内部维护，由仓储 / 应用服务在持久化后拉取并发布）
     */
    private final List<Object> domainEvents = new ArrayList<>();

    /**
     * 私有构造，强制通过工厂方法或重建方法创建
     */
    private ErpSaleOrderAggregate() {
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建销售订单（工厂方法）
     *
     * <p>初始化为草稿状态，计算金额派生值，注册创建事件。
     *
     * @param cmd 创建命令
     * @return 新建的聚合根实例
     */
    public static ErpSaleOrderAggregate create(CreateSaleOrderCommand cmd) {
        ErpSaleOrderAggregate agg = new ErpSaleOrderAggregate();
        agg.no = cmd.getNo();
        agg.status = SaleOrderStatus.DRAFT;
        agg.customerId = cmd.getCustomerId();
        agg.accountId = cmd.getAccountId();
        agg.saleUserId = cmd.getSaleUserId();
        agg.orderTime = cmd.getOrderTime();
        agg.discountPercent = cmd.getDiscountPercent();
        agg.depositPrice = cmd.getDepositPrice();
        agg.currencyId = cmd.getCurrencyId();
        agg.exchangeRate = cmd.getExchangeRate();
        agg.fileUrl = cmd.getFileUrl();
        agg.remark = cmd.getRemark();
        agg.outCount = BigDecimal.ZERO;
        agg.returnCount = BigDecimal.ZERO;
        // 构建明细
        List<SaleOrderItem> itemList = new ArrayList<>();
        if (cmd.getItems() != null) {
            for (CreateSaleOrderCommand.Item src : cmd.getItems()) {
                BigDecimal quantity = nvl(src.getCount());
                BigDecimal unitPrice = nvl(src.getProductPrice());
                BigDecimal taxRate = nvl(src.getTaxPercent());
                BigDecimal subtotal = unitPrice.multiply(quantity);
                BigDecimal taxPrice = subtotal.multiply(taxRate)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                itemList.add(new SaleOrderItem(
                        null, src.getProductId(), src.getProductUnitId(),
                        quantity, unitPrice, taxRate, subtotal, taxPrice,
                        BigDecimal.ZERO, BigDecimal.ZERO, src.getRemark()));
            }
        }
        agg.items = Collections.unmodifiableList(itemList);
        // 计算派生金额
        agg.recalculateAmount();
        // 注册事件
        agg.domainEvents.add(new SaleOrderCreatedEvent(agg.id, agg.no));
        return agg;
    }

    // ==================== 业务方法 ====================

    /**
     * 审批通过
     *
     * <p>业务规则：只有草稿状态的订单可以审批。
     *
     * @throws IllegalStateException 状态不满足审批条件
     */
    public void audit() {
        if (this.status != SaleOrderStatus.DRAFT) {
            throw new IllegalStateException("只有草稿状态的订单可以审批，当前状态：" + status);
        }
        this.status = SaleOrderStatus.AUDITED;
        this.domainEvents.add(new SaleOrderAuditedEvent(this.id, this.no));
    }

    /**
     * 取消订单
     *
     * <p>业务规则：已取消的订单不可重复取消；已有出库的订单不允许取消。
     *
     * @throws IllegalStateException 当前状态不允许取消
     */
    public void cancel() {
        if (this.status == SaleOrderStatus.CANCELED) {
            throw new IllegalStateException("订单已取消，无需重复取消");
        }
        if (nvl(this.outCount).signum() > 0) {
            throw new IllegalStateException("已出库的销售订单无法取消，请先处理出库记录");
        }
        this.status = SaleOrderStatus.CANCELED;
    }

    /**
     * 更新出库数量（由出库单完成时触发）
     *
     * <p>业务规则：
     * <ul>
     *     <li>订单必须处于已审批状态</li>
     *     <li>出库后累计数量不能超过订单总数量</li>
     * </ul>
     *
     * @param delta 本次出库增量
     * @throws IllegalStateException  状态不允许出库或超过总量
     */
    public void updateOutCount(BigDecimal delta) {
        if (this.status != SaleOrderStatus.AUDITED) {
            throw new IllegalStateException("只有已审批的订单可以出库，当前状态：" + status);
        }
        if (delta == null || delta.signum() < 0) {
            throw new IllegalArgumentException("出库增量不能为负");
        }
        BigDecimal newOutCount = nvl(this.outCount).add(delta);
        if (newOutCount.compareTo(nvl(this.totalCount)) > 0) {
            throw new IllegalStateException("出库数量不能超过订单总数量");
        }
        this.outCount = newOutCount;
        this.domainEvents.add(new SaleOrderOutCountUpdatedEvent(this.id, this.no, newOutCount));
    }

    /**
     * 更新退货数量（由退货单完成时触发）
     *
     * <p>业务规则：
     * <ul>
     *     <li>订单必须处于已审批状态</li>
     *     <li>退货后累计数量不能超过已出库数量</li>
     * </ul>
     *
     * @param delta 本次退货增量
     * @throws IllegalStateException  状态不允许退货或超过出库量
     */
    public void updateReturnCount(BigDecimal delta) {
        if (this.status != SaleOrderStatus.AUDITED) {
            throw new IllegalStateException("只有已审批的订单可以退货，当前状态：" + status);
        }
        if (delta == null || delta.signum() < 0) {
            throw new IllegalArgumentException("退货增量不能为负");
        }
        BigDecimal newReturnCount = nvl(this.returnCount).add(delta);
        if (newReturnCount.compareTo(nvl(this.outCount)) > 0) {
            throw new IllegalStateException("退货数量不能超过出库数量");
        }
        this.returnCount = newReturnCount;
        this.domainEvents.add(new SaleOrderReturnCountUpdatedEvent(this.id, this.no, newReturnCount));
    }

    // ==================== 重建 ====================

    /**
     * 从持久化数据重建聚合根
     *
     * <p>仓储实现加载 DO 后，通过此方法重建聚合根实例，再调用业务方法完成状态变更。
     * 重建时不注册领域事件（事件只在状态变更时产生，重建是恢复历史状态）。
     *
     * @param id               订单编号
     * @param no               销售订单号
     * @param status           当前状态
     * @param customerId       客户编号
     * @param accountId        结算账户编号
     * @param saleUserId       销售员编号
     * @param orderTime        下单时间
     * @param items            订单明细列表
     * @param discountPercent  优惠率
     * @param depositPrice     定金金额
     * @param currencyId       币种编号
     * @param exchangeRate     汇率
     * @param outCount         累计出库数量
     * @param returnCount      累计退货数量
     * @param fileUrl          附件地址
     * @param remark           备注
     * @return 聚合根实例
     */
    public static ErpSaleOrderAggregate reconstitute(Long id, String no, SaleOrderStatus status,
                                                      Long customerId, Long accountId, Long saleUserId,
                                                      LocalDateTime orderTime, List<SaleOrderItem> items,
                                                      BigDecimal discountPercent, BigDecimal depositPrice,
                                                      Long currencyId, BigDecimal exchangeRate,
                                                      BigDecimal outCount, BigDecimal returnCount,
                                                      String fileUrl, String remark) {
        ErpSaleOrderAggregate agg = new ErpSaleOrderAggregate();
        agg.id = id;
        agg.no = no;
        agg.status = status;
        agg.customerId = customerId;
        agg.accountId = accountId;
        agg.saleUserId = saleUserId;
        agg.orderTime = orderTime;
        agg.items = items == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(items));
        agg.discountPercent = discountPercent;
        agg.depositPrice = depositPrice;
        agg.currencyId = currencyId;
        agg.exchangeRate = exchangeRate;
        agg.outCount = outCount;
        agg.returnCount = returnCount;
        agg.fileUrl = fileUrl;
        agg.remark = remark;
        agg.recalculateAmount();
        return agg;
    }

    // ==================== 派生计算 ====================

    /**
     * 重新计算金额派生值
     *
     * <p>不变式：totalPrice = totalProductPrice + totalTaxPrice - discountPrice
     * baseCurrencyTotalPrice = totalPrice * exchangeRate
     */
    private void recalculateAmount() {
        if (items == null || items.isEmpty()) {
            this.totalProductPrice = BigDecimal.ZERO;
            this.totalTaxPrice = BigDecimal.ZERO;
            this.totalCount = BigDecimal.ZERO;
        } else {
            this.totalProductPrice = items.stream()
                    .map(i -> nvl(i.getSubtotal()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            this.totalTaxPrice = items.stream()
                    .map(i -> nvl(i.getTaxPrice()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            this.totalCount = items.stream()
                    .map(i -> nvl(i.getQuantity()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        // 优惠金额 = (合计产品价格 + 合计税额) * 优惠率
        BigDecimal baseForDiscount = this.totalProductPrice.add(this.totalTaxPrice);
        BigDecimal percent = nvl(this.discountPercent);
        this.discountPrice = percent.signum() > 0
                ? baseForDiscount.multiply(percent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        // 最终合计价格
        this.totalPrice = baseForDiscount.subtract(this.discountPrice);
        // 本位币折算
        BigDecimal rate = nvl(this.exchangeRate);
        this.baseCurrencyTotalPrice = rate.signum() > 0
                ? this.totalPrice.multiply(rate).setScale(2, RoundingMode.HALF_UP)
                : this.totalPrice;
    }

    // ==================== 领域事件 ====================

    /**
     * 拉取并清空领域事件队列
     *
     * <p>仓储 / 应用服务在持久化后调用此方法，取出事件并通过 Spring 事件机制发布。
     * 发布后清空队列，避免重复发布。
     *
     * @return 待发布的领域事件列表
     */
    public List<Object> pullDomainEvents() {
        List<Object> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return events;
    }

    // ==================== 工具方法 ====================

    /**
     * null 安全的 BigDecimal 取值
     */
    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
