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
    // 创建EnergyConsumption
    return await request.post({ url: `/mes/energy-consumption/create`, data })
  },
  updateEnergyConsumption: async (data: any) => {
    // 更新EnergyConsumption
    return await request.put({ url: `/mes/energy-consumption/update`, data })
  },
  deleteEnergyConsumption: async (id: number) => {
    // 删除EnergyConsumption
    return await request.delete({ url: `/mes/energy-consumption/delete?id=` + id })
  },
  getEnergyConsumption: async (id: number) => {
    // 获得EnergyConsumption
    return await request.get({ url: `/mes/energy-consumption/get?id=` + id })
  },
  getEnergyConsumptionPage: async (params: any) => {
    // 获得EnergyConsumption分页
    return await request.get({ url: `/mes/energy-consumption/page`, params })
  },
  EnergyConsumptionSummary: async (data?: any) => {
    // 获取车间能源消耗汇总（按能源类型分组）
    return await request.get({ url: `/mes/energy-consumption/summary`, params: data })
  },
  getEnergyConsumptionList: async (params?: any) => {
    // 获得EnergyConsumption列表
    return await request.get({ url: `/mes/energy-consumption/list`, params })
  },
}
