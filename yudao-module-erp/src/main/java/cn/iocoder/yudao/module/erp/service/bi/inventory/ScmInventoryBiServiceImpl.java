package cn.iocoder.yudao.module.erp.service.bi.inventory;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.bi.vo.ScmBiAgingRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.bi.vo.ScmBiInventoryTurnoverRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.bi.vo.ScmBiSlowMovingRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockRecordMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 供应链 BI 库存分析 Service 实现类
 *
 * @author 芋道源码
 */
@Service
public class ScmInventoryBiServiceImpl implements ScmInventoryBiService {

    @Resource
    private ErpStockMapper stockMapper;
    @Resource
    private ErpStockRecordMapper stockRecordMapper;
    @Resource
    private ErpProductMapper productMapper;

    @Override
    public ScmBiInventoryTurnoverRespVO getInventoryTurnoverRate(LocalDateTime beginTime, LocalDateTime endTime) {
        // 1. 加载所有产品，构建 id -> purchasePrice 的 Map
        Map<Long, BigDecimal> productPriceMap = productMapper.selectList(null).stream()
                .collect(Collectors.toMap(ErpProductDO::getId,
                        p -> p.getPurchasePrice() == null ? BigDecimal.ZERO : p.getPurchasePrice()));
        // 2. 出库总成本 = 出库记录 |count| * 采购单价
        List<ErpStockRecordDO> outRecords = stockRecordMapper.selectList(new LambdaQueryWrapperX<ErpStockRecordDO>()
                .ltIfPresent(ErpStockRecordDO::getCount, BigDecimal.ZERO)
                .geIfPresent(ErpStockRecordDO::getCreateTime, beginTime)
                .leIfPresent(ErpStockRecordDO::getCreateTime, endTime));
        BigDecimal outCostAmount = BigDecimal.ZERO;
        for (ErpStockRecordDO record : outRecords) {
            BigDecimal price = productPriceMap.getOrDefault(record.getProductId(), BigDecimal.ZERO);
            outCostAmount = outCostAmount.add(record.getCount().abs().multiply(price));
        }
        // 3. 平均库存金额 = 当前库存数量 * 采购单价
        List<ErpStockDO> stockList = stockMapper.selectList(null);
        BigDecimal inventoryAmount = BigDecimal.ZERO;
        for (ErpStockDO stock : stockList) {
            BigDecimal price = productPriceMap.getOrDefault(stock.getProductId(), BigDecimal.ZERO);
            BigDecimal count = stock.getCount() == null ? BigDecimal.ZERO : stock.getCount();
            inventoryAmount = inventoryAmount.add(count.multiply(price));
        }
        // 4. 周转率 = 出库总成本 / 平均库存金额
        BigDecimal turnoverRate = inventoryAmount.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : outCostAmount.divide(inventoryAmount, 4, RoundingMode.HALF_UP);
        ScmBiInventoryTurnoverRespVO respVO = new ScmBiInventoryTurnoverRespVO();
        respVO.setOutCostAmount(outCostAmount);
        respVO.setAvgInventoryAmount(inventoryAmount);
        respVO.setTurnoverRate(turnoverRate);
        return respVO;
    }

    @Override
    public List<ScmBiAgingRespVO> getInventoryAging() {
        List<ErpStockDO> stockList = stockMapper.selectList(null);
        if (stockList.isEmpty()) {
            return new ArrayList<>();
        }
        // 加载产品名称
        Map<Long, String> productNameMap = productMapper.selectList(null).stream()
                .collect(Collectors.toMap(ErpProductDO::getId, ErpProductDO::getName));
        // 加载所有库存记录，按 product+warehouse 分组取最近一条
        List<ErpStockRecordDO> allRecords = stockRecordMapper.selectList(null);
        Map<String, LocalDateTime> latestMoveTimeMap = allRecords.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getProductId() + "_" + r.getWarehouseId(),
                        Collectors.mapping(ErpStockRecordDO::getCreateTime,
                                Collectors.maxBy(Comparator.nullsFirst(Comparator.naturalOrder())))))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().orElse(null)));
        LocalDateTime now = LocalDateTime.now();
        List<ScmBiAgingRespVO> result = new ArrayList<>();
        for (ErpStockDO stock : stockList) {
            String key = stock.getProductId() + "_" + stock.getWarehouseId();
            LocalDateTime latestTime = latestMoveTimeMap.get(key);
            if (latestTime == null) {
                continue;
            }
            int ageDays = (int) Duration.between(latestTime, now).toDays();
            ScmBiAgingRespVO respVO = new ScmBiAgingRespVO();
            respVO.setProductId(stock.getProductId());
            respVO.setProductName(productNameMap.get(stock.getProductId()));
            respVO.setWarehouseId(stock.getWarehouseId());
            respVO.setQuantity(stock.getCount());
            respVO.setAgeDays(ageDays);
            respVO.setAgingBucket(buildAgingBucket(ageDays));
            result.add(respVO);
        }
        return result;
    }

    private String buildAgingBucket(int ageDays) {
        if (ageDays <= 30) {
            return "0-30";
        } else if (ageDays <= 60) {
            return "30-60";
        } else if (ageDays <= 90) {
            return "60-90";
        }
        return "90+";
    }

    @Override
    public List<ScmBiSlowMovingRespVO> getSlowMovingInventory(Integer idleDays) {
        int threshold = idleDays == null ? 90 : idleDays;
        List<ErpStockDO> stockList = stockMapper.selectList(null);
        if (stockList.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, String> productNameMap = productMapper.selectList(null).stream()
                .collect(Collectors.toMap(ErpProductDO::getId, ErpProductDO::getName));
        List<ErpStockRecordDO> allRecords = stockRecordMapper.selectList(null);
        Map<String, LocalDateTime> latestMoveTimeMap = allRecords.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getProductId() + "_" + r.getWarehouseId(),
                        Collectors.mapping(ErpStockRecordDO::getCreateTime,
                                Collectors.maxBy(Comparator.nullsFirst(Comparator.naturalOrder())))))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().orElse(null)));
        LocalDateTime now = LocalDateTime.now();
        List<ScmBiSlowMovingRespVO> result = new ArrayList<>();
        for (ErpStockDO stock : stockList) {
            String key = stock.getProductId() + "_" + stock.getWarehouseId();
            LocalDateTime latestTime = latestMoveTimeMap.get(key);
            if (latestTime == null) {
                continue;
            }
            int days = (int) Duration.between(latestTime, now).toDays();
            if (days < threshold) {
                continue;
            }
            ScmBiSlowMovingRespVO respVO = new ScmBiSlowMovingRespVO();
            respVO.setProductId(stock.getProductId());
            respVO.setProductName(productNameMap.get(stock.getProductId()));
            respVO.setWarehouseId(stock.getWarehouseId());
            respVO.setQuantity(stock.getCount());
            respVO.setLastMoveTime(latestTime);
            respVO.setIdleDays(days);
            result.add(respVO);
        }
        return result;
    }

}
