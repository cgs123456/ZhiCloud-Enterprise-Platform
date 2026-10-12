import request from '@/config/axios'

// Erp StockBatch VO
export interface StockBatchVO {
  id?: number // id
  batchNo?: string // batchNo
  productId?: number // productId
  warehouseId?: number // warehouseId
  productionDate?: number // productionDate
  expiryDate?: number // expiryDate
  quantity?: number // quantity
  status?: number // status
  remark?: string // remark
  createTime?: number // createTime
}

// Erp StockBatch API
export const StockBatchApi = {
  createStockBatch: async (data: any) => {
    // 创建库存批次
    return await request.post({ url: `/erp/stock-batch/create`, data })
  },
  updateStockBatch: async (data: any) => {
    // 更新库存批次
    return await request.put({ url: `/erp/stock-batch/update`, data })
  },
  deleteStockBatch: async (id: number) => {
    // 删除库存批次
    return await request.delete({ url: `/erp/stock-batch/delete?id=` + id })
  },
  getStockBatch: async (id: number) => {
    // 获得库存批次
    return await request.get({ url: `/erp/stock-batch/get?id=` + id })
  },
  getStockBatchPage: async (params: any) => {
    // 获得库存批次分页
    return await request.get({ url: `/erp/stock-batch/page`, params })
  },
  StockBatchExpiringList: async (data: any) => {
    // 获得即将过期的批次列表
    return await request.get({ url: `/erp/stock-batch/expiring-list`, data })
  },
}
