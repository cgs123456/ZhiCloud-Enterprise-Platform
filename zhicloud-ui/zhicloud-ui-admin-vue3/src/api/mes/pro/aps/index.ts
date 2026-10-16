import request from '@/config/axios'

// Mes Aps API
export const ApsApi = {
  ApsGenerate: async (data: any) => {
    // 生成排产
    return await request.post({ url: `/mes/pro/aps/generate`, data })
  },
  ApsPlanList: async (data: any) => {
    // 排产计划列表
    return await request.get({ url: `/mes/pro/aps/plan/list`, data })
  },
  ApsPlanGet: async (id: any) => {
    // 获得排产计划
    return await request.get({ url: `/mes/pro/aps/plan/get?id=` + id })
  },
  ApsPlanReschedule: async (data: any) => {
    // 重排产
    return await request.put({ url: `/mes/pro/aps/plan/reschedule`, data })
  },
  ApsPlanConfirm: async (id: any) => {
    // 确认排产计划（草稿 → 已确认）
    return await request.put({ url: `/mes/pro/aps/plan/confirm?id=` + id })
  },
  ApsPlanCancel: async (id: any) => {
    // 取消排产计划（草稿/已确认 → 已取消）
    return await request.put({ url: `/mes/pro/aps/plan/cancel?id=` + id })
  },
  ApsWorkstationLoad: async (data: any) => {
    // 工位负荷
    return await request.get({ url: `/mes/pro/aps/workstation-load`, data })
  },
}
