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
    // 创建工单成本归集
    return await request.post({ url: `/erp/work-order-cost/create`, data })
  },
  updateWorkOrderCost: async (data: any) => {
    // 更新工单成本归集
    return await request.put({ url: `/erp/work-order-cost/update`, data })
  },
  deleteWorkOrderCost: async (id: number) => {
    // 删除工单成本归集
    return await request.delete({ url: `/erp/work-order-cost/delete?id=` + id })
  },
  getWorkOrderCost: async (id: number) => {
    // 获得工单成本归集
    return await request.get({ url: `/erp/work-order-cost/get?id=` + id })
  },
  getWorkOrderCostPage: async (params: any) => {
    // 获得工单成本归集分页
    return await request.get({ url: `/erp/work-order-cost/page`, params })
  },
}
