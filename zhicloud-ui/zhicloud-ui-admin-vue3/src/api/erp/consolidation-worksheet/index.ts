import request from '@/config/axios'

// Erp ConsolidationWorksheet VO
export interface ConsolidationWorksheetVO {
  id?: number // id
  consolidationPeriod?: string // consolidationPeriod
  parentCompanyId?: number // parentCompanyId
  subsidiaryCompanyId?: number // subsidiaryCompanyId
  eliminationType?: number // eliminationType
  eliminationTypeName?: string // eliminationTypeName
  eliminationAmount?: number // eliminationAmount
  description?: string // description
  status?: number // status
  statusName?: string // statusName
  sort?: number // sort
  remark?: string // remark
  createTime?: number // createTime
}

// Erp ConsolidationWorksheet API
export const ConsolidationWorksheetApi = {
  createConsolidationWorksheet: async (data: any) => {
    // 创建ConsolidationWorksheet
    return await request.post({ url: `/erp/consolidation-worksheet/create`, data })
  },
  updateConsolidationWorksheet: async (data: any) => {
    // 更新ConsolidationWorksheet
    return await request.put({ url: `/erp/consolidation-worksheet/update`, data })
  },
  deleteConsolidationWorksheet: async (id: number) => {
    // 删除ConsolidationWorksheet
    return await request.delete({ url: `/erp/consolidation-worksheet/delete?id=` + id })
  },
  getConsolidationWorksheet: async (id: number) => {
    // 获得ConsolidationWorksheet
    return await request.get({ url: `/erp/consolidation-worksheet/get?id=` + id })
  },
  getConsolidationWorksheetPage: async (params: any) => {
    // 获得ConsolidationWorksheet分页
    return await request.get({ url: `/erp/consolidation-worksheet/page`, params })
  },
  ConsolidationWorksheetApprove: async (data: any) => {
    // 审核合并工作底稿
    return await request.put({ url: `/erp/consolidation-worksheet/approve`, data })
  },
  ConsolidationWorksheetReject: async (data: any) => {
    // 驳回合并工作底稿
    return await request.put({ url: `/erp/consolidation-worksheet/reject`, data })
  },
}
