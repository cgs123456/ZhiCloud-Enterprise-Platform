import request from '@/config/axios'

// QMS SPC 控制图（分析类端点，只读）
export const SpcApi = {
  // SPC 分析：按产品取样本，计算控制限与过程能力
  analyzeSpc: async (params: any) => {
    return await request.get({ url: '/qms/spc/analyze', params })
  },
  // 抽样计划：按批量查询 GB/T 2828 抽样方案
  getSamplingPlan: async (params: any) => {
    return await request.get({ url: '/qms/spc/sampling-plan', params })
  }
}
