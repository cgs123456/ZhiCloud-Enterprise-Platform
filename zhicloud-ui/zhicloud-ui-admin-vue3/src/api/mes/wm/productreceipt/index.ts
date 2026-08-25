import request from '@/config/axios'

// MES 产品收货单 VO
export interface WmProductRecptVO {
  id: number
  code: string
  name: string
  workOrderId: number
  workOrderCode: string
  itemId: number
  itemCode: string
  itemName: string
  specification: string
  unitMeasureName: string
  receiptDate: string
  status: number
  remark: string
  createTime: string
}

// MES 产品收货单 API
export const WmProductRecptApi = {
  // 查询产品收货单分页
  getProductRecptPage: async (params: any) => {
    return await request.get({ url: '/mes/wm/product-receipt/page', params })
  },

  // 查询产品收货单详情
  getProductRecpt: async (id: number) => {
    return await request.get({ url: '/mes/wm/product-receipt/get?id=' + id })
  },

  // 新增产品收货单
  createProductRecpt: async (data: WmProductRecptVO) => {
    return await request.post({ url: '/mes/wm/product-receipt/create', data })
  },

  // 修改产品收货单
  updateProductRecpt: async (data: WmProductRecptVO) => {
    return await request.put({ url: '/mes/wm/product-receipt/update', data })
  },

  // 删除产品收货单
  deleteProductRecpt: async (id: number) => {
    return await request.delete({ url: '/mes/wm/product-receipt/delete?id=' + id })
  },

  // 提交产品收货单
  submitProductRecpt: async (id: number) => {
    return await request.put({ url: '/mes/wm/product-receipt/submit?id=' + id })
  },

  // 执行上架
  stockProductRecpt: async (id: number) => {
    return await request.put({ url: '/mes/wm/product-receipt/stock?id=' + id })
  },

  // 执行入库
  executeProductRecpt: async (id: number) => {
    return await request.put({ url: '/mes/wm/product-receipt/finish?id=' + id })
  },

  // 取消产品收货单
  cancelProductRecpt: async (id: number) => {
    return await request.put({ url: '/mes/wm/product-receipt/cancel?id=' + id })
  },

  // 导出产品收货单 Excel
  exportProductRecpt: async (params: any) => {
    return await request.download({ url: '/mes/wm/product-receipt/export-excel', params })
  }
}
