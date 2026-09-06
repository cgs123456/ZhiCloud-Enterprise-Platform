import request from '@/config/axios'

// 客户投诉 VO
export interface CustomerComplaintVO {
  id?: number
  complaintNo?: string
  customerId?: number
  customerName?: string
  productId?: number
  productName?: string
  complaintContent?: string
  complaintDate?: Date
  rootCause?: string
  impactScope?: string
  handleType?: number
  handleAction?: string
  eightDId?: number
  status?: number
  closeTime?: Date
  remark?: string
  createTime?: Date
}

// 客户投诉 API
export const CustomerComplaintApi = {
  // 查询客户投诉分页
  getCustomerComplaintPage: async (params: any) => {
    return await request.get({ url: '/qms/customer-complaint/page', params })
  },

  // 查询客户投诉详情
  getCustomerComplaint: async (id: number) => {
    return await request.get({ url: '/qms/customer-complaint/get?id=' + id })
  },
  // 新增客户投诉
  createCustomerComplaint: async (data: CustomerComplaintVO) => {
    return await request.post({ url: '/qms/customer-complaint/create', data })
  },

  // 修改客户投诉
  updateCustomerComplaint: async (data: CustomerComplaintVO) => {
    return await request.put({ url: '/qms/customer-complaint/update', data })
  },

  // 删除客户投诉
  deleteCustomerComplaint: async (id: number) => {
    return await request.delete({ url: '/qms/customer-complaint/delete?id=' + id })
  },
}
