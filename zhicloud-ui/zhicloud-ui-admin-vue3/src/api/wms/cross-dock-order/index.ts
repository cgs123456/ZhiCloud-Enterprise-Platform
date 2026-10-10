import request from '@/config/axios'

// WMS 越库单 VO
export interface WmsCrossDockOrderVO {
  id?: number
  no?: string
  sourceSupplierId?: number
  sourceSupplierName?: string
  targetCustomerId?: number
  targetCustomerName?: string
  receiptOrderNo?: string
  shipmentOrderNo?: string
  status?: number
  totalQuantity?: number
  totalAmount?: number
  remark?: string
  creatorName?: string
  updaterName?: string
  createTime?: Date
}

// WMS 越库单 API
export const WmsCrossDockOrderApi = {
  // 查询分页
  getWmsCrossDockOrderPage: async (params: any) => {
    return await request.get({ url: '/wms/cross-dock-order/page', params })
  },
  // 查询详情
  getWmsCrossDockOrder: async (id: number) => {
    return await request.get({ url: '/wms/cross-dock-order/get?id=' + id })
  },
  // 新增
  createWmsCrossDockOrder: async (data: WmsCrossDockOrderVO) => {
    return await request.post({ url: '/wms/cross-dock-order/create', data })
  },
  // 修改
  updateWmsCrossDockOrder: async (data: WmsCrossDockOrderVO) => {
    return await request.put({ url: '/wms/cross-dock-order/update', data })
  },
  // 删除
  deleteWmsCrossDockOrder: async (id: number) => {
    return await request.delete({ url: '/wms/cross-dock-order/delete?id=' + id })
  },
}
