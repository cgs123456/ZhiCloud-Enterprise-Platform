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
    // 创建实际成本
    return await request.post({ url: `/erp/actual-cost/create`, data })
  },
  updateActualCost: async (data: any) => {
    // 更新实际成本
    return await request.put({ url: `/erp/actual-cost/update`, data })
  },
  deleteActualCost: async (id: number) => {
    // 删除实际成本
    return await request.delete({ url: `/erp/actual-cost/delete?id=` + id })
  },
  getActualCost: async (id: number) => {
    // 获得实际成本
    return await request.get({ url: `/erp/actual-cost/get?id=` + id })
  },
  getActualCostPage: async (params: any) => {
    // 获得实际成本分页
    return await request.get({ url: `/erp/actual-cost/page`, params })
  },
}
