import request from '@/config/axios'

// Erp ExchangeRate VO
export interface ExchangeRateVO {
  id?: number // id
  fromCurrencyId?: number // fromCurrencyId
  toCurrencyId?: number // toCurrencyId
  rate?: number // rate
  effectiveDate?: number // effectiveDate
  expiryDate?: number // expiryDate
  remark?: string // remark
  createTime?: number // createTime
}

// Erp ExchangeRate API
export const ExchangeRateApi = {
  createExchangeRate: async (data: any) => {
    // 创建汇率
    return await request.post({ url: `/erp/exchange-rate/create`, data })
  },
  updateExchangeRate: async (data: any) => {
    // 更新汇率
    return await request.put({ url: `/erp/exchange-rate/update`, data })
  },
  deleteExchangeRate: async (id: number) => {
    // 删除汇率
    return await request.delete({ url: `/erp/exchange-rate/delete?id=` + id })
  },
  getExchangeRate: async (id: number) => {
    // 获得汇率
    return await request.get({ url: `/erp/exchange-rate/get?id=` + id })
  },
  getExchangeRatePage: async (params: any) => {
    // 获得汇率分页
    return await request.get({ url: `/erp/exchange-rate/page`, params })
  },
  ExchangeRateLatest: async (data: any) => {
    // 获取最新汇率
    return await request.get({ url: `/erp/exchange-rate/latest`, data })
  },
}
