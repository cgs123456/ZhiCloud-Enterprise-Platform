import request from '@/config/axios'

// TMS 车辆 VO
export interface TmsVehicleVO {
  id?: number
  vehicleId?: number
  operationDate?: Date
  mileage?: number
  fuelConsumption?: number
  fuelCost?: number
  maintenanceCost?: number
  insuranceCost?: number
  inspectionCost?: number
  revenue?: number
  totalCost?: number
  profit?: number
  remark?: string
  createTime?: Date
}

// TMS 车辆 API
export const TmsVehicleApi = {
  // 查询分页
  getTmsVehiclePage: async (params: any) => {
    return await request.get({ url: '/tms/vehicle/page', params })
  },
  // 查询详情
  getTmsVehicle: async (id: number) => {
    return await request.get({ url: '/tms/vehicle/get?id=' + id })
  },
  // 新增
  createTmsVehicle: async (data: TmsVehicleVO) => {
    return await request.post({ url: '/tms/vehicle/create', data })
  },
  // 修改
  updateTmsVehicle: async (data: TmsVehicleVO) => {
    return await request.put({ url: '/tms/vehicle/update', data })
  },
  // 删除
  deleteTmsVehicle: async (id: number) => {
    return await request.delete({ url: '/tms/vehicle/delete?id=' + id })
  },
}
