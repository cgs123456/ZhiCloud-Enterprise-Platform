import request from '@/config/axios'

// Erp ActualCost VO
export interface ActualCostVO {
  id?: number // id
  productId?: number // productId
  productCode?: string // productCode
  costPeriod?: string // costPeriod
  costItemId?: number // costItemId
  actualCost?: number // actualCost
  actualQuantity?: number // actualQuantity
  unitCost?: number // unitCost
  varianceAmount?: number // varianceAmount
  varianceRate?: number // varianceRate
  remark?: string // remark
  createTime?: number // createTime
}

// Erp ActualCost API
export const ActualCostApi = {
  createActualCost: async (data: any) => {
    // 创建ActualCost
    return await request.post({ url: `/erp/actual-cost/create`, data })
  },
  updateActualCost: async (data: any) => {
    // 更新ActualCost
    return await request.put({ url: `/erp/actual-cost/update`, data })
  },
  deleteActualCost: async (id: number) => {
    // 删除ActualCost
    return await request.delete({ url: `/erp/actual-cost/delete?id=` + id })
  },
  getActualCost: async (id: number) => {
    // 获得ActualCost
    return await request.get({ url: `/erp/actual-cost/get?id=` + id })
  },
  getActualCostPage: async (params: any) => {
    // 获得ActualCost分页
    return await request.get({ url: `/erp/actual-cost/page`, params })
  },
}
