import request from '@/config/axios'

// Erp CreditLimit VO
export interface CreditLimitVO {
  id?: number // id
  customerId?: number // customerId
  customerName?: string // customerName
  creditLimit?: number // creditLimit
  usedAmount?: number // usedAmount
  availableAmount?: number // availableAmount
  overdueAmount?: number // overdueAmount
  warningRatio?: number // warningRatio
  status?: number // status
  remark?: string // remark
  creator?: string // creator
  createTime?: number // createTime
}

// Erp CreditLimit API
export const CreditLimitApi = {
  createCreditLimit: async (data: any) => {
    // 创建CreditLimit
    return await request.post({ url: `/erp/credit-limit/create`, data })
  },
  updateCreditLimit: async (data: any) => {
    // 更新CreditLimit
    return await request.put({ url: `/erp/credit-limit/update`, data })
  },
  deleteCreditLimit: async (id: number) => {
    // 删除CreditLimit
    return await request.delete({ url: `/erp/credit-limit/delete?id=` + id })
  },
  getCreditLimit: async (id: number) => {
    // 获得CreditLimit
    return await request.get({ url: `/erp/credit-limit/get?id=` + id })
  },
  getCreditLimitPage: async (params: any) => {
    // 获得CreditLimit分页
    return await request.get({ url: `/erp/credit-limit/page`, params })
  },
}
