import request from '@/config/axios'

export interface InventoryReconcileMismatch {
  source: string
  itemId: number
  warehouseId: number
  locationId?: number
  areaId?: number
  batchId?: number
  truthQuantity: number
  projectionQuantity: number
  description?: string
}

export interface InventoryReconcileReport {
  consistent: boolean
  sourceCount: number
  projectionCount: number
  mismatches: InventoryReconcileMismatch[]
}

export interface InventoryReconcileAlertResult {
  consistent: boolean
  pushed: boolean
  sourceCount: number
  projectionCount: number
  mismatchCount: number
  message: string
}

export const InventoryReconcileApi = {
  /** 手动执行一次只读库存对账 */
  run: async () => {
    return await request.get({ url: `/erp/inventory-reconcile/run` })
  },
  /** 手动执行对账并推送不一致告警 */
  pushAlert: async () => {
    return await request.post({ url: `/erp/inventory-reconcile/alert` })
  }
}
