import request from '@/config/axios'

// 审核报告 VO
export interface QmsAuditReportVO {
  id?: number
  planId?: number
  reportNo?: string
  auditSummary?: string
  conclusion?: number
  issueCount?: number
  remark?: string
  sort?: number
  createTime?: Date
}

// 审核报告 API
export const QmsAuditReportApi = {
  // 查询审核报告分页
  getQmsAuditReportPage: async (params: any) => {
    return await request.get({ url: '/qms/audit-report/page', params })
  },

  // 查询审核报告详情
  getQmsAuditReport: async (id: number) => {
    return await request.get({ url: '/qms/audit-report/get?id=' + id })
  },
  // 新增审核报告
  createQmsAuditReport: async (data: QmsAuditReportVO) => {
    return await request.post({ url: '/qms/audit-report/create', data })
  },

  // 修改审核报告
  updateQmsAuditReport: async (data: QmsAuditReportVO) => {
    return await request.put({ url: '/qms/audit-report/update', data })
  },

  // 删除审核报告
  deleteQmsAuditReport: async (id: number) => {
    return await request.delete({ url: '/qms/audit-report/delete?id=' + id })
  },
}
