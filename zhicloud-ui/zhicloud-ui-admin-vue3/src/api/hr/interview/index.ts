import request from '@/config/axios'

// HR 面试 VO
export interface HrInterviewVO {
  id?: number
  resumeId?: number
  interviewRound?: number
  interviewerId?: number
  interviewTime?: Date
  interviewType?: number
  result?: number
  comment?: string
  status?: number
  createTime?: Date
}

// HR 面试 API
export const HrInterviewApi = {
  // 查询分页
  getHrInterviewPage: async (params: any) => {
    return await request.get({ url: '/hr/interview/page', params })
  },
  // 查询详情
  getHrInterview: async (id: number) => {
    return await request.get({ url: '/hr/interview/get?id=' + id })
  },
  // 新增
  createHrInterview: async (data: HrInterviewVO) => {
    return await request.post({ url: '/hr/interview/create', data })
  },
  // 修改
  updateHrInterview: async (data: HrInterviewVO) => {
    return await request.put({ url: '/hr/interview/update', data })
  },
  // 删除
  deleteHrInterview: async (id: number) => {
    return await request.delete({ url: '/hr/interview/delete?id=' + id })
  },
}
