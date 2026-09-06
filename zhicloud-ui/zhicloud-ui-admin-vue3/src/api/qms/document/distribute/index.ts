import request from '@/config/axios'

// 文档分发 VO
export interface QmsDocumentDistributeVO {
  id?: number
  documentId?: number
  distributeTo?: string
  distributeQty?: number
  distributeDate?: Date
  receivedBy?: string
  receivedDate?: Date
  returnedQty?: number
  returnedDate?: Date
  remark?: string
  sort?: number
  createTime?: Date
}

// 文档分发 API
export const QmsDocumentDistributeApi = {
  // 查询文档分发分页
  getQmsDocumentDistributePage: async (params: any) => {
    return await request.get({ url: '/qms/document-distribute/page', params })
  },

  // 查询文档分发详情
  getQmsDocumentDistribute: async (id: number) => {
    return await request.get({ url: '/qms/document-distribute/get?id=' + id })
  },
  // 新增文档分发
  createQmsDocumentDistribute: async (data: QmsDocumentDistributeVO) => {
    return await request.post({ url: '/qms/document-distribute/create', data })
  },

  // 修改文档分发
  updateQmsDocumentDistribute: async (data: QmsDocumentDistributeVO) => {
    return await request.put({ url: '/qms/document-distribute/update', data })
  },

  // 删除文档分发
  deleteQmsDocumentDistribute: async (id: number) => {
    return await request.delete({ url: '/qms/document-distribute/delete?id=' + id })
  },
}
