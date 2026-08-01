package cn.iocoder.yudao.module.erp.query.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderMapper;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

/**
 * 销售订单查询服务实现（CQRS 读模型）
 *
 * <p>只读查询实现，复用现有 {@link ErpSaleOrderMapper} / {@link ErpSaleOrderItemMapper}，
 * 将 DO 投影为查询 VO。统计查询在内存中聚合（试点阶段），生产环境可改为专用统计 SQL。
 *
 * @author DDD 试点
 */
@Service
@Validated
public class SaleOrderQueryServiceImpl implements SaleOrderQueryService {

    @Resource
    private ErpSaleOrderMapper saleOrderMapper;
    @Resource
    private ErpSaleOrderItemMapper saleOrderItemMapper;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpProductService productService;

    @Override
    public PageResult<SaleOrderSummaryVO> querySummary(ErpSaleOrderPageReqVO pageReqVO) {
        PageResult<ErpSaleOrderDO> pageResult = saleOrderMapper.selectPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(pageResult.getTotal());
        }
        // 批量加载客户名称，避免 N+1 查询
        Set<Long> customerIds = convertSet(pageResult.getList(), ErpSaleOrderDO::getCustomerId);
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(customerIds);
        // 显式投影为摘要 VO（字段名与 DO 不完全一致，故手动映射）
        List<SaleOrderSummaryVO> list = convertList(pageResult.getList(), order -> {
            SaleOrderSummaryVO vo = new SaleOrderSummaryVO();
            vo.setId(order.getId());
            vo.setOrderNo(order.getNo());
            vo.setCustomerId(order.getCustomerId());
            ErpCustomerDO customer = customerMap.get(order.getCustomerId());
            vo.setCustomerName(customer == null ? null : customer.getName());
            vo.setTotalAmount(order.getTotalPrice());
            vo.setStatus(order.getStatus());
            vo.setOrderDate(order.getOrderTime());
            vo.setOutCount(order.getOutCount());
            vo.setReturnCount(order.getReturnCount());
            return vo;
        });
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public SaleOrderDetailVO queryDetail(Long orderId) {
        ErpSaleOrderDO order = saleOrderMapper.selectById(orderId);
        if (order == null) {
            return null;
        }
        List<ErpSaleOrderItemDO> itemDOs = saleOrderItemMapper.selectListByOrderId(orderId);
        // 批量加载产品与客户名称
        Set<Long> productIds = convertSet(itemDOs, ErpSaleOrderItemDO::getProductId);
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(productIds);
        ErpCustomerDO customer = customerService.getCustomer(order.getCustomerId());

        // 投影订单头
        SaleOrderDetailVO vo = new SaleOrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getNo());
        vo.setCustomerId(order.getCustomerId());
        vo.setCustomerName(customer == null ? null : customer.getName());
        vo.setTotalAmount(order.getTotalPrice());
        vo.setStatus(order.getStatus());
        vo.setOrderDate(order.getOrderTime());
        vo.setTotalCount(order.getTotalCount());
        vo.setOutCount(order.getOutCount());
        vo.setReturnCount(order.getReturnCount());
        vo.setRemark(order.getRemark());
        // 投影明细
        List<SaleOrderDetailVO.Item> items = convertList(itemDOs, item -> {
            SaleOrderDetailVO.Item itemVO = new SaleOrderDetailVO.Item();
            itemVO.setItemId(item.getId());
            itemVO.setProductId(item.getProductId());
            ErpProductRespVO product = productMap.get(item.getProductId());
            itemVO.setProductName(product == null ? null : product.getName());
            itemVO.setQuantity(item.getCount());
            itemVO.setUnitPrice(item.getProductPrice());
            itemVO.setAmount(item.getTotalPrice());
            itemVO.setOutCount(item.getOutCount());
            itemVO.setReturnCount(item.getReturnCount());
            return itemVO;
        });
        vo.setItems(items);
        return vo;
    }

    @Override
    public SaleOrderStatisticsVO queryStatistics(LocalDate start, LocalDate end) {
        // 1. 按下单时间区间查询订单（复用现有 Mapper，试点阶段在内存聚合）
        LambdaQueryWrapper<ErpSaleOrderDO> wrapper = new LambdaQueryWrapper<>();
        if (start != null) {
            wrapper.ge(ErpSaleOrderDO::getOrderTime, start.atStartOfDay());
        }
        if (end != null) {
            // end 为含当天，故使用次日零点作为上界
            wrapper.lt(ErpSaleOrderDO::getOrderTime, end.plusDays(1).atStartOfDay());
        }
        List<ErpSaleOrderDO> orders = saleOrderMapper.selectList(wrapper);

        // 2. 总量统计
        SaleOrderStatisticsVO vo = new SaleOrderStatisticsVO();
        vo.setTotalOrderCount(orders.size());
        vo.setTotalAmount(orders.stream()
                .map(this::safeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // 3. 按状态分组聚合
        Map<Integer, List<ErpSaleOrderDO>> byStatus = orders.stream()
                .collect(Collectors.groupingBy(o -> Objects.requireNonNullElse(o.getStatus(), 0)));
        List<SaleOrderStatisticsVO.StatusStat> statusStats = new ArrayList<>();
        byStatus.forEach((status, group) -> {
            SaleOrderStatisticsVO.StatusStat stat = new SaleOrderStatisticsVO.StatusStat();
            stat.setStatus(status);
            stat.setCount(group.size());
            stat.setAmount(group.stream().map(this::safeAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
            statusStats.add(stat);
        });
        vo.setByStatus(statusStats);

        // 4. 按客户分组聚合
        Map<Long, List<ErpSaleOrderDO>> byCustomer = orders.stream()
                .filter(o -> o.getCustomerId() != null)
                .collect(Collectors.groupingBy(ErpSaleOrderDO::getCustomerId));
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(byCustomer.keySet());
        List<SaleOrderStatisticsVO.CustomerStat> customerStats = new ArrayList<>();
        byCustomer.forEach((customerId, group) -> {
            SaleOrderStatisticsVO.CustomerStat stat = new SaleOrderStatisticsVO.CustomerStat();
            stat.setCustomerId(customerId);
            ErpCustomerDO customer = customerMap.get(customerId);
            stat.setCustomerName(customer == null ? null : customer.getName());
            stat.setCount(group.size());
            stat.setAmount(group.stream().map(this::safeAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
            customerStats.add(stat);
        });
        vo.setByCustomer(customerStats);
        return vo;
    }

    /**
     * 安全获取订单金额，规避 null
     */
    private BigDecimal safeAmount(ErpSaleOrderDO order) {
        return order.getTotalPrice() == null ? BigDecimal.ZERO : order.getTotalPrice();
    }

}
