import request from '@/config/axios'

// ERP 合并抵消分录 VO
export interface ErpConsolidationEntryVO {
  id?: number
  consolidationNo?: string
  periodId?: number
  periodCode?: string
  eliminationType?: number
  eliminationTypeName?: string
  debitAccountId?: number
  debitAccountCode?: string
  debitAccountName?: string
  creditAccountId?: number
  creditAccountCode?: string
  creditAccountName?: string
  eliminationAmount?: number
  status?: number
  statusName?: string
  remark?: string
  createTime?: Date
}

// ERP 合并抵消分录 API
export const ErpConsolidationEntryApi = {
  // 查询分页
  getErpConsolidationEntryPage: async (params: any) => {
    return await request.get({ url: '/erp/consolidation-entry/page', params })
  },
  // 查询详情
  getErpConsolidationEntry: async (id: number) => {
    return await request.get({ url: '/erp/consolidation-entry/get?id=' + id })
  },
  // 新增
  createErpConsolidationEntry: async (data: ErpConsolidationEntryVO) => {
    return await request.post({ url: '/erp/consolidation-entry/create', data })
  },
  // 修改
  updateErpConsolidationEntry: async (data: ErpConsolidationEntryVO) => {
    return await request.put({ url: '/erp/consolidation-entry/update', data })
  },
  // 删除
  deleteErpConsolidationEntry: async (id: number) => {
    return await request.delete({ url: '/erp/consolidation-entry/delete?id=' + id })
  },
}
