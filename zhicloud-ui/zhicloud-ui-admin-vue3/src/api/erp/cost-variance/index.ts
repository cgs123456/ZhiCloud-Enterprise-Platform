import request from '@/config/axios'

// Erp CostVariance VO
export interface CostVarianceVO {
  id?: number // id
  productId?: number // productId
  costPeriod?: string // costPeriod
  costItemId?: number // costItemId
  standardCost?: number // standardCost
  actualCost?: number // actualCost
  varianceAmount?: number // varianceAmount
  varianceRate?: number // varianceRate
  varianceType?: number // varianceType
  analysisRemark?: string // analysisRemark
  remark?: string // remark
  createTime?: number // createTime
}

// Erp CostVariance API
export const CostVarianceApi = {
  createCostVariance: async (data: any) => {
    // 创建CostVariance
    return await request.post({ url: `/erp/cost-variance/create`, data })
  },
  updateCostVariance: async (data: any) => {
    // 更新CostVariance
    return await request.put({ url: `/erp/cost-variance/update`, data })
  },
  deleteCostVariance: async (id: number) => {
    // 删除CostVariance
    return await request.delete({ url: `/erp/cost-variance/delete?id=` + id })
  },
  getCostVariance: async (id: number) => {
    // 获得CostVariance
    return await request.get({ url: `/erp/cost-variance/get?id=` + id })
  },
  getCostVariancePage: async (params: any) => {
    // 获得CostVariance分页
    return await request.get({ url: `/erp/cost-variance/page`, params })
  },
}
