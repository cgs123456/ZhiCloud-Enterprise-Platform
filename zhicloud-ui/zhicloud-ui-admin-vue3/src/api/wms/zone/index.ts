import request from '@/config/axios'

// Wms Zone VO
export interface ZoneVO {
  id?: number // id
  warehouseId?: number // warehouseId
  code?: string // code
  name?: string // name
  type?: number // type
  remark?: string // remark
  sort?: number // sort
  createTime?: number // createTime
}

// Wms Zone API
export const ZoneApi = {
  createZone: async (data: any) => {
    // 创建库区
    return await request.post({ url: `/wms/zone/create`, data })
  },
  updateZone: async (data: any) => {
    // 更新库区
    return await request.put({ url: `/wms/zone/update`, data })
  },
  deleteZone: async (id: number) => {
    // 删除库区
    return await request.delete({ url: `/wms/zone/delete?id=` + id })
  },
  getZone: async (id: number) => {
    // 获得库区
    return await request.get({ url: `/wms/zone/get?id=` + id })
  },
  getZonePage: async (params: any) => {
    // 获得库区分页
    return await request.get({ url: `/wms/zone/page`, params })
  },
  getZoneList: async (params?: any) => {
    // 获得库区列表
    return await request.get({ url: `/wms/zone/list`, params })
  },
  exportZone: async (params: any) => {
    // 导出库区 Excel
    return await.download({ url: `/wms/zone/export-excel`, params })
  },
}
