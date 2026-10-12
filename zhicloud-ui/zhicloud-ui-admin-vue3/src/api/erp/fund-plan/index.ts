import request from '@/config/axios'

// Erp FundPlan VO
export interface FundPlanVO {
  id?: number // id
  planPeriod?: string // planPeriod
  planType?: number // planType
  amount?: number // amount
  bankAccountId?: number // bankAccountId
  remark?: string // remark
  createTime?: number // createTime
}

// Erp FundPlan API
export const FundPlanApi = {
  createFundPlan: async (data: any) => {
    // 创建资金计划
    return await request.post({ url: `/erp/fund-plan/create`, data })
  },
  updateFundPlan: async (data: any) => {
    // 更新资金计划
    return await request.put({ url: `/erp/fund-plan/update`, data })
  },
  deleteFundPlan: async (id: number) => {
    // 删除资金计划
    return await request.delete({ url: `/erp/fund-plan/delete?id=` + id })
  },
  getFundPlan: async (id: number) => {
    // 获得资金计划
    return await request.get({ url: `/erp/fund-plan/get?id=` + id })
  },
  getFundPlanPage: async (params: any) => {
    // 获得资金计划分页
    return await request.get({ url: `/erp/fund-plan/page`, params })
  },
  FundPlanSumByPeriod: async (planPeriod: any) => {
    // 按期间汇总资金计划净额（收款-付款）
    return await request.get({ url: `/erp/fund-plan/sum-by-period?planPeriod=` + planPeriod })
  },
}
