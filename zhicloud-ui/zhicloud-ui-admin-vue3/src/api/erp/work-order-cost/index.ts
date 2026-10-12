import request from '@/config/axios'

// Erp WorkOrderCost VO
export interface WorkOrderCostVO {
  id?: number // id
  workOrderId?: number // workOrderId
  workOrderCode?: string // workOrderCode
  productId?: number // productId
  costPeriod?: string // costPeriod
  materialCost?: number // materialCost
  laborCost?: number // laborCost
  overheadCost?: number // overheadCost
  outsourcingCost?: number // outsourcingCost
  totalCost?: number // totalCost
  quantity?: number // quantity
  unitCost?: number // unitCost
  remark?: string // remark
  createTime?: number // createTime
}

// Erp WorkOrderCost API
export const WorkOrderCostApi = {
  createWorkOrderCost: async (data: any) => {
    // 创建WorkOrderCost
    return await request.post({ url: `/erp/work-order-cost/create`, data })
  },
  updateWorkOrderCost: async (data: any) => {
    // 更新WorkOrderCost
    return await request.put({ url: `/erp/work-order-cost/update`, data })
  },
  deleteWorkOrderCost: async (id: number) => {
    // 删除WorkOrderCost
    return await request.delete({ url: `/erp/work-order-cost/delete?id=` + id })
  },
  getWorkOrderCost: async (id: number) => {
    // 获得WorkOrderCost
    return await request.get({ url: `/erp/work-order-cost/get?id=` + id })
  },
  getWorkOrderCostPage: async (params: any) => {
    // 获得WorkOrderCost分页
    return await request.get({ url: `/erp/work-order-cost/page`, params })
  },
}
