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
    // 创建MrpPlan
    return await request.post({ url: `/erp/mrp-plan/create`, data })
  },
  updateMrpPlan: async (data: any) => {
    // 更新MrpPlan
    return await request.put({ url: `/erp/mrp-plan/update`, data })
  },
  deleteMrpPlan: async (id: number) => {
    // 删除MrpPlan
    return await request.delete({ url: `/erp/mrp-plan/delete?id=` + id })
  },
  getMrpPlan: async (id: number) => {
    // 获得MrpPlan
    return await request.get({ url: `/erp/mrp-plan/get?id=` + id })
  },
  getMrpPlanPage: async (params: any) => {
    // 获得MrpPlan分页
    return await request.get({ url: `/erp/mrp-plan/page`, params })
  },
  MrpPlanExecute: async (data: any) => {
    // 执行 MRP 计算
    return await request.put({ url: `/erp/mrp-plan/execute`, data })
  },
  MrpPlanConfirm: async (data: any) => {
    // 确认 MRP 计划
    return await request.put({ url: `/erp/mrp-plan/confirm`, data })
  },
  MrpPlanResultPage: async (data?: any) => {
    // 获得 MRP 结果分页
    return await request.get({ url: `/erp/mrp-plan/result/page`, params: data })
  },
  MrpPlanResultList: async (data?: any) => {
    // 获得 MRP 结果列表
    return await request.get({ url: `/erp/mrp-plan/result/list`, params: data })
  },
}
