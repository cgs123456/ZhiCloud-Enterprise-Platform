import request from '@/config/axios'

// TMS 跟踪 VO
export interface TmsTrackingVO {
  id?: number
  shipmentId?: number
  eventType?: number
  eventTime?: Date
  location?: string
  longitude?: number
  latitude?: number
  description?: string
  createTime?: Date
}

// TMS 跟踪 API（上报 / 按运单查询 / 删除，无 page/update）
export const TmsTrackingApi = {
  // 按运单查询跟踪记录
  listByShipment: async (shipmentId: number) => {
    return await request.get({ url: '/tms/tracking/list-by-shipment?shipmentId=' + shipmentId })
  },
  // 详情
  getTracking: async (id: number) => {
    return await request.get({ url: '/tms/tracking/get?id=' + id })
  },
  // 上报跟踪事件
  reportTracking: async (data: TmsTrackingVO) => {
    return await request.post({ url: '/tms/tracking/report', data })
  },
  // 删除
  deleteTracking: async (id: number) => {
    return await request.delete({ url: '/tms/tracking/delete?id=' + id })
  }
}
