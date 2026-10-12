import request from '@/config/axios'

// Mes ReworkOrderDetail API
export const ReworkOrderDetailApi = {
  createReworkOrderDetail: async (data: any) => {
    // 新增返工明细
    return await request.post({ url: `/mes/pro/rework-order-detail/create`, data })
  },
  updateReworkOrderDetail: async (data: any) => {
    // 更新返工明细
    return await request.put({ url: `/mes/pro/rework-order-detail/update`, data })
  },
  deleteReworkOrderDetail: async (id: number) => {
    // 删除返工明细
    return await request.delete({ url: `/mes/pro/rework-order-detail/delete?id=` + id })
  },
  getReworkOrderDetail: async (id: number) => {
    // 获得返工明细
    return await request.get({ url: `/mes/pro/rework-order-detail/get?id=` + id })
  },
  ReworkOrderDetailListByReworkOrder: async (data: any) => {
    // 根据返工工单编号获得明细列表
    return await request.get({ url: `/mes/pro/rework-order-detail/list-by-rework-order`, data })
  },
}
