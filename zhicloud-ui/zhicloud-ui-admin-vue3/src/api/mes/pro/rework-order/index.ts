import request from '@/config/axios'

// Mes ReworkOrder API
export const ReworkOrderApi = {
  createReworkOrder: async (data: any) => {
    // 基于原工单创建返工工单
    return await request.post({ url: `/mes/pro/rework-order/create`, data })
  },
  updateReworkOrder: async (data: any) => {
    // 更新返工工单
    return await request.put({ url: `/mes/pro/rework-order/update`, data })
  },
  deleteReworkOrder: async (id: number) => {
    // 删除返工工单
    return await request.delete({ url: `/mes/pro/rework-order/delete?id=` + id })
  },
  getReworkOrder: async (id: number) => {
    // 获得返工工单
    return await request.get({ url: `/mes/pro/rework-order/get?id=` + id })
  },
  getReworkOrderPage: async (params: any) => {
    // 获得返工工单分页
    return await request.get({ url: `/mes/pro/rework-order/page`, params })
  },
  exportReworkOrder: async (params: any) => {
    // 导出返工工单 Excel
    return await.download({ url: `/mes/pro/rework-order/export-excel`, params })
  },
  ReworkOrderStart: async (id: any) => {
    // 开工返工
    return await request.put({ url: `/mes/pro/rework-order/start?id=` + id })
  },
  ReworkOrderComplete: async (id: any) => {
    // 完工返工
    return await request.put({ url: `/mes/pro/rework-order/complete?id=` + id })
  },
  ReworkOrderCancel: async (id: any) => {
    // 取消返工
    return await request.put({ url: `/mes/pro/rework-order/cancel?id=` + id })
  },
}
