import request from '@/config/axios'

// FMEA分析项 VO
export interface FmeaItemVO {
  id?: number
  fmeaId?: number
  function?: string
  failureMode?: string
  failureEffect?: string
  severity?: number
  potentialCause?: string
  occurrence?: number
  currentControls?: string
  detection?: number
  rpn?: number
  actionPriority?: string
  riskLevelName?: string
  riskLevelColor?: string
  actionRecommended?: string
  actionTaken?: string
  remark?: string
  createTime?: Date
}

// FMEA分析项 API
export const FmeaItemApi = {
  // 查询FMEA分析项分页
  getFmeaItemPage: async (params: any) => {
    return await request.get({ url: '/qms/fmea/item/page', params })
  },

  // 查询FMEA分析项详情
  getFmeaItem: async (id: number) => {
    return await request.get({ url: '/qms/fmea/item/get?id=' + id })
  },
  // 新增FMEA分析项
  createFmeaItem: async (data: FmeaItemVO) => {
    return await request.post({ url: '/qms/fmea/item/create', data })
  },

  // 修改FMEA分析项
  updateFmeaItem: async (data: FmeaItemVO) => {
    return await request.put({ url: '/qms/fmea/item/update', data })
  },

  // 删除FMEA分析项
  deleteFmeaItem: async (id: number) => {
    return await request.delete({ url: '/qms/fmea/item/delete?id=' + id })
  },
}
