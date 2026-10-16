import request from '@/config/axios'

// 检验单 VO
export interface InspectionOrderVO {
  id?: number
  orderNo?: string
  type?: number
  supplierId?: number
  batchNo?: string
  workOrderId?: number
  productId?: number
  inspector?: string
  inspectTime?: Date
  status?: number
  remark?: string
  createTime?: Date
}

// 检验单 API
export const InspectionOrderApi = {
  // 查询检验单分页
  getInspectionOrderPage: async (params: any) => {
    return await request.get({ url: '/qms/inspection-order/page', params })
  },

  // 查询检验单详情
  getInspectionOrder: async (id: number) => {
    return await request.get({ url: '/qms/inspection-order/get?id=' + id })
  },
  // 新增检验单
  createInspectionOrder: async (data: InspectionOrderVO) => {
    return await request.post({ url: '/qms/inspection-order/create', data })
  },

  // 修改检验单
  updateInspectionOrder: async (data: InspectionOrderVO) => {
    return await request.put({ url: '/qms/inspection-order/update', data })
  },

  // 删除检验单
  deleteInspectionOrder: async (id: number) => {
    return await request.delete({ url: '/qms/inspection-order/delete?id=' + id })
  },

  // 提交检验（10/20 → 按 AQL 判定 30 合格 / 40 不合格；records 为检验记录数组；§12.239 接线）
  submitInspectionOrder: async (orderId: number, records: any[]) => {
    return await request.post({
      url: '/qms/inspection-order/submit?orderId=' + orderId,
      data: records
    })
  }
}
