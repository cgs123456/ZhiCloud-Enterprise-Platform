package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.inventory.service.InventoryProjection;
import cn.iocoder.yudao.framework.inventory.service.InventoryProjectionReader;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ERP 库存投影读取器（P1-1 阶段 A 只读投影）
 *
 * <p>将 {@code erp_stock} 当前库存快照映射为规范 {@link InventoryProjection}，
 * 供共享 Starter 的日终对账服务与真值源 {@code inventory_item} 比对。
 *
 * <p>ERP 库存仅以 (product_id, warehouse_id) 为粒度，无 location / area / batch 维度，
 * 故投影中将这三项置 null，由 {@code InventoryReconciliationService#compositeKey} 统一按 0L 处理，
 * 与双写期真值源 {@code inventory_item} 的 null 维度对齐。
 *
 * <p>本类作为 {@link InventoryProjectionReader} SPI 实现，避免共享 Starter 反向 import ERP 表，严守 P1-3 边界。
 *
 * @author 智云库存治理
 */
@Component
@RequiredArgsConstructor
public class ErpInventoryProjectionReader implements InventoryProjectionReader {

    private final ErpStockMapper stockMapper;

    @Override
    public List<InventoryProjection> readAll() {
        // 空 Wrapper 取全量（含零库存行），避免对账因过滤漏行而误报不一致
        return stockMapper.selectList(new LambdaQueryWrapper<>()).stream()
                .map(this::toProjection)
                .collect(Collectors.toList());
    }

    private InventoryProjection toProjection(ErpStockDO stock) {
        InventoryProjection p = new InventoryProjection();
        p.setSource("erp");
        p.setItemId(stock.getProductId());
        p.setWarehouseId(stock.getWarehouseId());
        p.setLocationId(null);
        p.setAreaId(null);
        p.setBatchId(null);
        p.setQuantity(stock.getCount());
        p.setLockedCount(stock.getLockedCount());
        return p;
    }

}
