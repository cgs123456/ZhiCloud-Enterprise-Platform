package cn.zhicloud.framework.inventory.service;

import cn.zhicloud.framework.inventory.dal.dataobject.InventoryItemDO;
import cn.zhicloud.framework.inventory.dal.mysql.InventoryItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 库存日终对账服务（M2 阶段 B 安全网）
 *
 * <p>将共享 Starter 的真值源 {@code inventory_item} 与各业务模块通过
 * {@link InventoryProjectionReader} 暴露的只读投影逐一比对，记录数量不一致项，作为双写过渡期的安全网。
 *
 * <p>本服务不反向 import 任何业务模块表，符合 P1-3 模块边界纪律。
 *
 * @author 智云库存治理
 */
@Service
@Slf4j
public class InventoryReconciliationService {

    @Resource
    private InventoryItemMapper inventoryItemMapper;

    /**
     * 由各模块注册（ERP/MES/WMS 在 P1-1 阶段 A 提供实现）。无实现时为空列表，对账结果直接视为一致。
     */
    @Autowired(required = false)
    private List<InventoryProjectionReader> projectionReaders = List.of();

    /**
     * 执行一次对账
     */
    public InventoryReconcileReport reconcile() {
        InventoryReconcileReport report = new InventoryReconcileReport();
        List<InventoryItemDO> truthList = inventoryItemMapper.selectList(new LambdaQueryWrapper<>());
        report.setSourceCount(truthList.size());

        Map<String, InventoryItemDO> truthByKey = truthList.stream()
                .collect(Collectors.toMap(this::compositeKey, e -> e, (a, b) -> a));

        if (CollectionUtils.isEmpty(projectionReaders)) {
            return report;
        }

        for (InventoryProjectionReader reader : projectionReaders) {
            List<InventoryProjection> projections = reader.readAll();
            report.setProjectionCount(report.getProjectionCount() + projections.size());
            for (InventoryProjection p : projections) {
                InventoryItemDO truth = truthByKey.get(compositeKey(p.getItemId(), p.getWarehouseId(),
                        p.getLocationId(), p.getAreaId(), p.getBatchId()));
                BigDecimal truthQty = truth != null ? truth.getQuantity() : BigDecimal.ZERO;
                BigDecimal projQty = p.getQuantity() != null ? p.getQuantity() : BigDecimal.ZERO;
                if (truthQty.compareTo(projQty) != 0) {
                    InventoryReconcileReport.Mismatch m = new InventoryReconcileReport.Mismatch();
                    m.setSource(p.getSource());
                    m.setItemId(p.getItemId());
                    m.setWarehouseId(p.getWarehouseId());
                    m.setLocationId(p.getLocationId());
                    m.setAreaId(p.getAreaId());
                    m.setBatchId(p.getBatchId());
                    m.setTruthQuantity(truthQty);
                    m.setProjectionQuantity(projQty);
                    m.setDescription("真值源与投影库存数量不一致");
                    report.getMismatches().add(m);
                    log.warn("[reconcile][{} 不一致 item={} wh={} truth={} proj={}]",
                            p.getSource(), p.getItemId(), p.getWarehouseId(), truthQty, projQty);
                }
            }
        }
        return report;
    }

    private String compositeKey(InventoryItemDO d) {
        return compositeKey(d.getItemId(), d.getWarehouseId(), d.getLocationId(), d.getAreaId(), d.getBatchId());
    }

    private String compositeKey(Long itemId, Long warehouseId, Long locationId, Long areaId, Long batchId) {
        return String.join("|", String.valueOf(itemId), String.valueOf(warehouseId),
                String.valueOf(Objects.requireNonNullElse(locationId, 0L)),
                String.valueOf(Objects.requireNonNullElse(areaId, 0L)),
                String.valueOf(Objects.requireNonNullElse(batchId, 0L)));
    }

}
