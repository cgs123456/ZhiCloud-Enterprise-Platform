import request from '@/config/axios'

// 审核不符合项 VO
export interface QmsAuditNonconformityVO {
  id?: number
  reportId?: number
  ncNo?: string
  severity?: number
  description?: string
  clause?: string
  responsibleDeptId?: number
  correctiveActionDeadline?: Date
  status?: number
  remark?: string
  sort?: number
  createTime?: Date
}

// 审核不符合项 API
export const QmsAuditNonconformityApi = {
  // 查询审核不符合项分页
  getQmsAuditNonconformityPage: async (params: any) => {
    return await request.get({ url: '/qms/audit-nonconformity/page', params })
  },

  // 查询审核不符合项详情
  getQmsAuditNonconformity: async (id: number) => {
    return await request.get({ url: '/qms/audit-nonconformity/get?id=' + id })
  },
  // 新增审核不符合项
  createQmsAuditNonconformity: async (data: QmsAuditNonconformityVO) => {
    return await request.post({ url: '/qms/audit-nonconformity/create', data })
  },

  // 修改审核不符合项
  updateQmsAuditNonconformity: async (data: QmsAuditNonconformityVO) => {
    return await request.put({ url: '/qms/audit-nonconformity/update', data })
  },

  // 删除审核不符合项
  deleteQmsAuditNonconformity: async (id: number) => {
    return await request.delete({ url: '/qms/audit-nonconformity/delete?id=' + id })
  },
}
