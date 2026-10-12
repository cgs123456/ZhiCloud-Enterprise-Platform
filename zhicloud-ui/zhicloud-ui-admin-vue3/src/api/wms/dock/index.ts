import request from '@/config/axios'

// Wms Dock VO
export interface DockVO {
  id?: number // id
  warehouseId?: number // warehouseId
  warehouseName?: string // warehouseName
  dockCode?: string // dockCode
  dockName?: string // dockName
  dockType?: number // dockType
  status?: number // status
  remark?: string // remark
  createTime?: number // createTime
}

// Wms Dock API
export const DockApi = {
  createDock: async (data: any) => {
    // 创建Dock
    return await request.post({ url: `/wms/dock/create`, data })
  },
  updateDock: async (data: any) => {
    // 更新Dock
    return await request.put({ url: `/wms/dock/update`, data })
  },
  deleteDock: async (id: number) => {
    // 删除Dock
    return await request.delete({ url: `/wms/dock/delete?id=` + id })
  },
  getDock: async (id: number) => {
    // 获得Dock
    return await request.get({ url: `/wms/dock/get?id=` + id })
  },
  getDockPage: async (params: any) => {
    // 获得Dock分页
    return await request.get({ url: `/wms/dock/page`, params })
  },
}
