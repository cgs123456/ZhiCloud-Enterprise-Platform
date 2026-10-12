import request from '@/config/axios'

// Wms AsnOrder VO
export interface AsnOrderVO {
  id?: number // id
  no?: string // no
  supplierId?: number // supplierId
  supplierName?: string // supplierName
  warehouseId?: number // warehouseId
  warehouseName?: string // warehouseName
  dockId?: number // dockId
  dockName?: string // dockName
  expectedArrivalTime?: number // expectedArrivalTime
  actualArrivalTime?: number // actualArrivalTime
  status?: number // status
  totalQuantity?: number // totalQuantity
  totalAmount?: number // totalAmount
  transportMode?: number // transportMode
  carrierName?: string // carrierName
  vehicleNo?: string // vehicleNo
  remark?: string // remark
  details?: any[] // details
  createTime?: number // createTime
  creator?: string // creator
  updateTime?: number // updateTime
}

// Wms AsnOrder API
export const AsnOrderApi = {
  createAsnOrder: async (data: any) => {
    // 创建 ASN 到货通知单
    return await request.post({ url: `/wms/asn-order/create`, data })
  },
  updateAsnOrder: async (data: any) => {
    // 更新 ASN 到货通知单
    return await request.put({ url: `/wms/asn-order/update`, data })
  },
  deleteAsnOrder: async (id: number) => {
    // 删除 ASN 到货通知单
    return await request.delete({ url: `/wms/asn-order/delete?id=` + id })
  },
  AsnOrderClose: async (id: any) => {
    // 关闭 ASN 到货通知单
    return await request.put({ url: `/wms/asn-order/close?id=` + id })
  },
  AsnOrderConfirmArrival: async (id: any) => {
    // 确认到货
    return await request.put({ url: `/wms/asn-order/confirm-arrival?id=` + id })
  },
  AsnOrderConvertToReceipt: async (id: any) => {
    // 转收货单
    return await request.post({ url: `/wms/asn-order/convert-to-receipt?id=` + id })
  },
  getAsnOrder: async (id: number) => {
    // 获得 ASN 到货通知单
    return await request.get({ url: `/wms/asn-order/get?id=` + id })
  },
  getAsnOrderPage: async (params: any) => {
    // 获得 ASN 到货通知单分页
    return await request.get({ url: `/wms/asn-order/page`, params })
  },
}
