import request from '@/config/axios'

// Wms InventoryAlert VO
export interface InventoryAlertVO {
  id?: number // id
  alertType?: string // alertType
  warehouseId?: number // warehouseId
  warehouseName?: string // warehouseName
  productId?: number // productId
  skuCode?: string // skuCode
  skuName?: string // skuName
  batchNo?: string // batchNo
  currentQuantity?: number // currentQuantity
  thresholdValue?: number // thresholdValue
  alertTime?: number // alertTime
  status?: number // status
  remark?: string // remark
  createTime?: number // createTime
}

// Wms InventoryAlert API
export const InventoryAlertApi = {
  getInventoryAlertPage: async (params: any) => {
    // 获得InventoryAlert分页
    return await request.get({ url: `/wms/inventory-alert/page`, params })
  },
  getInventoryAlert: async (id: number) => {
    // 获得InventoryAlert
    return await request.get({ url: `/wms/inventory-alert/get?id=` + id })
  },
  InventoryAlertConfirm: async (data: any) => {
    // 确认预警
    return await request.put({ url: `/wms/inventory-alert/confirm`, data })
  },
  InventoryAlertProcess: async (data: any) => {
    // 处理预警
    return await request.put({ url: `/wms/inventory-alert/process`, data })
  },
}
