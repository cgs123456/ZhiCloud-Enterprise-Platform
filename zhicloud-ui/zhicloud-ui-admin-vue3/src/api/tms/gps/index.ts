import request from '@/config/axios'

// TMS GPS 定位 VO
export interface TmsGpsPositionVO {
  id?: number
  vehicleId?: number // 车辆编号
  shipmentId?: number // 运单编号（可空）
  longitude?: number // 经度
  latitude?: number // 纬度
  speed?: number // 速度（km/h）
  direction?: number // 方向（0-360）
  reportTime?: Date // 上报时间
  locationDesc?: string // 位置描述
  createTime?: Date
}

// TMS GPS 定位 API
// 注意：GPS 域无 CRUD——report 是唯一写入口（供外部设备/平台推送），其余三端点为只读查询。
export const TmsGpsApi = {
  // 上报位置（vehicleId 必须存在；reportTime 留空由服务端填当前时间）
  reportTmsGpsPosition: async (data: TmsGpsPositionVO) => {
    return await request.post({ url: '/tms/gps/report', data })
  },
  // 车辆最新位置（单个对象；无记录返回 null）
  getLatestTmsGpsPosition: async (vehicleId: number) => {
    return await request.get({ url: '/tms/gps/latest-position?vehicleId=' + vehicleId })
  },
  // 运单轨迹（按上报时间正序）
  getShipmentTrack: async (shipmentId: number) => {
    return await request.get({ url: '/tms/gps/shipment-track?shipmentId=' + shipmentId })
  },
  // 车辆轨迹（时间区间；格式 yyyy-MM-ddTHH:mm:ss）
  getVehicleTrack: async (params: { vehicleId: number; startTime: string; endTime: string }) => {
    return await request.get({ url: '/tms/gps/vehicle-track', params })
  }
}
