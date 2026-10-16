import request from '@/config/axios'

// 受控文档 VO
export interface QmsDocumentVO {
  id?: number
  docNo?: string
  title?: string
  docType?: number
  version?: string
  status?: number
  effectiveDate?: Date
  expiryDate?: Date
  approverId?: number
  approveDate?: Date
  ownerDeptId?: number
  fileUrl?: string
  remark?: string
  sort?: number
  createTime?: Date
}

// 受控文档 API
export const QmsDocumentApi = {
  // 查询受控文档分页
  getQmsDocumentPage: async (params: any) => {
    return await request.get({ url: '/qms/document/page', params })
  },

  // 查询受控文档详情
  getQmsDocument: async (id: number) => {
    return await request.get({ url: '/qms/document/get?id=' + id })
  },
  // 新增受控文档
  createQmsDocument: async (data: QmsDocumentVO) => {
    return await request.post({ url: '/qms/document/create', data })
  },

  // 修改受控文档
  updateQmsDocument: async (data: QmsDocumentVO) => {
    return await request.put({ url: '/qms/document/update', data })
  },

  // 删除受控文档
  deleteQmsDocument: async (id: number) => {
    return await request.delete({ url: '/qms/document/delete?id=' + id })
  },

  // 提交审批（10 草稿 → 20 待审批；§12.239 接线审批闭环）
  submitQmsDocument: async (id: number) => {
    return await request.put({ url: '/qms/document/submit?id=' + id })
  },

  // 审批通过并发布（20 待审批 → 30 已发布；受控操作：需电子签名；fileUrl 可选定稿文件）
  approveQmsDocument: async (id: number, fileUrl?: string, headers?: Record<string, string>) => {
    const suffix = fileUrl ? '&fileUrl=' + encodeURIComponent(fileUrl) : ''
    return await request.put({ url: '/qms/document/approve?id=' + id + suffix, headers })
  },

  // 驳回（20 待审批 → 10 草稿；受控操作：需电子签名；reason 写入文档备注）
  rejectQmsDocument: async (data: { id: number; reason: string }, headers?: Record<string, string>) => {
    return await request.put({ url: '/qms/document/reject', data, headers })
  },

  // 作废（30 已发布 → 40 已作废）
  revokeQmsDocument: async (id: number) => {
    return await request.put({ url: '/qms/document/revoke?id=' + id })
  }
}
