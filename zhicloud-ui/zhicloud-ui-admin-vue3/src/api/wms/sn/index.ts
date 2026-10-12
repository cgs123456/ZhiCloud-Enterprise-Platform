import request from '@/config/axios'

// Wms Sn VO
export interface SnVO {
  id?: number // id
  sn?: string // sn
  productId?: number // productId
  productCode?: string // productCode
  productName?: string // productName
  batchId?: number // batchId
  inventoryId?: number // inventoryId
  status?: string // status
  warehouseId?: number // warehouseId
  zoneId?: number // zoneId
  locationId?: number // locationId
  inboundOrderId?: number // inboundOrderId
  outboundOrderId?: number // outboundOrderId
  boundTime?: number // boundTime
  shippedTime?: number // shippedTime
  remark?: string // remark
  createTime?: number // createTime
}

// Wms Sn API
export const SnApi = {
  SnGenerate: async (data: any) => {
    // 批量生成序列号
    return await request.post({ url: `/wms/sn/generate`, data })
  },
  createSn: async (data: any) => {
    // 创建Sn
    return await request.post({ url: `/wms/sn/create`, data })
  },
  updateSn: async (data: any) => {
    // 更新Sn
    return await request.put({ url: `/wms/sn/update`, data })
  },
  deleteSn: async (id: number) => {
    // 删除Sn
    return await request.delete({ url: `/wms/sn/delete?id=` + id })
  },
  getSn: async (id: number) => {
    // 获得Sn
    return await request.get({ url: `/wms/sn/get?id=` + id })
  },
  SnGetBySn: async (data?: any) => {
    // 按序列号字符串获取
    return await request.get({ url: `/wms/sn/get-by-sn`, params: data })
  },
  getSnPage: async (params: any) => {
    // 获得Sn分页
    return await request.get({ url: `/wms/sn/page`, params })
  },
  SnBind: async (data: any) => {
    // 序列号入库绑定
    return await request.put({ url: `/wms/sn/bind`, data })
  },
  SnShip: async (data: any) => {
    // 序列号出库解绑
    return await request.put({ url: `/wms/sn/ship`, data })
  },
  SnReturn: async (data: any) => {
    // 序列号退货
    return await request.put({ url: `/wms/sn/return`, data })
  },
  SnTrace: async (data?: any) => {
    // 序列号追溯
    return await request.get({ url: `/wms/sn/trace`, params: data })
  },
}
