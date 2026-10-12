import request from '@/config/axios'

// Mes WorkOrderKitting API
export const WorkOrderKittingApi = {
  WorkOrderKittingAnalyze: async (data?: any) => {
    // 工单齐套分析
    return await request.get({ url: `/mes/pro/work-order-kitting/analyze`, params: data })
  },
}
