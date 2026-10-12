import request from '@/config/axios'

// Mes TpPlan API
export const TpPlanApi = {
  createTpPlan: async (data: any) => {
    // 创建 TPM 计划
    return await request.post({ url: `/mes/dv/tp-plan/create`, data })
  },
  updateTpPlan: async (data: any) => {
    // 更新 TPM 计划
    return await request.put({ url: `/mes/dv/tp-plan/update`, data })
  },
  deleteTpPlan: async (ids: any) => {
    // 删除 TPM 计划
    return await request.delete({ url: `/mes/dv/tp-plan/delete?ids=` + ids })
  },
  getTpPlan: async (id: number) => {
    // 获得 TPM 计划
    return await request.get({ url: `/mes/dv/tp-plan/get?id=` + id })
  },
  getTpPlanPage: async (params: any) => {
    // 获得 TPM 计划分页
    return await request.get({ url: `/mes/dv/tp-plan/page`, params })
  },
  TpPlanEnable: async (id: any) => {
    // 启用 TPM 计划
    return await request.put({ url: `/mes/dv/tp-plan/enable?id=` + id })
  },
  TpPlanDisable: async (id: any) => {
    // 禁用 TPM 计划
    return await request.put({ url: `/mes/dv/tp-plan/disable?id=` + id })
  },
  TpPlanExecute: async (data: any) => {
    // 执行 TPM 计划
    return await request.post({ url: `/mes/dv/tp-plan/execute`, data })
  },
  TpPlanOverdue: async (data: any) => {
    // 查询逾期未执行的计划
    return await request.get({ url: `/mes/dv/tp-plan/overdue`, data })
  },
  TpPlanRecordList: async (data: any) => {
    // 获得 TPM 执行记录列表
    return await request.get({ url: `/mes/dv/tp-plan/record/list`, data })
  },
}
