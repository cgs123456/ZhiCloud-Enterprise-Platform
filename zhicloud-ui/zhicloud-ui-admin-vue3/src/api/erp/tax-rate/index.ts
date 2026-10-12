import request from '@/config/axios'

// Erp TaxRate VO
export interface TaxRateVO {
  id?: number // id
  code?: string // code
  name?: string // name
  rateType?: number // rateType
  rate?: number // rate
  isDefault?: number // isDefault
  effectiveDate?: number // effectiveDate
  expiryDate?: number // expiryDate
  remark?: string // remark
  status?: number // status
  createTime?: number // createTime
}

// Erp TaxRate API
export const TaxRateApi = {
  createTaxRate: async (data: any) => {
    // 创建税率
    return await request.post({ url: `/erp/tax-rate/create`, data })
  },
  updateTaxRate: async (data: any) => {
    // 更新税率
    return await request.put({ url: `/erp/tax-rate/update`, data })
  },
  deleteTaxRate: async (id: number) => {
    // 删除税率
    return await request.delete({ url: `/erp/tax-rate/delete?id=` + id })
  },
  getTaxRate: async (id: number) => {
    // 获得税率
    return await request.get({ url: `/erp/tax-rate/get?id=` + id })
  },
  getTaxRatePage: async (params: any) => {
    // 获得税率分页
    return await request.get({ url: `/erp/tax-rate/page`, params })
  },
  TaxRateGetDefault: async (data: any) => {
    // 获得默认税率
    return await request.get({ url: `/erp/tax-rate/get-default`, data })
  },
}
