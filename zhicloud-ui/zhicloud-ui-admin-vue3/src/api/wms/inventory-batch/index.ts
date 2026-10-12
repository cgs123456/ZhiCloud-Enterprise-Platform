import request from '@/config/axios'

// Wms InventoryBatch VO
export interface InventoryBatchVO {
  id?: number // id
  inventoryId?: number // inventoryId
  batchNo?: string // batchNo
  productionDate?: number // productionDate
  expiryDate?: number // expiryDate
  shelfLifeDays?: number // shelfLifeDays
  supplierBatchNo?: string // supplierBatchNo
  quantity?: number // quantity
  lockedQuantity?: number // lockedQuantity
  availableQuantity?: number // availableQuantity
  status?: string // status
  remark?: string // remark
  createTime?: number // createTime
}

// Wms InventoryBatch API
export const InventoryBatchApi = {
  createInventoryBatch: async (data: any) => {
    // 创建库存批次
    return await request.post({ url: `/wms/inventory-batch/create`, data })
  },
  updateInventoryBatch: async (data: any) => {
    // 更新库存批次
    return await request.put({ url: `/wms/inventory-batch/update`, data })
  },
  deleteInventoryBatch: async (id: number) => {
    // 删除库存批次
    return await request.delete({ url: `/wms/inventory-batch/delete?id=` + id })
  },
  getInventoryBatch: async (id: number) => {
    // 获得库存批次
    return await request.get({ url: `/wms/inventory-batch/get?id=` + id })
  },
  getInventoryBatchPage: async (params: any) => {
    // 获得库存批次分页
    return await request.get({ url: `/wms/inventory-batch/page`, params })
  },
  InventoryBatchListByInventory: async (data: any) => {
    // 按库存编号查询批次明细列表
    return await request.get({ url: `/wms/inventory-batch/list-by-inventory`, data })
  },
  InventoryBatchExpiring: async (data: any) => {
    // 查询即将过期的批次（预警）
    return await request.get({ url: `/wms/inventory-batch/expiring`, data })
  },
  InventoryBatchExpired: async (data: any) => {
    // 查询已过期批次
    return await request.get({ url: `/wms/inventory-batch/expired`, data })
  },
  InventoryBatchFifo: async (data: any) => {
    // 应用 FIFO 先进先出策略
    return await request.get({ url: `/wms/inventory-batch/fifo`, data })
  },
  InventoryBatchFefo: async (data: any) => {
    // 应用 FEFO 先到期先出策略
    return await request.get({ url: `/wms/inventory-batch/fefo`, data })
  },
}
