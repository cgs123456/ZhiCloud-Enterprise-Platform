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

  // 提交整改措施（10 待整改 → 20 整改中）
  rectifyNonconformity: async (data: { id: number; action: string }) => {
    return await request.put({ url: '/qms/audit-nonconformity/rectify', data })
  },

  // 完成整改（20 整改中 → 30 已整改；completionNote 可空，§12.218 补齐断环）
  completeRectifyNonconformity: async (data: { id: number; completionNote?: string }) => {
    return await request.put({ url: '/qms/audit-nonconformity/complete-rectify', data })
  },

  // 验证整改效果（30 已整改 → 40 已验证；需电子签名）
  verifyNonconformity: async (
    data: { id: number; result: string },
    headers?: Record<string, string>
  ) => {
    return await request.put({ url: '/qms/audit-nonconformity/verify', data, headers })
  },

  // 关闭不符合项（40 已验证 → 50 已关闭；需电子签名）
  closeNonconformity: async (id: number, headers?: Record<string, string>) => {
    return await request.put({ url: `/qms/audit-nonconformity/close?id=${id}`, headers })
  },

  // 按审核报告查不符合项列表
  getNonconformityListByReport: async (reportId: number) => {
    return await request.get({ url: '/qms/audit-nonconformity/list-by-report?reportId=' + reportId })
  },
}
