import request from '@/config/axios'

// TMS 运单 VO
export interface TmsShipmentVO {
  id?: number
  no?: string
  carrierId?: number
  vehicleId?: number
  driverId?: number
  originAddress?: string
  destinationAddress?: string
  shipmentType?: number
  sourceOrderNo?: string
  totalQuantity?: number
  totalWeight?: number
  totalVolume?: number
  freightAmount?: number
  departureTime?: Date
  estimatedArrivalTime?: Date
  actualArrivalTime?: Date
  status?: number
  remark?: string
  createTime?: Date
}

// 调度请求 VO：匹配车辆/司机
export interface TmsShipmentDispatchVO {
  id: number
  vehicleId: number
  driverId: number
}

// 站点 VO
export interface TmsShipmentStopVO {
  id?: number
  shipmentId?: number
  stopOrder?: number
  address?: string
  contactName?: string
  contactPhone?: string
  stopType?: number
  status?: number
  remark?: string
}

// TMS 运单 API
export const TmsShipmentApi = {
  // 查询分页
  getTmsShipmentPage: async (params: any) => {
    return await request.get({ url: '/tms/shipment/page', params })
  },
  // 查询详情
  getTmsShipment: async (id: number) => {
    return await request.get({ url: '/tms/shipment/get?id=' + id })
  },
  // 新增
  createTmsShipment: async (data: TmsShipmentVO) => {
    return await request.post({ url: '/tms/shipment/create', data })
  },
  // 修改
  updateTmsShipment: async (data: TmsShipmentVO) => {
    return await request.put({ url: '/tms/shipment/update', data })
  },
  // 删除
  deleteTmsShipment: async (id: number) => {
    return await request.delete({ url: '/tms/shipment/delete?id=' + id })
  },
  // 获得运单站点列表
  getShipmentStopList: async (shipmentId: number) => {
    return await request.get({ url: '/tms/shipment/stop-list?shipmentId=' + shipmentId })
  },
  // 调度运单：匹配车辆/司机（状态 10 待发车 -> 20 运输中）
  dispatch: async (data: TmsShipmentDispatchVO) => {
    return await request.post({ url: '/tms/shipment/dispatch', data })
  },
  // 确认到达（状态 20 运输中 -> 30 已到达）
  confirmArrival: async (id: number) => {
    return await request.post({ url: '/tms/shipment/confirm-arrival?id=' + id })
  },
  // 确认签收（状态 30 已到达 -> 40 已签收）
  confirmSign: async (id: number) => {
    return await request.post({ url: '/tms/shipment/confirm-sign?id=' + id })
  }
}
