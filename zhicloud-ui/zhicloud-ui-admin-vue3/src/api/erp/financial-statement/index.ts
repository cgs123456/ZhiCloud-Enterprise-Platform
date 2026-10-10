import request from '@/config/axios'

// ERP 财务报表（只读聚合端点）
export const ErpFinancialStatementApi = {
  // 资产负债表
  getBalanceSheet: async (params: any) => {
    return await request.get({ url: '/erp/financial-statement/balance-sheet', params })
  },
  // 利润表
  getIncomeStatement: async (params: any) => {
    return await request.get({ url: '/erp/financial-statement/income-statement', params })
  },
  // 现金流量表
  getCashFlow: async (params: any) => {
    return await request.get({ url: '/erp/financial-statement/cash-flow', params })
  }
}
