import request from '@/config/axios'

// Mes EnergyConsumption VO
export interface EnergyConsumptionVO {
  id?: number // id
  workshopId?: number // workshopId
  workstationId?: number // workstationId
  energyType?: number // energyType
  recordDate?: number // recordDate
  consumption?: number // consumption
  unit?: string // unit
  unitPrice?: number // unitPrice
  totalAmount?: number // totalAmount
  remark?: string // remark
  createTime?: number // createTime
}

// Mes EnergyConsumption API
export const EnergyConsumptionApi = {
  createEnergyConsumption: async (data: any) => {
    // 创建能源消耗记录
    return await request.post({ url: `/mes/energy-consumption/create`, data })
  },
  updateEnergyConsumption: async (data: any) => {
    // 更新能源消耗记录
    return await request.put({ url: `/mes/energy-consumption/update`, data })
  },
  deleteEnergyConsumption: async (id: number) => {
    // 删除能源消耗记录
    return await request.delete({ url: `/mes/energy-consumption/delete?id=` + id })
  },
  getEnergyConsumption: async (id: number) => {
    // 获取能源消耗记录
    return await request.get({ url: `/mes/energy-consumption/get?id=` + id })
  },
  getEnergyConsumptionPage: async (params: any) => {
    // 获取能源消耗记录分页
    return await request.get({ url: `/mes/energy-consumption/page`, params })
  },
  EnergyConsumptionSummary: async (data: any) => {
    // 获取车间能源消耗汇总（按能源类型分组）
    return await request.get({ url: `/mes/energy-consumption/summary`, data })
  },
  getEnergyConsumptionList: async (params?: any) => {
    // 获取车间能源消耗明细列表
    return await request.get({ url: `/mes/energy-consumption/list`, params })
  },
}
