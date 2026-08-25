import request from '@/config/axios'

// MES SN 码 VO
export interface WmSnVO {
  id: number
  uuid: string
  code: string
  itemId: number
  itemCode: string
  itemName: string
  specification: string
  unitName: string
  batchCode: string
  workOrderId: number
  createTime: string
}

// MES SN 码分组 VO（后端按批次 UUID 聚合）
export interface WmSnGroupVO {
  uuid: string
  count: number
  itemId: number
  itemCode: string
  itemName: string
  specification: string
  unitName: string
  batchCode: string
  workOrderId: number
  createTime: string
}

// MES SN 码生成 VO（对应后端 MesWmSnGenerateReqVO）
export interface WmSnGenerateVO {
  itemId: number
  batchCode?: string
  workOrderId?: number
  count: number
}

// MES SN 码 API
export const WmSnApi = {
  // 生成 SN 码
  generateSnCodes: async (data: WmSnGenerateVO) => {
    return await request.post({ url: '/mes/wm/sn/generate', data })
  },

  // 查询 SN 码分组分页
  getSnGroupPage: async (params: any) => {
    return await request.get({ url: '/mes/wm/sn/group-page', params })
  },

  // 查询批次 SN 码明细列表
  getSnListByUuid: async (uuid: string) => {
    return await request.get({ url: '/mes/wm/sn/list-by-uuid', params: { uuid } })
  },

  // 批量删除 SN 码（按批次 UUID）
  deleteSnBatch: async (uuid: string) => {
    return await request.delete({ url: '/mes/wm/sn/delete-batch', params: { uuid } })
  },

  // 导出 SN 码分组 Excel
  exportSnGroupExcel: async (params: any) => {
    return await request.download({ url: '/mes/wm/sn/group-export-excel', params })
  },

  // 导出批次 SN 码明细 Excel
  exportSnExcel: async (uuid: string) => {
    return await request.download({ url: '/mes/wm/sn/export-excel', params: { uuid } })
  }
}
