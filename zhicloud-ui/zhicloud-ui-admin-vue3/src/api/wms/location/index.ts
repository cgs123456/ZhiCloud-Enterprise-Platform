import request from '@/config/axios'

// Wms Location VO
export interface LocationVO {
  id?: number // id
  zoneId?: number // zoneId
  warehouseId?: number // warehouseId
  code?: string // code
  name?: string // name
  barcode?: string // barcode
  type?: number // type
  capacityWeight?: number // capacityWeight
  capacityVolume?: number // capacityVolume
  status?: number // status
  remark?: string // remark
  sort?: number // sort
  createTime?: number // createTime
}

// Wms Location API
export const LocationApi = {
  createLocation: async (data: any) => {
    // 创建库位
    return await request.post({ url: `/wms/location/create`, data })
  },
  updateLocation: async (data: any) => {
    // 更新库位
    return await request.put({ url: `/wms/location/update`, data })
  },
  deleteLocation: async (id: number) => {
    // 删除库位
    return await request.delete({ url: `/wms/location/delete?id=` + id })
  },
  getLocation: async (id: number) => {
    // 获得库位
    return await request.get({ url: `/wms/location/get?id=` + id })
  },
  getLocationPage: async (params: any) => {
    // 获得库位分页
    return await request.get({ url: `/wms/location/page`, params })
  },
  getLocationList: async (params?: any) => {
    // 获得库位列表
    return await request.get({ url: `/wms/location/list`, params })
  },
  exportLocation: async (params: any) => {
    // 导出库位 Excel
    return await.download({ url: `/wms/location/export-excel`, params })
  },
}
