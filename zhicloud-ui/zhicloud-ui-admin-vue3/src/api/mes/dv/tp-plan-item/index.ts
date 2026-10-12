import request from '@/config/axios'

// Mes TpPlanItem API
export const TpPlanItemApi = {
  createTpPlanItem: async (data: any) => {
    // 创建TpPlanItem
    return await request.post({ url: `/mes/dv/tp-plan-item/create`, data })
  },
  updateTpPlanItem: async (data: any) => {
    // 更新TpPlanItem
    return await request.put({ url: `/mes/dv/tp-plan-item/update`, data })
  },
  deleteTpPlanItem: async (id: number) => {
    // 删除TpPlanItem
    return await request.delete({ url: `/mes/dv/tp-plan-item/delete?id=` + id })
  },
  getTpPlanItem: async (id: number) => {
    // 获得TpPlanItem
    return await request.get({ url: `/mes/dv/tp-plan-item/get?id=` + id })
  },
  getTpPlanItemList: async (params?: any) => {
    // 获得TpPlanItem列表
    return await request.get({ url: `/mes/dv/tp-plan-item/list`, params })
  },
}
