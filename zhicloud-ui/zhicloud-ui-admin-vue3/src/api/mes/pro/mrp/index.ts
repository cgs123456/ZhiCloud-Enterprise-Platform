import request from '@/config/axios'

// Mes Mrp API
export const MrpApi = {
  MrpPlanCreate: async (data: any) => {
    // 创建 MRP 计划
    return await request.post({ url: `/mes/pro/mrp/plan/create`, data })
  },
  MrpCalculate: async (planId: number) => {
    // 触发 MRP 计算（planId 为 query 参数）
    return await request.post({ url: `/mes/pro/mrp/calculate?planId=` + planId })
  },
  getMrpResult: async (planId: number) => {
    // 获取 MRP 计算结果
    return await request.get({ url: `/mes/pro/mrp/result/` + planId })
  },
  MrpPlanList: async (data: any) => {
    // MRP 计划列表
    return await request.get({ url: `/mes/pro/mrp/plan/list`, data })
  },
  MrpPlanGet: async (id: any) => {
    // 获得 MRP 计划
    return await request.get({ url: `/mes/pro/mrp/plan/get?id=` + id })
  },
  MrpPlanConfirm: async (id: any) => {
    // 确认 MRP 计划（1 已计算 → 2 已确认）
    return await request.put({ url: `/mes/pro/mrp/plan/confirm?id=` + id })
  },
  MrpPlanConvert: async (id: any) => {
    // 转采购/生产建议（2 已确认 → 3 已转建议；逐行落地为草稿工单）
    return await request.post({ url: `/mes/pro/mrp/plan/convert?id=` + id })
  },
}
