import request from '@/config/axios'

// 培训记录 VO
export interface TrainingRecordVO {
  id?: number
  recordNo?: string
  planId?: number
  traineeId?: number
  traineeName?: string
  score?: number
  passed?: number
  certificateNo?: string
  certificateExpireDate?: Date
  status?: number
  remark?: string
  createTime?: Date
}

// 培训记录 API
export const TrainingRecordApi = {
  // 查询培训记录分页
  getTrainingRecordPage: async (params: any) => {
    return await request.get({ url: '/qms/training-record/page', params })
  },

  // 查询培训记录详情
  getTrainingRecord: async (id: number) => {
    return await request.get({ url: '/qms/training-record/get?id=' + id })
  },
  // 新增培训记录
  createTrainingRecord: async (data: TrainingRecordVO) => {
    return await request.post({ url: '/qms/training-record/create', data })
  },

  // 修改培训记录
  updateTrainingRecord: async (data: TrainingRecordVO) => {
    return await request.put({ url: '/qms/training-record/update', data })
  },

  // 删除培训记录
  deleteTrainingRecord: async (id: number) => {
    return await request.delete({ url: '/qms/training-record/delete?id=' + id })
  },
}
