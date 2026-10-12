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
    // 创建币种
    return await request.post({ url: `/erp/currency/create`, data })
  },
  updateCurrency: async (data: any) => {
    // 更新币种
    return await request.put({ url: `/erp/currency/update`, data })
  },
  deleteCurrency: async (id: number) => {
    // 删除币种
    return await request.delete({ url: `/erp/currency/delete?id=` + id })
  },
  getCurrency: async (id: number) => {
    // 获得币种
    return await request.get({ url: `/erp/currency/get?id=` + id })
  },
  getCurrencyPage: async (params: any) => {
    // 获得币种分页
    return await request.get({ url: `/erp/currency/page`, params })
  },
  getCurrencyList: async (params?: any) => {
    // 获得启用的币种精简列表
    return await request.get({ url: `/erp/currency/simple-list`, params })
  },
}
