import request from '@/config/axios'

// Wms BatchExpiryAlert VO
export interface BatchExpiryAlertVO {
  id?: number // id
  alertType?: string // alertType
  warehouseId?: number // warehouseId
  warehouseName?: string // warehouseName
  productId?: number // productId
  skuCode?: string // skuCode
  skuName?: string // skuName
  batchNo?: string // batchNo
  supplierBatchNo?: string // supplierBatchNo
  productionDate?: number // productionDate
  expiryDate?: number // expiryDate
  shelfLifeDays?: number // shelfLifeDays
  currentQuantity?: number // currentQuantity
  thresholdValue?: number // thresholdValue
  alertTime?: number // alertTime
  status?: number // status
  remark?: string // remark
  createTime?: number // createTime
}

// Wms BatchExpiryAlert API
export const BatchExpiryAlertApi = {
  getBatchExpiryAlertPage: async (params: any) => {
    // 获得BatchExpiryAlert分页
    return await request.get({ url: `/wms/batch-expiry-alert/page`, params })
  },
  BatchExpiryAlertScan: async (data: any) => {
    // 手动触发批次效期扫描
    return await request.post({ url: `/wms/batch-expiry-alert/scan`, data })
  },
}
