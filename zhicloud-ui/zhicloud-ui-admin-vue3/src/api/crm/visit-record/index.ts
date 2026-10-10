import request from '@/config/axios'

// CRM 拜访签到 VO
export interface CrmVisitRecordVO {
  id?: number
  customerId?: number
  customerName?: string
  contactId?: number
  contactName?: string
  ownerUserId?: number
  ownerUserName?: string
  ownerUserDeptName?: string
  visitTime?: Date
  signInTime?: Date
  signInLatitude?: number
  signInLongitude?: number
  signInAddress?: string
  visitType?: number
  content?: string
  creatorName?: string
  createTime?: Date
}

// CRM 拜访签到 API
export const CrmVisitRecordApi = {
  // 查询分页
  getCrmVisitRecordPage: async (params: any) => {
    return await request.get({ url: '/crm/visit-record/page', params })
  },
  // 查询详情
  getCrmVisitRecord: async (id: number) => {
    return await request.get({ url: '/crm/visit-record/get?id=' + id })
  },
  // 新增
  createCrmVisitRecord: async (data: CrmVisitRecordVO) => {
    return await request.post({ url: '/crm/visit-record/create', data })
  },
  // 修改
  updateCrmVisitRecord: async (data: CrmVisitRecordVO) => {
    return await request.put({ url: '/crm/visit-record/update', data })
  },
  // 删除
  deleteCrmVisitRecord: async (id: number) => {
    return await request.delete({ url: '/crm/visit-record/delete?id=' + id })
  },
}
