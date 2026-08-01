package cn.iocoder.yudao.module.erp.service.bi.sale;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.bi.vo.ScmBiOrderFulfillmentRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.bi.vo.ScmBiStockoutRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 供应链 BI 销售分析 Service 实现类
 *
 * @author 芋道源码
 */
@Service
public class ScmSaleBiServiceImpl implements ScmSaleBiService {

    @Resource
    private ErpSaleOrderMapper saleOrderMapper;
    @Resource
    private ErpStockMapper stockMapper;
    @Resource
    private ErpProductMapper productMapper;

    @Override
    public ScmBiOrderFulfillmentRespVO getOrderFulfillmentRate(LocalDateTime beginTime, LocalDateTime endTime) {
        // 1. 查询区间内的销售订单
        List<ErpSaleOrderDO> orderList = saleOrderMapper.selectList(new LambdaQueryWrapperX<ErpSaleOrderDO>()
                .geIfPresent(ErpSaleOrderDO::getOrderTime, beginTime)
                .leIfPresent(ErpSaleOrderDO::getOrderTime, endTime));
        if (orderList.isEmpty()) {
            ScmBiOrderFulfillmentRespVO respVO = new ScmBiOrderFulfillmentRespVO();
            respVO.setTotalOrders(0);
            respVO.setFulfilledOrders(0);
            respVO.setFulfillmentRate(BigDecimal.ZERO);
            return respVO;
        }
        // 2. 已履约 = 出库数量 >= 合计数量（且合计数量 > 0）
        int fulfilled = 0;
        for (ErpSaleOrderDO order : orderList) {
            BigDecimal total = order.getTotalCount() == null ? BigDecimal.ZERO : order.getTotalCount();
            BigDecimal out = order.getOutCount() == null ? BigDecimal.ZERO : order.getOutCount();
            if (total.compareTo(BigDecimal.ZERO) > 0 && out.compareTo(total) >= 0) {
                fulfilled++;
            }
        }
        BigDecimal fulfillmentRate = BigDecimal.valueOf(fulfilled)
                .divide(BigDecimal.valueOf(orderList.size()), 4, RoundingMode.HALF_UP);
        ScmBiOrderFulfillmentRespVO respVO = new ScmBiOrderFulfillmentRespVO();
        respVO.setTotalOrders(orderList.size());
        respVO.setFulfilledOrders(fulfilled);
        respVO.setFulfillmentRate(fulfillmentRate);
        return respVO;
    }

    @Override
    public ScmBiStockoutRespVO getStockoutRate() {
        // 1. 产品总数
        List<ErpProductDO> productList = productMapper.selectList(null);
        Set<Long> productIds = productList.stream().map(ErpProductDO::getId).collect(Collectors.toSet());
        if (productIds.isEmpty()) {
            ScmBiStockoutRespVO respVO = new ScmBiStockoutRespVO();
            respVO.setTotalProducts(0);
            respVO.setStockoutProducts(0);
            respVO.setStockoutRate(BigDecimal.ZERO);
            return respVO;
        }
        // 2. 统计库存 <= 0 的产品数（按产品聚合：所有仓库库存合计 <= 0 视为缺货）
        List<ErpStockDO> stockList = stockMapper.selectList(null);
        java.util.Map<Long, BigDecimal> stockSumByProduct = new java.util.HashMap<>();
        for (ErpStockDO stock : stockList) {
            BigDecimal count = stock.getCount() == null ? BigDecimal.ZERO : stock.getCount();
            stockSumByProduct.merge(stock.getProductId(), count, BigDecimal::add);
        }
        int stockout = 0;
        for (Long productId : productIds) {
            BigDecimal sum = stockSumByProduct.getOrDefault(productId, BigDecimal.ZERO);
            if (sum.compareTo(BigDecimal.ZERO) <= 0) {
                stockout++;
            }
        }
        BigDecimal stockoutRate = BigDecimal.valueOf(stockout)
                .divide(BigDecimal.valueOf(productIds.size()), 4, RoundingMode.HALF_UP);
        ScmBiStockoutRespVO respVO = new ScmBiStockoutRespVO();
        respVO.setTotalProducts(productIds.size());
        respVO.setStockoutProducts(stockout);
        respVO.setStockoutRate(stockoutRate);
        return respVO;
    }

}
