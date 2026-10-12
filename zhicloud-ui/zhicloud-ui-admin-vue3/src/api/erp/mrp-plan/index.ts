import request from '@/config/axios'

// Erp MrpPlan VO
export interface MrpPlanVO {
  id?: number // id
  no?: string // no
  planName?: string // planName
  planDate?: number // planDate
  mpsPlanId?: number // mpsPlanId
  status?: number // status
  totalDemandCount?: number // totalDemandCount
  totalPurchaseCount?: number // totalPurchaseCount
  totalProduceCount?: number // totalProduceCount
  remark?: string // remark
  creator?: string // creator
  createTime?: number // createTime
  results?: any[] // results
}

// Erp MrpPlan API
export const MrpPlanApi = {
  createMrpPlan: async (data: any) => {
    // 创建 MRP 计划
    return await request.post({ url: `/erp/mrp-plan/create`, data })
  },
  updateMrpPlan: async (data: any) => {
    // 更新 MRP 计划
    return await request.put({ url: `/erp/mrp-plan/update`, data })
  },
  deleteMrpPlan: async (ids: any) => {
    // 删除 MRP 计划
    return await request.delete({ url: `/erp/mrp-plan/delete?ids=` + ids })
  },
  getMrpPlan: async (id: number) => {
    // 获得 MRP 计划
    return await request.get({ url: `/erp/mrp-plan/get?id=` + id })
  },
  getMrpPlanPage: async (params: any) => {
    // 获得 MRP 计划分页
    return await request.get({ url: `/erp/mrp-plan/page`, params })
  },
  MrpPlanExecute: async (id: any) => {
    // 执行 MRP 计算
    return await request.put({ url: `/erp/mrp-plan/execute?id=` + id })
  },
  MrpPlanConfirm: async (id: any) => {
    // 确认 MRP 计划
    return await request.put({ url: `/erp/mrp-plan/confirm?id=` + id })
  },
  MrpPlanResultPage: async (data: any) => {
    // 获得 MRP 结果分页
    return await request.get({ url: `/erp/mrp-plan/result/page`, data })
  },
  MrpPlanResultList: async (data: any) => {
    // 获得 MRP 结果列表
    return await request.get({ url: `/erp/mrp-plan/result/list`, data })
  },
}
