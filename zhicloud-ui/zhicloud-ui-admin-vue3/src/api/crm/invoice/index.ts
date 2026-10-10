import request from '@/config/axios'

// CRM 开票管理 VO
export interface CrmInvoiceVO {
  id?: number
  no?: string
  contractId?: number
  contractName?: string
  customerId?: number
  customerName?: string
  contactId?: number
  contactName?: string
  invoiceType?: number
  invoiceNo?: string
  buyerName?: string
  buyerTaxNo?: string
  amountWithoutTax?: number
  taxAmount?: number
  amountWithTax?: number
  invoiceDate?: Date
  auditStatus?: number
  processInstanceId?: string
  ownerUserId?: number
  ownerUserName?: string
  ownerUserDeptName?: string
  remark?: string
  creatorName?: string
  createTime?: Date
}

// CRM 开票管理 API
export const CrmInvoiceApi = {
  // 查询分页
  getCrmInvoicePage: async (params: any) => {
    return await request.get({ url: '/crm/invoice/page', params })
  },
  // 查询详情
  getCrmInvoice: async (id: number) => {
    return await request.get({ url: '/crm/invoice/get?id=' + id })
  },
  // 新增
  createCrmInvoice: async (data: CrmInvoiceVO) => {
    return await request.post({ url: '/crm/invoice/create', data })
  },
  // 修改
  updateCrmInvoice: async (data: CrmInvoiceVO) => {
    return await request.put({ url: '/crm/invoice/update', data })
  },
  // 删除
  deleteCrmInvoice: async (id: number) => {
    return await request.delete({ url: '/crm/invoice/delete?id=' + id })
  },
}
