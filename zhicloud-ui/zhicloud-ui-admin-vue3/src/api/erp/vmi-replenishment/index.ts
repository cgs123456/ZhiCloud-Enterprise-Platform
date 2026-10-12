import request from '@/config/axios'

// Erp VmiReplenishment VO
export interface VmiReplenishmentVO {
  id?: number // id
  no?: string // no
  supplierId?: number // supplierId
  warehouseId?: number // warehouseId
  status?: number // status
  totalQuantity?: number // totalQuantity
  remark?: string // remark
  createTime?: number // createTime
  items?: any[] // items
}

// Erp VmiReplenishment API
export const VmiReplenishmentApi = {
  deleteVmiReplenishment: async (id: number) => {
    // 删除VmiReplenishment
    return await request.delete({ url: `/erp/vmi-replenishment/delete?id=` + id })
  },
  getVmiReplenishment: async (id: number) => {
    // 获得VmiReplenishment
    return await request.get({ url: `/erp/vmi-replenishment/get?id=` + id })
  },
  getVmiReplenishmentPage: async (params: any) => {
    // 获得VmiReplenishment分页
    return await request.get({ url: `/erp/vmi-replenishment/page`, params })
  },
  VmiReplenishmentGenerate: async (data: any) => {
    // 生成补货建议
    return await request.post({ url: `/erp/vmi-replenishment/generate`, data })
  },
  VmiReplenishmentConvertToPurchaseOrder: async (data: any) => {
    // 将补货建议转换为采购订单
    return await request.post({ url: `/erp/vmi-replenishment/convert-to-purchase-order`, data })
  },
}
