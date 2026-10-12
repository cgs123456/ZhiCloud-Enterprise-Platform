import request from '@/config/axios'

// Erp ConsolidationScope VO
export interface ConsolidationScopeVO {
  id?: number // id
  parentCompanyId?: number // parentCompanyId
  subsidiaryCompanyId?: number // subsidiaryCompanyId
  holdingRatio?: number // holdingRatio
  consolidationMethod?: number // consolidationMethod
  consolidationMethodName?: string // consolidationMethodName
  effectiveDate?: number // effectiveDate
  expiryDate?: number // expiryDate
  status?: number // status
  statusName?: string // statusName
  remark?: string // remark
  createTime?: number // createTime
}

// Erp ConsolidationScope API
export const ConsolidationScopeApi = {
  createConsolidationScope: async (data: any) => {
    // 创建ConsolidationScope
    return await request.post({ url: `/erp/consolidation-scope/create`, data })
  },
  updateConsolidationScope: async (data: any) => {
    // 更新ConsolidationScope
    return await request.put({ url: `/erp/consolidation-scope/update`, data })
  },
  deleteConsolidationScope: async (id: number) => {
    // 删除ConsolidationScope
    return await request.delete({ url: `/erp/consolidation-scope/delete?id=` + id })
  },
  getConsolidationScope: async (id: number) => {
    // 获得ConsolidationScope
    return await request.get({ url: `/erp/consolidation-scope/get?id=` + id })
  },
  getConsolidationScopePage: async (params: any) => {
    // 获得ConsolidationScope分页
    return await request.get({ url: `/erp/consolidation-scope/page`, params })
  },
}
