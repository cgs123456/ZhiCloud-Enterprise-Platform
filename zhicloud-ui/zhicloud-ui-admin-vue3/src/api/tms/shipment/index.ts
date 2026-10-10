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
}
