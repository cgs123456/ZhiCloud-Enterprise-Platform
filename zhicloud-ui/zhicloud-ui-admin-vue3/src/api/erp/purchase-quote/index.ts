import request from '@/config/axios'

// ERP 采购报价单 VO
export interface ErpPurchaseQuoteVO {
  id?: number
  no?: string
  inquiryId?: number
  inquiryNo?: string
  supplierId?: number
  supplierName?: string
  quoteDate?: Date
  totalAmount?: number
  status?: number
  remark?: string
  inquiryItemId?: number
  productId?: number
  productName?: string
  quantity?: number
  unitPrice?: number
  amount?: number
  deliveryDate?: Date
  createTime?: Date
}

// ERP 采购报价单 API
export const ErpPurchaseQuoteApi = {
  // 查询分页
  getErpPurchaseQuotePage: async (params: any) => {
    return await request.get({ url: '/erp/purchase-quote/page', params })
  },
  // 查询详情
  getErpPurchaseQuote: async (id: number) => {
    return await request.get({ url: '/erp/purchase-quote/get?id=' + id })
  },
  // 新增
  createErpPurchaseQuote: async (data: ErpPurchaseQuoteVO) => {
    return await request.post({ url: '/erp/purchase-quote/create', data })
  },
  // 修改
  updateErpPurchaseQuote: async (data: ErpPurchaseQuoteVO) => {
    return await request.put({ url: '/erp/purchase-quote/update', data })
  },
  // 删除
  deleteErpPurchaseQuote: async (id: number) => {
    return await request.delete({ url: '/erp/purchase-quote/delete?id=' + id })
  },
}
