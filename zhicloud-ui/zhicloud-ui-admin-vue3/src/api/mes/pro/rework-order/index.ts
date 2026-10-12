import request from '@/config/axios'

// Mes ReworkOrder API
export const ReworkOrderApi = {
  createReworkOrder: async (data: any) => {
    // 创建ReworkOrder
    return await request.post({ url: `/mes/pro/rework-order/create`, data })
  },
  updateReworkOrder: async (data: any) => {
    // 更新ReworkOrder
    return await request.put({ url: `/mes/pro/rework-order/update`, data })
  },
  deleteReworkOrder: async (id: number) => {
    // 删除ReworkOrder
    return await request.delete({ url: `/mes/pro/rework-order/delete?id=` + id })
  },
  getReworkOrder: async (id: number) => {
    // 获得ReworkOrder
    return await request.get({ url: `/mes/pro/rework-order/get?id=` + id })
  },
  getReworkOrderPage: async (params: any) => {
    // 获得ReworkOrder分页
    return await request.get({ url: `/mes/pro/rework-order/page`, params })
  },
  exportReworkOrder: async (params: any) => {
    // 导出ReworkOrder Excel
    return await request.download({ url: `/mes/pro/rework-order/export-excel`, params })
  },
  ReworkOrderStart: async (data: any) => {
    // 开工返工
    return await request.put({ url: `/mes/pro/rework-order/start`, data })
  },
  ReworkOrderComplete: async (data: any) => {
    // 完工返工
    return await request.put({ url: `/mes/pro/rework-order/complete`, data })
  },
  ReworkOrderCancel: async (data: any) => {
    // 取消返工
    return await request.put({ url: `/mes/pro/rework-order/cancel`, data })
  },
}
