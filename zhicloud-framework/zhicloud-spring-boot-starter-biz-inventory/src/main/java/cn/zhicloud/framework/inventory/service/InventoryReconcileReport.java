package cn.zhicloud.framework.inventory.service;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 库存对账报告（M2 日终对账结果）
 *
 * @author 智云库存治理
 */
@Data
public class InventoryReconcileReport {

    /**
     * 不一致项（真值源与投影数量不符、或投影存在真值源缺失的键）
     */
    private final List<Mismatch> mismatches = new ArrayList<>();

    private int sourceCount;
    private int projectionCount;

    public boolean isConsistent() {
        return mismatches.isEmpty();
    }

    @Data
    public static class Mismatch {
        private String source;
        private Long itemId;
        private Long warehouseId;
        private Long locationId;
        private Long areaId;
        private Long batchId;
        private BigDecimal truthQuantity;
        private BigDecimal projectionQuantity;
        private String description;
    }

}
