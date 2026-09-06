import request from '@/config/axios'

// 审核计划 VO
export interface QmsAuditPlanVO {
  id?: number
  planNo?: string
  auditType?: number
  title?: string
  auditStandard?: string
  auditScope?: string
  auditPurpose?: string
  leadAuditorId?: number
  auditStartDate?: Date
  auditEndDate?: Date
  status?: number
  remark?: string
  sort?: number
  createTime?: Date
}

// 审核计划 API
export const QmsAuditPlanApi = {
  // 查询审核计划分页
  getQmsAuditPlanPage: async (params: any) => {
    return await request.get({ url: '/qms/audit-plan/page', params })
  },

  // 查询审核计划详情
  getQmsAuditPlan: async (id: number) => {
    return await request.get({ url: '/qms/audit-plan/get?id=' + id })
  },
  // 新增审核计划
  createQmsAuditPlan: async (data: QmsAuditPlanVO) => {
    return await request.post({ url: '/qms/audit-plan/create', data })
  },

  // 修改审核计划
  updateQmsAuditPlan: async (data: QmsAuditPlanVO) => {
    return await request.put({ url: '/qms/audit-plan/update', data })
  },

  // 删除审核计划
  deleteQmsAuditPlan: async (id: number) => {
    return await request.delete({ url: '/qms/audit-plan/delete?id=' + id })
  },
}
