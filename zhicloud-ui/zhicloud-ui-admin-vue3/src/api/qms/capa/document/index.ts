import request from '@/config/axios'

// 纠正预防措施CAPA VO
export interface CAPADocumentVO {
  id?: number
  capaNo?: string
  source?: number
  priority?: number
  stage?: number
  problem?: string
  cause?: string
  rootCauseAnalysis?: string
  correctiveAction?: string
  preventiveAction?: string
  responsiblePerson?: string
  dueDate?: Date
  closeDate?: Date
  status?: number
  verificationResult?: number
  verificationComment?: string
  verifiedBy?: string
  verifiedTime?: Date
  remark?: string
  createTime?: Date
}

// 纠正预防措施CAPA API
export const CAPADocumentApi = {
  // 查询纠正预防措施CAPA分页
  getCAPADocumentPage: async (params: any) => {
    return await request.get({ url: '/qms/capa/page', params })
  },

  // 查询纠正预防措施CAPA详情
  getCAPADocument: async (id: number) => {
    return await request.get({ url: '/qms/capa/get?id=' + id })
  },
  // 新增纠正预防措施CAPA
  createCAPADocument: async (data: CAPADocumentVO) => {
    return await request.post({ url: '/qms/capa/create', data })
  },

  // 修改纠正预防措施CAPA
  updateCAPADocument: async (data: CAPADocumentVO) => {
    return await request.put({ url: '/qms/capa/update', data })
  },

  // 删除纠正预防措施CAPA
  deleteCAPADocument: async (id: number) => {
    return await request.delete({ url: '/qms/capa/delete?id=' + id })
  },
}
