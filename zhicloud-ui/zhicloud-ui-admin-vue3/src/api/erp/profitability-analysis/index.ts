import request from '@/config/axios'

// Erp ProfitabilityAnalysis VO
export interface ProfitabilityAnalysisVO {
  id?: number // id
  profitCenterId?: number // profitCenterId
  periodId?: number // periodId
  revenue?: number // revenue
  cost?: number // cost
  profit?: number // profit
  profitMargin?: number // profitMargin
  remark?: string // remark
  createTime?: number // createTime
}

// Erp ProfitabilityAnalysis API
export const ProfitabilityAnalysisApi = {
  createProfitabilityAnalysis: async (data: any) => {
    // 创建获利分析记录
    return await request.post({ url: `/erp/profitability-analysis/create`, data })
  },
  updateProfitabilityAnalysis: async (data: any) => {
    // 更新获利分析记录
    return await request.put({ url: `/erp/profitability-analysis/update`, data })
  },
  deleteProfitabilityAnalysis: async (id: number) => {
    // 删除获利分析记录
    return await request.delete({ url: `/erp/profitability-analysis/delete?id=` + id })
  },
  getProfitabilityAnalysis: async (id: number) => {
    // 获得获利分析记录
    return await request.get({ url: `/erp/profitability-analysis/get?id=` + id })
  },
  getProfitabilityAnalysisPage: async (params: any) => {
    // 获得获利分析分页
    return await request.get({ url: `/erp/profitability-analysis/page`, params })
  },
  ProfitabilityAnalysisCalculate: async (data: any) => {
    // 计算获利能力
    return await request.post({ url: `/erp/profitability-analysis/calculate`, data })
  },
}
