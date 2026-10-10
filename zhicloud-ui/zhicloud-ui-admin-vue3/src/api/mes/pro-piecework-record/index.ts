import request from '@/config/axios'

// MES 计件记录（只读）
export const MesProPieceworkRecordApi = {
  // 分页查询
  getRecordPage: async (params: any) => {
    return await request.get({ url: '/mes/pro-piecework-record/page', params })
  },
  // 详情
  getRecord: async (id: number) => {
    return await request.get({ url: '/mes/pro-piecework-record/get?id=' + id })
  },
  // 按期间汇总
  summaryByPeriod: async (params: any) => {
    return await request.get({ url: '/mes/pro-piecework-record/summary-by-period', params })
  },
  // 按人员汇总
  summaryByUser: async (params: any) => {
    return await request.get({ url: '/mes/pro-piecework-record/summary-by-user', params })
  }
}
