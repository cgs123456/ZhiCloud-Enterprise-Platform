import request from '@/config/axios'

// MES PDA 报工（手持端）
export const MesPdaApi = {
  // 查询当前用户的报工记录
  getMyFeedbackList: async () => {
    return await request.get({ url: '/mes-api/pro/feedback/my-feedback-list' })
  }
}
