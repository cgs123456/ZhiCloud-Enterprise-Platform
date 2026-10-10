import request from '@/config/axios'

// HR 绩效考核 VO
export interface HrPerformanceVO {
  id?: number
  rank?: number
  employeeId?: number
  period?: string
  score?: number
  grade?: number
  createTime?: Date
}

// HR 绩效考核 API
export const HrPerformanceApi = {
  // 查询分页
  getHrPerformancePage: async (params: any) => {
    return await request.get({ url: '/hr/performance/page', params })
  },
  // 查询详情
  getHrPerformance: async (id: number) => {
    return await request.get({ url: '/hr/performance/get?id=' + id })
  },
  // 新增
  createHrPerformance: async (data: HrPerformanceVO) => {
    return await request.post({ url: '/hr/performance/create', data })
  },
  // 修改
  updateHrPerformance: async (data: HrPerformanceVO) => {
    return await request.put({ url: '/hr/performance/update', data })
  },
  // 删除
  deleteHrPerformance: async (id: number) => {
    return await request.delete({ url: '/hr/performance/delete?id=' + id })
  },
}
