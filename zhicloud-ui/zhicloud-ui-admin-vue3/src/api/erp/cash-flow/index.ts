import request from '@/config/axios'

// Erp CashFlow VO
export interface CashFlowVO {
  id?: number // id
  bizType?: number // bizType
  amount?: number // amount
  bankAccountId?: number // bankAccountId
  bizOrderId?: number // bizOrderId
  bizOrderType?: string // bizOrderType
  occurDate?: number // occurDate
  remark?: string // remark
  createTime?: number // createTime
}

// Erp CashFlow API
export const CashFlowApi = {
  createCashFlow: async (data: any) => {
    // 创建现金流记录
    return await request.post({ url: `/erp/cash-flow/create`, data })
  },
  deleteCashFlow: async (id: number) => {
    // 删除现金流记录
    return await request.delete({ url: `/erp/cash-flow/delete?id=` + id })
  },
  getCashFlow: async (id: number) => {
    // 获得现金流记录
    return await request.get({ url: `/erp/cash-flow/get?id=` + id })
  },
  getCashFlowPage: async (params: any) => {
    // 获得现金流记录分页
    return await request.get({ url: `/erp/cash-flow/page`, params })
  },
  CashFlowListByPeriod: async (data: any) => {
    // 按期间获取现金流记录列表
    return await request.get({ url: `/erp/cash-flow/list-by-period`, data })
  },
}
