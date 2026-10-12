import request from '@/config/axios'

// Mes Dashboard API
export const DashboardApi = {
  DashboardConnect: async (data?: any) => {
    // 获得看板 WebSocket 连接信息
    return await request.get({ url: `/mes/dashboard/connect`, params: data })
  },
  DashboardSnapshot: async (data?: any) => {
    // 获得看板同步快照（兼容无 WS 场景）
    return await request.get({ url: `/mes/dashboard/snapshot`, params: data })
  },
}
