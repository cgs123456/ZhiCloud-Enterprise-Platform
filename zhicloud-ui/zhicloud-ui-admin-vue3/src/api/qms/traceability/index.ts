import request from '@/config/axios'

// QMS 质量追溯（正向 / 反向，只读）
export const QualityTraceabilityApi = {
  // 正向追溯：由物料查流向
  forward: async (params: any) => {
    return await request.get({ url: '/qms/traceability/forward', params })
  },
  // 反向追溯：由产品查来源
  backward: async (params: any) => {
    return await request.get({ url: '/qms/traceability/backward', params })
  }
}
