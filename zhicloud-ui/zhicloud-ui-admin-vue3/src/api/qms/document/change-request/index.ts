import request from '@/config/axios'

// 变更申请 VO
export interface QmsDocumentChangeRequestVO {
  id?: number
  documentId?: number
  changeType?: number
  changeReason?: string
  changeContent?: string
  applicantId?: number
  applyDate?: Date
  approverId?: number
  approveDate?: Date
  status?: number
  remark?: string
  sort?: number
  createTime?: Date
}

// 变更申请 API
export const QmsDocumentChangeRequestApi = {
  // 查询变更申请分页
  getQmsDocumentChangeRequestPage: async (params: any) => {
    return await request.get({ url: '/qms/document-change-request/page', params })
  },

  // 查询变更申请详情
  getQmsDocumentChangeRequest: async (id: number) => {
    return await request.get({ url: '/qms/document-change-request/get?id=' + id })
  },
  // 新增变更申请
  createQmsDocumentChangeRequest: async (data: QmsDocumentChangeRequestVO) => {
    return await request.post({ url: '/qms/document-change-request/create', data })
  },

  // 修改变更申请
  updateQmsDocumentChangeRequest: async (data: QmsDocumentChangeRequestVO) => {
    return await request.put({ url: '/qms/document-change-request/update', data })
  },

  // 删除变更申请
  deleteQmsDocumentChangeRequest: async (id: number) => {
    return await request.delete({ url: '/qms/document-change-request/delete?id=' + id })
  },

  // 批准变更申请（10 待审 → 20 已批准；受控操作：需电子签名；批准后自动执行变更）
  approveQmsDocumentChangeRequest: async (id: number, headers?: Record<string, string>) => {
    return await request.put({ url: '/qms/document-change-request/approve?id=' + id, headers })
  },

  // 驳回变更申请（10 待审 → 30 已驳回；reason 写入备注）
  rejectQmsDocumentChangeRequest: async (data: { id: number; reason: string }) => {
    return await request.put({ url: '/qms/document-change-request/reject', data })
  }
}
