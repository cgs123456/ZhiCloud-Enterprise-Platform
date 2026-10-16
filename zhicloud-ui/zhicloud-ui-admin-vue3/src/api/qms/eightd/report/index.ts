import request from '@/config/axios'

// 8D报告 VO
export interface EightDReportVO {
  id?: number
  reportNo?: string
  title?: string
  ncrId?: number
  capaId?: number
  status?: number
  d1TeamMembers?: string
  d2ProblemDescription?: string
  d3InterimAction?: string
  d4RootCause?: string
  d5PermanentAction?: string
  d6ImplementationResult?: string
  d7PreventionAction?: string
  d8TeamRecognition?: string
  closeTime?: Date
  remark?: string
  createTime?: Date
}

// 8D报告 API
export const EightDReportApi = {
  // 查询8D报告分页
  getEightDReportPage: async (params: any) => {
    return await request.get({ url: '/qms/eight-d/page', params })
  },

  // 查询8D报告详情
  getEightDReport: async (id: number) => {
    return await request.get({ url: '/qms/eight-d/get?id=' + id })
  },
  // 新增8D报告
  createEightDReport: async (data: EightDReportVO) => {
    return await request.post({ url: '/qms/eight-d/create', data })
  },

  // 修改8D报告
  updateEightDReport: async (data: EightDReportVO) => {
    return await request.put({ url: '/qms/eight-d/update', data })
  },

  // 删除8D报告
  deleteEightDReport: async (id: number) => {
    return await request.delete({ url: '/qms/eight-d/delete?id=' + id })
  },

  // 推进阶段（D(n) → D(n+1)；D7 封顶不再推进；§12.239 接线）
  advanceEightDReportStage: async (id: number) => {
    return await request.put({ url: '/qms/eight-d/advance-stage?id=' + id })
  },

  // 关闭（→ D8 已关闭；受控操作：需电子签名）
  closeEightDReport: async (id: number, headers?: Record<string, string>) => {
    return await request.put({ url: '/qms/eight-d/close?id=' + id, headers })
  }
}
