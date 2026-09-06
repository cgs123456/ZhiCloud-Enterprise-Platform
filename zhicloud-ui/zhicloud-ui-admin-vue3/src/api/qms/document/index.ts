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
}
