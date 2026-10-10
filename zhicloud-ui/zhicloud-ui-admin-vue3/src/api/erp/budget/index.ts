import request from '@/config/axios'

// ERP 预算管理 VO
export interface ErpBudgetVO {
  id?: number
  budgetNo?: string
  budgetYear?: number
  periodId?: number
  periodCode?: string
  departmentId?: number
  budgetType?: number
  budgetTypeName?: string
  totalAmount?: number
  status?: number
  statusName?: string
  remark?: string
  createTime?: Date
}

// ERP 预算管理 API
export const ErpBudgetApi = {
  // 查询分页
  getErpBudgetPage: async (params: any) => {
    return await request.get({ url: '/erp/budget/page', params })
  },
  // 查询详情
  getErpBudget: async (id: number) => {
    return await request.get({ url: '/erp/budget/get?id=' + id })
  },
  // 新增
  createErpBudget: async (data: ErpBudgetVO) => {
    return await request.post({ url: '/erp/budget/create', data })
  },
  // 修改
  updateErpBudget: async (data: ErpBudgetVO) => {
    return await request.put({ url: '/erp/budget/update', data })
  },
  // 删除
  deleteErpBudget: async (id: number) => {
    return await request.delete({ url: '/erp/budget/delete?id=' + id })
  },
}
