import request from '@/config/axios'

// Erp TaxInvoiceLine VO
export interface TaxInvoiceLineVO {
  id?: number // id
  invoiceId?: number // invoiceId
  lineNo?: number // lineNo
  productName?: string // productName
  specification?: string // specification
  unit?: string // unit
  quantity?: number // quantity
  unitPrice?: number // unitPrice
  amountWithoutTax?: number // amountWithoutTax
  taxRate?: number // taxRate
  taxAmount?: number // taxAmount
  amountWithTax?: number // amountWithTax
  remark?: string // remark
  createTime?: number // createTime
}

// Erp TaxInvoiceLine API
export const TaxInvoiceLineApi = {
  createTaxInvoiceLine: async (data: any) => {
    // 创建TaxInvoiceLine
    return await request.post({ url: `/erp/tax-invoice-line/create`, data })
  },
  updateTaxInvoiceLine: async (data: any) => {
    // 更新TaxInvoiceLine
    return await request.put({ url: `/erp/tax-invoice-line/update`, data })
  },
  deleteTaxInvoiceLine: async (id: number) => {
    // 删除TaxInvoiceLine
    return await request.delete({ url: `/erp/tax-invoice-line/delete?id=` + id })
  },
  getTaxInvoiceLine: async (id: number) => {
    // 获得TaxInvoiceLine
    return await request.get({ url: `/erp/tax-invoice-line/get?id=` + id })
  },
  getTaxInvoiceLinePage: async (params: any) => {
    // 获得TaxInvoiceLine分页
    return await request.get({ url: `/erp/tax-invoice-line/page`, params })
  },
  TaxInvoiceLineListByInvoice: async (data?: any) => {
    // 根据发票 ID 获得明细列表
    return await request.get({ url: `/erp/tax-invoice-line/list-by-invoice`, params: data })
  },
}
