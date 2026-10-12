import request from '@/config/axios'

// Erp Currency VO
export interface CurrencyVO {
  id?: number // id
  code?: string // code
  name?: string // name
  symbol?: string // symbol
  isBase?: boolean // isBase
  enabled?: number // enabled
  remark?: string // remark
  createTime?: number // createTime
}

// Erp Currency API
export const CurrencyApi = {
  createCurrency: async (data: any) => {
    // 创建Currency
    return await request.post({ url: `/erp/currency/create`, data })
  },
  updateCurrency: async (data: any) => {
    // 更新Currency
    return await request.put({ url: `/erp/currency/update`, data })
  },
  deleteCurrency: async (id: number) => {
    // 删除Currency
    return await request.delete({ url: `/erp/currency/delete?id=` + id })
  },
  getCurrency: async (id: number) => {
    // 获得Currency
    return await request.get({ url: `/erp/currency/get?id=` + id })
  },
  getCurrencyPage: async (params: any) => {
    // 获得Currency分页
    return await request.get({ url: `/erp/currency/page`, params })
  },
  getCurrencyList: async (params?: any) => {
    // 获得Currency列表
    return await request.get({ url: `/erp/currency/simple-list`, params })
  },
}
