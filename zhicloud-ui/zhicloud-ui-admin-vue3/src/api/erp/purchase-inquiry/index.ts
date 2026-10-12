import request from '@/config/axios'

// ERP 采购询价单 VO
export interface ErpPurchaseInquiryVO {
  id?: number
  no?: string
  inquiryName?: string
  supplierIds?: string
  supplierNames?: string
  status?: number
  totalAmount?: number
  expectedDeliveryDate?: Date
  remark?: string
  creatorName?: string
  productId?: number
  productName?: string
  quantity?: number
  unit?: string
  unitPrice?: number
  deliveryDate?: Date
  createTime?: Date
}

// ERP 采购询价单 API
export const ErpPurchaseInquiryApi = {
  // 查询分页
  getErpPurchaseInquiryPage: async (params: any) => {
    return await request.get({ url: '/erp/purchase-inquiry/page', params })
  },
  // 查询详情
  getErpPurchaseInquiry: async (id: number) => {
    return await request.get({ url: '/erp/purchase-inquiry/get?id=' + id })
  },
  // 新增
  createErpPurchaseInquiry: async (data: ErpPurchaseInquiryVO) => {
    return await request.post({ url: '/erp/purchase-inquiry/create', data })
  },
  // 修改
  updateErpPurchaseInquiry: async (data: ErpPurchaseInquiryVO) => {
    return await request.put({ url: '/erp/purchase-inquiry/update', data })
  },
  // 删除
  deleteErpPurchaseInquiry: async (id: number) => {
    return await request.delete({ url: '/erp/purchase-inquiry/delete?id=' + id })
  },
}
