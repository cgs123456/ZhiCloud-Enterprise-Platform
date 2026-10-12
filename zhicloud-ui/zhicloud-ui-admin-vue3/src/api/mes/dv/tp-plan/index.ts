import request from '@/config/axios'

// Mes TpPlan API
export const TpPlanApi = {
  createTpPlan: async (data: any) => {
    // 创建TpPlan
    return await request.post({ url: `/mes/dv/tp-plan/create`, data })
  },
  updateTpPlan: async (data: any) => {
    // 更新TpPlan
    return await request.put({ url: `/mes/dv/tp-plan/update`, data })
  },
  deleteTpPlan: async (id: number) => {
    // 删除TpPlan
    return await request.delete({ url: `/mes/dv/tp-plan/delete?id=` + id })
  },
  getTpPlan: async (id: number) => {
    // 获得TpPlan
    return await request.get({ url: `/mes/dv/tp-plan/get?id=` + id })
  },
  getTpPlanPage: async (params: any) => {
    // 获得TpPlan分页
    return await request.get({ url: `/mes/dv/tp-plan/page`, params })
  },
  TpPlanEnable: async (data: any) => {
    // 启用 TPM 计划
    return await request.put({ url: `/mes/dv/tp-plan/enable`, data })
  },
  TpPlanDisable: async (data: any) => {
    // 禁用 TPM 计划
    return await request.put({ url: `/mes/dv/tp-plan/disable`, data })
  },
  TpPlanExecute: async (data: any) => {
    // 执行 TPM 计划
    return await request.post({ url: `/mes/dv/tp-plan/execute`, data })
  },
  TpPlanOverdue: async (data?: any) => {
    // 查询逾期未执行的计划
    return await request.get({ url: `/mes/dv/tp-plan/overdue`, params: data })
  },
  TpPlanRecordList: async (data?: any) => {
    // 获得 TPM 执行记录列表
    return await request.get({ url: `/mes/dv/tp-plan/record/list`, params: data })
  },
}
