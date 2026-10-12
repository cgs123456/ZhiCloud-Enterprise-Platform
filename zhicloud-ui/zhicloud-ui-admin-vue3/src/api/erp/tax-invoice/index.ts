import request from '@/config/axios'

// Erp TaxInvoice VO
export interface TaxInvoiceVO {
  id?: number // id
  invoiceNo?: string // invoiceNo
  invoiceCode?: string // invoiceCode
  invoiceType?: number // invoiceType
  buyerName?: string // buyerName
  buyerTaxNo?: string // buyerTaxNo
  sellerName?: string // sellerName
  sellerTaxNo?: string // sellerTaxNo
  invoiceDate?: number // invoiceDate
  amountWithoutTax?: number // amountWithoutTax
  taxAmount?: number // taxAmount
  amountWithTax?: number // amountWithTax
  status?: number // status
  sourceOrderType?: string // sourceOrderType
  sourceOrderId?: number // sourceOrderId
  remark?: string // remark
  createTime?: number // createTime
}

// Erp TaxInvoice API
export const TaxInvoiceApi = {
  createTaxInvoice: async (data: any) => {
    // 创建TaxInvoice
    return await request.post({ url: `/erp/tax-invoice/create`, data })
  },
  updateTaxInvoice: async (data: any) => {
    // 更新TaxInvoice
    return await request.put({ url: `/erp/tax-invoice/update`, data })
  },
  deleteTaxInvoice: async (id: number) => {
    // 删除TaxInvoice
    return await request.delete({ url: `/erp/tax-invoice/delete?id=` + id })
  },
  getTaxInvoice: async (id: number) => {
    // 获得TaxInvoice
    return await request.get({ url: `/erp/tax-invoice/get?id=` + id })
  },
  getTaxInvoicePage: async (params: any) => {
    // 获得TaxInvoice分页
    return await request.get({ url: `/erp/tax-invoice/page`, params })
  },
  TaxInvoiceIssue: async (data: any) => {
    // 开具发票（草稿 → 已开具）
    return await request.post({ url: `/erp/tax-invoice/issue`, data })
  },
  TaxInvoiceRevoke: async (data: any) => {
    // 作废发票（已开具 → 已作废）
    return await request.post({ url: `/erp/tax-invoice/revoke`, data })
  },
  TaxInvoiceRed: async (data: any) => {
    // 红冲发票（已开具 → 已红冲）
    return await request.post({ url: `/erp/tax-invoice/red`, data })
  },
}
