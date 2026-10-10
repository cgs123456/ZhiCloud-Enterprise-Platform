import request from '@/config/axios'

// HR 招聘职位 VO
export interface HrJobPostingVO {
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

// HR 招聘职位 API
export const HrJobPostingApi = {
  // 查询分页
  getHrJobPostingPage: async (params: any) => {
    return await request.get({ url: '/hr/job-posting/page', params })
  },
  // 查询详情
  getHrJobPosting: async (id: number) => {
    return await request.get({ url: '/hr/job-posting/get?id=' + id })
  },
  // 新增
  createHrJobPosting: async (data: HrJobPostingVO) => {
    return await request.post({ url: '/hr/job-posting/create', data })
  },
  // 修改
  updateHrJobPosting: async (data: HrJobPostingVO) => {
    return await request.put({ url: '/hr/job-posting/update', data })
  },
  // 删除
  deleteHrJobPosting: async (id: number) => {
    return await request.delete({ url: '/hr/job-posting/delete?id=' + id })
  },
}
