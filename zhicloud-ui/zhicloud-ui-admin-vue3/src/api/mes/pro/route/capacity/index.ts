import request from '@/config/axios'

// MES 工艺路线产能 VO
export interface ProRouteCapacityVO {
  id?: number // 编号
  routeId: number // 工艺路线编号
  capacity: number // 产能（件/小时；0 = 未配置）
  efficiency: number // 效率系数（>0）
  remark?: string // 备注
  createTime?: Date // 创建时间
}

// MES 工艺路线产能 API
export const ProRouteCapacityApi = {
  // 按工艺路线查询产能配置（未配置返回 null）
  getRouteCapacityByRoute: async (routeId: number) => {
    return await request.get({ url: `/mes/pro/route-capacity/get-by-route?routeId=` + routeId })
  },

  // 保存工艺路线产能（upsert）
  saveRouteCapacity: async (data: ProRouteCapacityVO) => {
    return await request.put({ url: `/mes/pro/route-capacity/save`, data })
  }
}
