import request from '@/config/axios'

// Erp CostAllocation VO
export interface CostAllocationVO {
  id?: number // id
  costCenterId?: number // costCenterId
  allocationType?: number // allocationType
  amount?: number // amount
  allocationDate?: number // allocationDate
  targetCostCenterId?: number // targetCostCenterId
  remark?: string // remark
  createTime?: number // createTime
}

// Erp CostAllocation API
export const CostAllocationApi = {
  createCostAllocation: async (data: any) => {
    // 创建成本分摊记录
    return await request.post({ url: `/erp/cost-allocation/create`, data })
  },
  updateCostAllocation: async (data: any) => {
    // 更新成本分摊记录
    return await request.put({ url: `/erp/cost-allocation/update`, data })
  },
  deleteCostAllocation: async (id: number) => {
    // 删除成本分摊记录
    return await request.delete({ url: `/erp/cost-allocation/delete?id=` + id })
  },
  getCostAllocation: async (id: number) => {
    // 获得成本分摊记录
    return await request.get({ url: `/erp/cost-allocation/get?id=` + id })
  },
  getCostAllocationPage: async (params: any) => {
    // 获得成本分摊分页
    return await request.get({ url: `/erp/cost-allocation/page`, params })
  },
  CostAllocationAllocate: async (data: any) => {
    // 执行成本分摊
    return await request.post({ url: `/erp/cost-allocation/allocate`, data })
  },
}
