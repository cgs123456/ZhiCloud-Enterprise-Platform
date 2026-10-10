import request from '@/config/axios'

// WMS 波次单 VO
export interface WmsWaveOrderVO {
  id?: number
  no?: string
  warehouseId?: number
  strategy?: number
  orderTime?: Date
  status?: number
  picker?: string
  remark?: string
  shipmentCount?: number
  skuCount?: number
  totalQuantity?: number
  totalPrice?: number
  createTime?: Date
}

// WMS 波次单 API
export const WmsWaveOrderApi = {
  // 查询分页
  getWmsWaveOrderPage: async (params: any) => {
    return await request.get({ url: '/wms/wave-order/page', params })
  },
  // 查询详情
  getWmsWaveOrder: async (id: number) => {
    return await request.get({ url: '/wms/wave-order/get?id=' + id })
  },
  // 新增
  createWmsWaveOrder: async (data: WmsWaveOrderVO) => {
    return await request.post({ url: '/wms/wave-order/create', data })
  },
  // 修改
  updateWmsWaveOrder: async (data: WmsWaveOrderVO) => {
    return await request.put({ url: '/wms/wave-order/update', data })
  },
  // 删除
  deleteWmsWaveOrder: async (id: number) => {
    return await request.delete({ url: '/wms/wave-order/delete?id=' + id })
  },
}
