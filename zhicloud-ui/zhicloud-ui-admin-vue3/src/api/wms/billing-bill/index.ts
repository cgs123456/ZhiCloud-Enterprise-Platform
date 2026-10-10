import request from '@/config/axios'

// WMS 计费账单 VO
export interface WmsBillingBillVO {
  id?: number
  billNo?: string
  ownerId?: number
  ownerName?: string
  billingPeriodStart?: Date
  billingPeriodEnd?: Date
  totalAmount?: number
  status?: number
  remark?: string
  creatorName?: string
  createTime?: Date
}

// WMS 计费账单 API
export const WmsBillingBillApi = {
  // 查询分页
  getWmsBillingBillPage: async (params: any) => {
    return await request.get({ url: '/wms/billing-bill/page', params })
  },
  // 查询详情
  getWmsBillingBill: async (id: number) => {
    return await request.get({ url: '/wms/billing-bill/get?id=' + id })
  },
  // 新增
  createWmsBillingBill: async (data: WmsBillingBillVO) => {
    return await request.post({ url: '/wms/billing-bill/create', data })
  },
  // 修改
  updateWmsBillingBill: async (data: WmsBillingBillVO) => {
    return await request.put({ url: '/wms/billing-bill/update', data })
  },
  // 删除
  deleteWmsBillingBill: async (id: number) => {
    return await request.delete({ url: '/wms/billing-bill/delete?id=' + id })
  },
}
