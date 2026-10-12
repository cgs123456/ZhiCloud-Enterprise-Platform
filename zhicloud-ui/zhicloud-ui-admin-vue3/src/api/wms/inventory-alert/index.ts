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
    // 获得库存预警分页
    return await request.get({ url: `/wms/inventory-alert/page`, params })
  },
  getInventoryAlert: async (id: number) => {
    // 获得库存预警
    return await request.get({ url: `/wms/inventory-alert/get?id=` + id })
  },
  InventoryAlertConfirm: async (id: any) => {
    // 确认预警
    return await request.put({ url: `/wms/inventory-alert/confirm?id=` + id })
  },
  InventoryAlertProcess: async (id: any) => {
    // 处理预警
    return await request.put({ url: `/wms/inventory-alert/process?id=` + id })
  },
}
