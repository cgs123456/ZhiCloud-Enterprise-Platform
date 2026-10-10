import request from '@/config/axios'

// OA 工作台门户 VO
export interface OaPortalDashboardVO {
  announcements?: any[]
  todoCount?: number
  todayMeetingCount?: number
  pendingReimburseCount?: number
  myReimburseCount?: number
  myDocumentsCount?: number
  publishedKnowledgeCount?: number
}

// OA 工作台门户 API（只读聚合端点）
export const OaPortalApi = {
  // 获取工作台门户数据
  getOaPortalDashboard: async () => {
    return await request.get({ url: '/oa/portal/dashboard' })
  }
}
