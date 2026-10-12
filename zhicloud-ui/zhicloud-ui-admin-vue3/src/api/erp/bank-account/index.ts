import request from '@/config/axios'

// Erp BankAccount VO
export interface BankAccountVO {
  id?: number // id
  accountNo?: string // accountNo
  accountName?: string // accountName
  bankName?: string // bankName
  bankBranch?: string // bankBranch
  balance?: number // balance
  currencyId?: number // currencyId
  status?: number // status
  remark?: string // remark
  createTime?: number // createTime
}

// Erp BankAccount API
export const BankAccountApi = {
  createBankAccount: async (data: any) => {
    // 创建BankAccount
    return await request.post({ url: `/erp/bank-account/create`, data })
  },
  updateBankAccount: async (data: any) => {
    // 更新BankAccount
    return await request.put({ url: `/erp/bank-account/update`, data })
  },
  deleteBankAccount: async (id: number) => {
    // 删除BankAccount
    return await request.delete({ url: `/erp/bank-account/delete?id=` + id })
  },
  getBankAccount: async (id: number) => {
    // 获得BankAccount
    return await request.get({ url: `/erp/bank-account/get?id=` + id })
  },
  getBankAccountPage: async (params: any) => {
    // 获得BankAccount分页
    return await request.get({ url: `/erp/bank-account/page`, params })
  },
}
