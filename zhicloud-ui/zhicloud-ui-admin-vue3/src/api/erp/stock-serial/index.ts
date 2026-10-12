import request from '@/config/axios'

// Erp StockSerial VO
export interface StockSerialVO {
  id?: number // id
  serialNo?: string // serialNo
  productId?: number // productId
  warehouseId?: number // warehouseId
  batchId?: number // batchId
  status?: number // status
  remark?: string // remark
  createTime?: number // createTime
}

// Erp StockSerial API
export const StockSerialApi = {
  createStockSerial: async (data: any) => {
    // 创建库存序列号
    return await request.post({ url: `/erp/stock-serial/create`, data })
  },
  updateStockSerial: async (data: any) => {
    // 更新库存序列号
    return await request.put({ url: `/erp/stock-serial/update`, data })
  },
  deleteStockSerial: async (id: number) => {
    // 删除库存序列号
    return await request.delete({ url: `/erp/stock-serial/delete?id=` + id })
  },
  getStockSerial: async (id: number) => {
    // 获得库存序列号
    return await request.get({ url: `/erp/stock-serial/get?id=` + id })
  },
  StockSerialScan: async (serialNo: any) => {
    // 扫码查询库存序列号
    return await request.get({ url: `/erp/stock-serial/scan?serialNo=` + serialNo })
  },
  getStockSerialPage: async (params: any) => {
    // 获得库存序列号分页
    return await request.get({ url: `/erp/stock-serial/page`, params })
  },
}
