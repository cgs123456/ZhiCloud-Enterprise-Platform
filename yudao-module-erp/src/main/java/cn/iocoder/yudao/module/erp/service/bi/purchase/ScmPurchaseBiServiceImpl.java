package cn.iocoder.yudao.module.erp.service.bi.purchase;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.bi.vo.ScmBiPriceFluctuationRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.bi.vo.ScmBiPurchaseOnTimeRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.bi.vo.ScmBiSupplierScoreRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 供应链 BI 采购分析 Service 实现类
 *
 * @author 芋道源码
 */
@Service
public class ScmPurchaseBiServiceImpl implements ScmPurchaseBiService {

    /**
     * 采购到货及时阈值（天）：入库时间 - 下单时间 <= 7 天视为及时
     */
    private static final long ON_TIME_THRESHOLD_DAYS = 7L;

    @Resource
    private ErpPurchaseInMapper purchaseInMapper;
    @Resource
    private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Resource
    private ErpPurchaseOrderMapper purchaseOrderMapper;
    @Resource
    private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Resource
    private ErpSupplierMapper supplierMapper;

    @Override
    public ScmBiPurchaseOnTimeRespVO getPurchaseOnTimeRate(LocalDateTime beginTime, LocalDateTime endTime) {
        // 1. 查询区间内已审核的采购入库单
        List<ErpPurchaseInDO> inList = purchaseInMapper.selectList(new LambdaQueryWrapperX<ErpPurchaseInDO>()
                .geIfPresent(ErpPurchaseInDO::getInTime, beginTime)
                .leIfPresent(ErpPurchaseInDO::getInTime, endTime));
        if (inList.isEmpty()) {
            ScmBiPurchaseOnTimeRespVO respVO = new ScmBiPurchaseOnTimeRespVO();
            respVO.setTotalOrders(0);
            respVO.setOnTimeOrders(0);
            respVO.setOnTimeRate(BigDecimal.ZERO);
            return respVO;
        }
        // 2. 加载关联采购订单，计算到货及时性
        List<Long> orderIds = inList.stream().map(ErpPurchaseInDO::getOrderId)
                .filter(id -> id != null).distinct().collect(Collectors.toList());
        Map<Long, ErpPurchaseOrderDO> orderMap = orderIds.isEmpty()
                ? Map.of()
                : purchaseOrderMapper.selectByIds(orderIds).stream()
                .collect(Collectors.toMap(ErpPurchaseOrderDO::getId, o -> o));
        int onTime = 0;
        for (ErpPurchaseInDO in : inList) {
            ErpPurchaseOrderDO order = orderMap.get(in.getOrderId());
            if (order == null || order.getOrderTime() == null || in.getInTime() == null) {
                continue;
            }
            long days = Duration.between(order.getOrderTime(), in.getInTime()).toDays();
            if (days <= ON_TIME_THRESHOLD_DAYS) {
                onTime++;
            }
        }
        BigDecimal onTimeRate = BigDecimal.valueOf(onTime)
                .divide(BigDecimal.valueOf(inList.size()), 4, RoundingMode.HALF_UP);
        ScmBiPurchaseOnTimeRespVO respVO = new ScmBiPurchaseOnTimeRespVO();
        respVO.setTotalOrders(inList.size());
        respVO.setOnTimeOrders(onTime);
        respVO.setOnTimeRate(onTimeRate);
        return respVO;
    }

    @Override
    public List<ScmBiPriceFluctuationRespVO> getPriceFluctuation(LocalDateTime beginTime, LocalDateTime endTime) {
        // 1. 查询区间内的采购入库明细，按产品分组统计采购价
        List<ErpPurchaseInItemDO> itemList = purchaseInItemMapper.selectList(
                new LambdaQueryWrapperX<ErpPurchaseInItemDO>()
                        .geIfPresent(ErpPurchaseInItemDO::getCreateTime, beginTime)
                        .leIfPresent(ErpPurchaseInItemDO::getCreateTime, endTime));
        Map<Long, List<ErpPurchaseInItemDO>> grouped = itemList.stream()
                .filter(i -> i.getProductId() != null && i.getProductPrice() != null)
                .collect(Collectors.groupingBy(ErpPurchaseInItemDO::getProductId));
        List<ScmBiPriceFluctuationRespVO> result = new ArrayList<>();
        grouped.forEach((productId, items) -> {
            BigDecimal sum = BigDecimal.ZERO;
            BigDecimal max = items.get(0).getProductPrice();
            BigDecimal min = items.get(0).getProductPrice();
            for (ErpPurchaseInItemDO item : items) {
                BigDecimal price = item.getProductPrice();
                sum = sum.add(price);
                if (price.compareTo(max) > 0) {
                    max = price;
                }
                if (price.compareTo(min) < 0) {
                    min = price;
                }
            }
            BigDecimal avg = sum.divide(BigDecimal.valueOf(items.size()), 4, RoundingMode.HALF_UP);
            BigDecimal fluctuationRate = avg.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : max.subtract(min).divide(avg, 4, RoundingMode.HALF_UP);
            ScmBiPriceFluctuationRespVO respVO = new ScmBiPriceFluctuationRespVO();
            respVO.setProductId(productId);
            respVO.setAvgPrice(avg);
            respVO.setMaxPrice(max);
            respVO.setMinPrice(min);
            respVO.setFluctuationRate(fluctuationRate);
            result.add(respVO);
        });
        return result;
    }

    @Override
    public List<ScmBiSupplierScoreRespVO> getSupplierPerformance(LocalDateTime beginTime, LocalDateTime endTime) {
        // 1. 区间内采购入库单按供应商分组
        List<ErpPurchaseInDO> inList = purchaseInMapper.selectList(new LambdaQueryWrapperX<ErpPurchaseInDO>()
                .geIfPresent(ErpPurchaseInDO::getInTime, beginTime)
                .leIfPresent(ErpPurchaseInDO::getInTime, endTime));
        Map<Long, List<ErpPurchaseInDO>> inBySupplier = inList.stream()
                .filter(i -> i.getSupplierId() != null)
                .collect(Collectors.groupingBy(ErpPurchaseInDO::getSupplierId));
        // 2. 区间内采购退货单按供应商分组（用于质量合格率）
        List<ErpPurchaseReturnDO> returnList = purchaseReturnMapper.selectList(
                new LambdaQueryWrapperX<ErpPurchaseReturnDO>()
                        .geIfPresent(ErpPurchaseReturnDO::getReturnTime, beginTime)
                        .leIfPresent(ErpPurchaseReturnDO::getReturnTime, endTime));
        Map<Long, Long> returnCountBySupplier = returnList.stream()
                .filter(r -> r.getSupplierId() != null)
                .collect(Collectors.groupingBy(ErpPurchaseReturnDO::getSupplierId, Collectors.counting()));
        // 3. 加载供应商名称
        Map<Long, String> supplierNameMap = supplierMapper.selectList(null).stream()
                .collect(Collectors.toMap(ErpSupplierDO::getId, ErpSupplierDO::getName));
        // 4. 预加载所有关联订单（避免N+1查询）
        Set<Long> allOrderIds = inList.stream()
                .map(ErpPurchaseInDO::getOrderId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, ErpPurchaseOrderDO> allOrdersMap = allOrderIds.isEmpty()
                ? Map.of()
                : purchaseOrderMapper.selectByIds(new ArrayList<>(allOrderIds)).stream()
                .collect(Collectors.toMap(ErpPurchaseOrderDO::getId, o -> o));
        // 5. 计算各供应商绩效
        List<ScmBiSupplierScoreRespVO> result = new ArrayList<>();
        inBySupplier.forEach((supplierId, ins) -> {
            int onTime = 0;
            for (ErpPurchaseInDO in : ins) {
                if (in.getInTime() == null) {
                    continue;
                }
                ErpPurchaseOrderDO order = in.getOrderId() == null ? null
                        : allOrdersMap.get(in.getOrderId());
                if (order == null || order.getOrderTime() == null) {
                    continue;
                }
                long days = Duration.between(order.getOrderTime(), in.getInTime()).toDays();
                if (days <= ON_TIME_THRESHOLD_DAYS) {
                    onTime++;
                }
            }
            BigDecimal onTimeRate = BigDecimal.valueOf(onTime)
                    .divide(BigDecimal.valueOf(ins.size()), 4, RoundingMode.HALF_UP);
            // 质量合格率 = 1 - 退货单数 / 入库单数
            long returnCount = returnCountBySupplier.getOrDefault(supplierId, 0L);
            BigDecimal qualityRate = BigDecimal.valueOf(Math.max(0, ins.size() - returnCount))
                    .divide(BigDecimal.valueOf(ins.size()), 4, RoundingMode.HALF_UP);
            // 综合评分 = 到货及时率 * 60 + 质量合格率 * 40
            BigDecimal overallScore = onTimeRate.multiply(BigDecimal.valueOf(60))
                    .add(qualityRate.multiply(BigDecimal.valueOf(40)));
            ScmBiSupplierScoreRespVO respVO = new ScmBiSupplierScoreRespVO();
            respVO.setSupplierId(supplierId);
            respVO.setSupplierName(supplierNameMap.get(supplierId));
            respVO.setOnTimeRate(onTimeRate);
            respVO.setQualityRate(qualityRate);
            respVO.setOverallScore(overallScore);
            result.add(respVO);
        });
        return result;
    }

}
