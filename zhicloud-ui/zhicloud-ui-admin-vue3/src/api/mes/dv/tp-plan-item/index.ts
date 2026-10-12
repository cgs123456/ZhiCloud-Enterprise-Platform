import request from '@/config/axios'

// Mes TpPlanItem API
export const TpPlanItemApi = {
  createTpPlanItem: async (data: any) => {
    // 添加 TPM 计划项目
    return await request.post({ url: `/mes/dv/tp-plan-item/create`, data })
  },
  updateTpPlanItem: async (data: any) => {
    // 更新 TPM 计划项目
    return await request.put({ url: `/mes/dv/tp-plan-item/update`, data })
  },
  deleteTpPlanItem: async (id: number) => {
    // 删除 TPM 计划项目
    return await request.delete({ url: `/mes/dv/tp-plan-item/delete?id=` + id })
  },
  getTpPlanItem: async (id: number) => {
    // 获得 TPM 计划项目
    return await request.get({ url: `/mes/dv/tp-plan-item/get?id=` + id })
  },
  getTpPlanItemList: async (params?: any) => {
    // 获得 TPM 计划项目列表
    return await request.get({ url: `/mes/dv/tp-plan-item/list`, params })
  },
}
