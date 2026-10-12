import request from '@/config/axios'

// Erp Cashier VO
export interface CashierVO {
  id?: number // id
  no?: string // no
  cashierType?: number // cashierType
  bankAccountId?: number // bankAccountId
  bankAccountName?: string // bankAccountName
  counterpartyName?: string // counterpartyName
  counterpartyAccount?: string // counterpartyAccount
  counterpartyBank?: string // counterpartyBank
  amount?: number // amount
  paymentMethod?: number // paymentMethod
  paymentDate?: number // paymentDate
  status?: number // status
  bankSerialNo?: string // bankSerialNo
  businessOrderNo?: string // businessOrderNo
  remark?: string // remark
  creator?: string // creator
  createTime?: number // createTime
}

// Erp Cashier API
export const CashierApi = {
  createCashier: async (data: any) => {
    // 创建出纳单
    return await request.post({ url: `/erp/cashier/create`, data })
  },
  updateCashier: async (data: any) => {
    // 更新出纳单
    return await request.put({ url: `/erp/cashier/update`, data })
  },
  deleteCashier: async (ids: any) => {
    // 删除出纳单
    return await request.delete({ url: `/erp/cashier/delete?ids=` + ids })
  },
  getCashier: async (id: number) => {
    // 获得出纳单
    return await request.get({ url: `/erp/cashier/get?id=` + id })
  },
  getCashierPage: async (params: any) => {
    // 获得出纳单分页
    return await request.get({ url: `/erp/cashier/page`, params })
  },
  CashierSubmitBank: async (id: any) => {
    // 提交银行（发送支付指令）
    return await request.put({ url: `/erp/cashier/submit-bank?id=` + id })
  },
  CashierSyncBankStatus: async (id: any) => {
    // 同步银行状态
    return await request.put({ url: `/erp/cashier/sync-bank-status?id=` + id })
  },
  CashierBankStatement: async (data: any) => {
    // 获取银行对账单（网银直联）
    return await request.get({ url: `/erp/cashier/bank-statement`, data })
  },
}
