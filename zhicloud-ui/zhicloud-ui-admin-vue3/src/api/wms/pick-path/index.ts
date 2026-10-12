import request from '@/config/axios'

// Wms PickPath VO
export interface PickPathVO {
  orderId?: number // orderId
  orderNo?: string // orderNo
  warehouseId?: number // warehouseId
  warehouseName?: string // warehouseName
  strategy?: string // strategy
  locationCount?: number // locationCount
  totalQuantity?: number // totalQuantity
  estimatedDistance?: number // estimatedDistance
  locations?: any[] // locations
}

// Wms PickPath API
export const PickPathApi = {
  optimizePickPath: async (shipmentOrderId: number) => {
    // 优化单出库单拣货路径
    return await request.get({ url: `/wms/pick-path/optimize/` + shipmentOrderId })
  },
  optimizeBatchPickPath: async (waveOrderId: number) => {
    // 优化波次拣货路径
    return await request.get({ url: `/wms/pick-path/optimize-wave/` + waveOrderId })
  },
}
