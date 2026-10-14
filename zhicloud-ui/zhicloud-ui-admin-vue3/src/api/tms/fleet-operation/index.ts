import request from '@/config/axios'

// TMS 车队运营记录 VO
// 注意：totalCost / profit 由服务端重算（totalCost = 四项成本之和；profit = 收入 - 总成本），
// 提交时不要传，服务端会覆盖。
export interface TmsFleetOperationVO {
  id?: number
  vehicleId?: number // 车辆编号
  operationDate?: Date // 运营日期
  mileage?: number // 行驶里程
  fuelConsumption?: number // 油耗
  fuelCost?: number // 油费
  maintenanceCost?: number // 维修保养费
  insuranceCost?: number // 保险费
  inspectionCost?: number // 年检费
  revenue?: number // 运营收入
  totalCost?: number // 总运营成本（只读，服务端重算）
  profit?: number // 运营利润（只读，服务端重算）
  remark?: string
  createTime?: Date
}

// TMS 车队运营 API
export const TmsFleetOperationApi = {
  // 查询分页
  getTmsFleetOperationPage: async (params: any) => {
    return await request.get({ url: '/tms/fleet-operation/page', params })
  },
  // 查询详情
  getTmsFleetOperation: async (id: number) => {
    return await request.get({ url: '/tms/fleet-operation/get?id=' + id })
  },
  // 新增
  createTmsFleetOperation: async (data: TmsFleetOperationVO) => {
    return await request.post({ url: '/tms/fleet-operation/create', data })
  },
  // 修改
  updateTmsFleetOperation: async (data: TmsFleetOperationVO) => {
    return await request.put({ url: '/tms/fleet-operation/update', data })
  },
  // 删除
  deleteTmsFleetOperation: async (id: number) => {
    return await request.delete({ url: '/tms/fleet-operation/delete?id=' + id })
  }
}
