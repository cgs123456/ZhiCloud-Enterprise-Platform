import request from '@/config/axios'

// FMEA文档 VO
export interface FmeaDocumentVO {
  id?: number
  fmeaNo?: string
  fmeaType?: number
  productId?: number
  processId?: number
  version?: string
  status?: number
  remark?: string
  createTime?: Date
}

// FMEA文档 API
export const FmeaDocumentApi = {
  // 查询FMEA文档分页
  getFmeaDocumentPage: async (params: any) => {
    return await request.get({ url: '/qms/fmea/page', params })
  },

  // 查询FMEA文档详情
  getFmeaDocument: async (id: number) => {
    return await request.get({ url: '/qms/fmea/get?id=' + id })
  },
  // 新增FMEA文档
  createFmeaDocument: async (data: FmeaDocumentVO) => {
    return await request.post({ url: '/qms/fmea/create', data })
  },

  // 修改FMEA文档
  updateFmeaDocument: async (data: FmeaDocumentVO) => {
    return await request.put({ url: '/qms/fmea/update', data })
  },

  // 删除FMEA文档
  deleteFmeaDocument: async (id: number) => {
    return await request.delete({ url: '/qms/fmea/delete?id=' + id })
  },
}
