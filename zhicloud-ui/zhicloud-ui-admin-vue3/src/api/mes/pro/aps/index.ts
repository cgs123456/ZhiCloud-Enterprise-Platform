import request from '@/config/axios'

// Mes Aps API
export const ApsApi = {
  ApsGenerate: async (data: any) => {
    // 生成排产
    return await request.post({ url: `/mes/pro/aps/generate`, data })
  },
  ApsPlanList: async (data?: any) => {
    // 排产计划列表
    return await request.get({ url: `/mes/pro/aps/plan/list`, params: data })
  },
  ApsPlanGet: async (data?: any) => {
    // 获得排产计划
    return await request.get({ url: `/mes/pro/aps/plan/get`, params: data })
  },
  ApsPlanReschedule: async (data: any) => {
    // 重排产
    return await request.put({ url: `/mes/pro/aps/plan/reschedule`, data })
  },
  ApsWorkstationLoad: async (data?: any) => {
    // 工位负荷
    return await request.get({ url: `/mes/pro/aps/workstation-load`, params: data })
  },
}
