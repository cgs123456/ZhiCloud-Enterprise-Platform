package cn.zhicloud.module.mes.service.wm.materialstock;

import cn.zhicloud.framework.inventory.service.InventoryProjection;
import cn.zhicloud.framework.inventory.service.InventoryProjectionReader;
import cn.zhicloud.module.mes.dal.dataobject.wm.materialstock.MesWmMaterialStockDO;
import cn.zhicloud.module.mes.dal.mysql.wm.materialstock.MesWmMaterialStockMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MES 库存投影读取器（P1-1 阶段 A 只读投影）
 *
 * <p>将 {@code mes_wm_material_stock} 当前库存快照映射为规范 {@link InventoryProjection}，
 * 供共享 Starter 的日终对账服务与真值源 {@code inventory_item} 比对。
 *
 * <p>MES 库存台账已具备 (item_id, warehouse_id, location_id, area_id, batch_id) 完整复合维度，
 * 与共享 Starter 规范键天然对齐；MES 无「锁定」概念，故 {@code lockedCount} 置 null（对账仅比对数量）。
 *
 * <p>本类作为 {@link InventoryProjectionReader} SPI 实现，避免共享 Starter 反向 import MES 表，严守 P1-3 边界。
 *
 * @author 智云库存治理
 */
@Component
@RequiredArgsConstructor
public class MesInventoryProjectionReader implements InventoryProjectionReader {

    private final MesWmMaterialStockMapper materialStockMapper;

    @Override
    public List<InventoryProjection> readAll() {
        // 使用空 Wrapper 取全量（含零库存行），避免对账因过滤漏行而误报不一致
        return materialStockMapper.selectList(new LambdaQueryWrapper<>()).stream()
                .map(this::toProjection)
                .collect(Collectors.toList());
    }

    private InventoryProjection toProjection(MesWmMaterialStockDO stock) {
        InventoryProjection p = new InventoryProjection();
        p.setSource("mes");
        p.setItemId(stock.getItemId());
        p.setWarehouseId(stock.getWarehouseId());
        p.setLocationId(stock.getLocationId());
        p.setAreaId(stock.getAreaId());
        p.setBatchId(stock.getBatchId());
        p.setQuantity(stock.getQuantity());
        p.setLockedCount(null); // MES 无锁定概念
        return p;
    }

}
