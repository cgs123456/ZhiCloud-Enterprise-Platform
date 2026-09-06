import request from '@/config/axios'

// 不合格品NCR VO
export interface NcrDocumentVO {
  id?: number
  ncrNo?: string
  source?: number
  inspectionOrderId?: number
  productId?: number
  supplierId?: number
  workOrderId?: number
  defectDescription?: string
  defectLevel?: number
  quantity?: number
  disposition?: number
  status?: number
  remark?: string
  createTime?: Date
}

// 不合格品NCR API
export const NcrDocumentApi = {
  // 查询不合格品NCR分页
  getNcrDocumentPage: async (params: any) => {
    return await request.get({ url: '/qms/ncr/page', params })
  },

  // 查询不合格品NCR详情
  getNcrDocument: async (id: number) => {
    return await request.get({ url: '/qms/ncr/get?id=' + id })
  },
  // 新增不合格品NCR
  createNcrDocument: async (data: NcrDocumentVO) => {
    return await request.post({ url: '/qms/ncr/create', data })
  },

  // 修改不合格品NCR
  updateNcrDocument: async (data: NcrDocumentVO) => {
    return await request.put({ url: '/qms/ncr/update', data })
  },

  // 删除不合格品NCR
  deleteNcrDocument: async (id: number) => {
    return await request.delete({ url: '/qms/ncr/delete?id=' + id })
  },
}
