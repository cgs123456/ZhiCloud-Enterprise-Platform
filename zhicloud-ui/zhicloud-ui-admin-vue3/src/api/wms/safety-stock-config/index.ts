import request from '@/config/axios'

// Wms SafetyStockConfig VO
export interface SafetyStockConfigVO {
  id?: number // id
  warehouseId?: number // warehouseId
  warehouseName?: string // warehouseName
  productId?: number // productId
  skuCode?: string // skuCode
  skuName?: string // skuName
  safetyStock?: number // safetyStock
  maxStock?: number // maxStock
  minStock?: number // minStock
  remark?: string // remark
  createTime?: number // createTime
}

// Wms SafetyStockConfig API
export const SafetyStockConfigApi = {
  createSafetyStockConfig: async (data: any) => {
    // 创建安全库存配置
    return await request.post({ url: `/wms/safety-stock-config/create`, data })
  },
  updateSafetyStockConfig: async (data: any) => {
    // 更新安全库存配置
    return await request.put({ url: `/wms/safety-stock-config/update`, data })
  },
  deleteSafetyStockConfig: async (id: number) => {
    // 删除安全库存配置
    return await request.delete({ url: `/wms/safety-stock-config/delete?id=` + id })
  },
  getSafetyStockConfig: async (id: number) => {
    // 获得安全库存配置
    return await request.get({ url: `/wms/safety-stock-config/get?id=` + id })
  },
  getSafetyStockConfigPage: async (params: any) => {
    // 获得安全库存配置分页
    return await request.get({ url: `/wms/safety-stock-config/page`, params })
  },
}
