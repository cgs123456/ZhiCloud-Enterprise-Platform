import request from '@/config/axios'

// OA 公文 VO
export interface OaDocumentVO {
  id?: number
  no?: string
  title?: string
  documentType?: number
  urgency?: number
  confidentiality?: number
  issuerUserId?: number
  issueDeptId?: number
  issueDate?: Date
  content?: string
  processInstanceId?: string
  status?: number
  remark?: string
  reviewerUserId?: number
  reviewerName?: string
  reviewTime?: Date
  reviewOpinion?: string
  signerUserId?: number
  signerName?: string
  signTime?: Date
  signOpinion?: string
  archiverUserId?: number
  archiverName?: string
  archiveTime?: Date
  archiveNo?: string
  mainSendDepts?: string
  copySendDepts?: string
  readCount?: number
  createTime?: Date
}

// OA 公文 API
export const OaDocumentApi = {
  // 查询分页
  getOaDocumentPage: async (params: any) => {
    return await request.get({ url: '/oa/document/page', params })
  },
  // 查询详情
  getOaDocument: async (id: number) => {
    return await request.get({ url: '/oa/document/get?id=' + id })
  },
  // 新增
  createOaDocument: async (data: OaDocumentVO) => {
    return await request.post({ url: '/oa/document/create', data })
  },
  // 修改
  updateOaDocument: async (data: OaDocumentVO) => {
    return await request.put({ url: '/oa/document/update', data })
  },
  // 删除
  deleteOaDocument: async (id: number) => {
    return await request.delete({ url: '/oa/document/delete?id=' + id })
  },
}
