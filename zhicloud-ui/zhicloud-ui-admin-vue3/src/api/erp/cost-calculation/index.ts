import request from '@/config/axios'

// Erp CostCalculation API
export const CostCalculationApi = {
  CostCalculationCalculateStandardCost: async (data: any) => {
    // 执行标准成本卷积计算
    return await request.post({ url: `/erp/cost-calculation/calculate-standard-cost`, data })
  },
  CostCalculationCollectActualCost: async (data: any) => {
    // 归集工单成本到实际成本
    return await request.post({ url: `/erp/cost-calculation/collect-actual-cost`, data })
  },
  CostCalculationAnalyzeVariance: async (data: any) => {
    // 执行差异分析
    return await request.post({ url: `/erp/cost-calculation/analyze-variance`, data })
  },
}
