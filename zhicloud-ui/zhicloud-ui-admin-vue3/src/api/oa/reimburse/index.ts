import request from '@/config/axios'

// OA 报销单 VO
export interface OaReimburseVO {
  id?: number
  no?: string
  reimburseName?: string
  applicantUserId?: number
  deptId?: number
  reimburseType?: number
  reimburseDate?: Date
  totalAmount?: number
  paymentStatus?: number
  processInstanceId?: string
  status?: number
  remark?: string
  createTime?: Date
}

// OA 报销单 API
export const OaReimburseApi = {
  // 查询分页
  getOaReimbursePage: async (params: any) => {
    return await request.get({ url: '/oa/reimburse/page', params })
  },
  // 查询详情
  getOaReimburse: async (id: number) => {
    return await request.get({ url: '/oa/reimburse/get?id=' + id })
  },
  // 新增
  createOaReimburse: async (data: OaReimburseVO) => {
    return await request.post({ url: '/oa/reimburse/create', data })
  },
  // 修改
  updateOaReimburse: async (data: OaReimburseVO) => {
    return await request.put({ url: '/oa/reimburse/update', data })
  },
  // 删除
  deleteOaReimburse: async (id: number) => {
    return await request.delete({ url: '/oa/reimburse/delete?id=' + id })
  },
}
