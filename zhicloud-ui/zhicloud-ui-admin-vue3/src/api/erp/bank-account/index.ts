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
    // 创建银行账户
    return await request.post({ url: `/erp/bank-account/create`, data })
  },
  updateBankAccount: async (data: any) => {
    // 更新银行账户
    return await request.put({ url: `/erp/bank-account/update`, data })
  },
  deleteBankAccount: async (id: number) => {
    // 删除银行账户
    return await request.delete({ url: `/erp/bank-account/delete?id=` + id })
  },
  getBankAccount: async (id: number) => {
    // 获得银行账户
    return await request.get({ url: `/erp/bank-account/get?id=` + id })
  },
  getBankAccountPage: async (params: any) => {
    // 获得银行账户分页
    return await request.get({ url: `/erp/bank-account/page`, params })
  },
}
