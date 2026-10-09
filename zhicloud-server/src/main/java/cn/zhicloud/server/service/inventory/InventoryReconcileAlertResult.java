package cn.zhicloud.server.service.inventory;

import lombok.Data;

/**
 * 库存对账手动告警结果。
 */
@Data
public class InventoryReconcileAlertResult {

    private boolean consistent;
    private boolean pushed;
    private int sourceCount;
    private int projectionCount;
    private int mismatchCount;
    private String message;

}
