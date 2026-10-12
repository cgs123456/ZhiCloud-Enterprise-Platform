import request from '@/config/axios'

// Erp VmiInventory VO
export interface VmiInventoryVO {
  id?: number // id
  supplierId?: number // supplierId
  warehouseId?: number // warehouseId
  productId?: number // productId
  productName?: string // productName
  quantity?: number // quantity
  availableQuantity?: number // availableQuantity
  lockedQuantity?: number // lockedQuantity
  minQuantity?: number // minQuantity
  maxQuantity?: number // maxQuantity
  replenishmentPoint?: number // replenishmentPoint
  remark?: string // remark
  createTime?: number // createTime
}

// Erp VmiInventory API
export const VmiInventoryApi = {
  createVmiInventory: async (data: any) => {
    // 创建VmiInventory
    return await request.post({ url: `/erp/vmi-inventory/create`, data })
  },
  updateVmiInventory: async (data: any) => {
    // 更新VmiInventory
    return await request.put({ url: `/erp/vmi-inventory/update`, data })
  },
  deleteVmiInventory: async (id: number) => {
    // 删除VmiInventory
    return await request.delete({ url: `/erp/vmi-inventory/delete?id=` + id })
  },
  getVmiInventory: async (id: number) => {
    // 获得VmiInventory
    return await request.get({ url: `/erp/vmi-inventory/get?id=` + id })
  },
  getVmiInventoryPage: async (params: any) => {
    // 获得VmiInventory分页
    return await request.get({ url: `/erp/vmi-inventory/page`, params })
  },
  VmiInventoryCheckReplenishment: async (data?: any) => {
    // 检查补货点，返回需要补货的库存列表
    return await request.get({ url: `/erp/vmi-inventory/check-replenishment`, params: data })
  },
}
