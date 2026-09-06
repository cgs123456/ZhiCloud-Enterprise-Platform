import request from '@/config/axios'

// 质量成本 VO
export interface QmsQualityCostVO {
  id?: number
  costType?: string
  costCategory?: string
  costItem?: string
  amount?: number
  periodYear?: number
  periodMonth?: number
  relatedId?: number
  relatedType?: string
  remark?: string
  createTime?: Date
}

// 质量成本 API
export const QmsQualityCostApi = {
  // 查询质量成本分页
  getQmsQualityCostPage: async (params: any) => {
    return await request.get({ url: '/qms/quality-cost/page', params })
  },

  // 查询质量成本详情
  getQmsQualityCost: async (id: number) => {
    return await request.get({ url: '/qms/quality-cost/get?id=' + id })
  },
  // 新增质量成本
  createQmsQualityCost: async (data: QmsQualityCostVO) => {
    return await request.post({ url: '/qms/quality-cost/create', data })
  },

  // 修改质量成本
  updateQmsQualityCost: async (data: QmsQualityCostVO) => {
    return await request.put({ url: '/qms/quality-cost/update', data })
  },

  // 删除质量成本
  deleteQmsQualityCost: async (id: number) => {
    return await request.delete({ url: '/qms/quality-cost/delete?id=' + id })
  },
}
