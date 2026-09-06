import request from '@/config/axios'

// 培训计划 VO
export interface TrainingPlanVO {
  id?: number
  planNo?: string
  planName?: string
  year?: number
  courseName?: string
  trainer?: string
  planDate?: Date
  status?: number
  remark?: string
  createTime?: Date
}

// 培训计划 API
export const TrainingPlanApi = {
  // 查询培训计划分页
  getTrainingPlanPage: async (params: any) => {
    return await request.get({ url: '/qms/training-plan/page', params })
  },

  // 查询培训计划详情
  getTrainingPlan: async (id: number) => {
    return await request.get({ url: '/qms/training-plan/get?id=' + id })
  },
  // 新增培训计划
  createTrainingPlan: async (data: TrainingPlanVO) => {
    return await request.post({ url: '/qms/training-plan/create', data })
  },

  // 修改培训计划
  updateTrainingPlan: async (data: TrainingPlanVO) => {
    return await request.put({ url: '/qms/training-plan/update', data })
  },

  // 删除培训计划
  deleteTrainingPlan: async (id: number) => {
    return await request.delete({ url: '/qms/training-plan/delete?id=' + id })
  },
}
