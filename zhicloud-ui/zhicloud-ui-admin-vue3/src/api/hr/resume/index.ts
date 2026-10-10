import request from '@/config/axios'

// HR 简历 VO
export interface HrResumeVO {
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

// HR 简历 API
export const HrResumeApi = {
  // 查询分页
  getHrResumePage: async (params: any) => {
    return await request.get({ url: '/hr/resume/page', params })
  },
  // 查询详情
  getHrResume: async (id: number) => {
    return await request.get({ url: '/hr/resume/get?id=' + id })
  },
  // 新增
  createHrResume: async (data: HrResumeVO) => {
    return await request.post({ url: '/hr/resume/create', data })
  },
  // 修改
  updateHrResume: async (data: HrResumeVO) => {
    return await request.put({ url: '/hr/resume/update', data })
  },
  // 删除
  deleteHrResume: async (id: number) => {
    return await request.delete({ url: '/hr/resume/delete?id=' + id })
  },
}
