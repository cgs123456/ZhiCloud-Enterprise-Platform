import request from '@/config/axios'

// 供应商审核 VO
export interface SupplierAuditVO {
  id?: number
  auditNo?: string
  auditName?: string
  supplierId?: number
  supplierName?: string
  auditType?: number
  plannedDate?: Date
  actualDate?: Date
  auditor?: string
  conclusion?: number
  auditReport?: string
  status?: number
  remark?: string
  createTime?: Date
}

// 供应商审核 API
export const SupplierAuditApi = {
  // 查询供应商审核分页
  getSupplierAuditPage: async (params: any) => {
    return await request.get({ url: '/qms/supplier-audit/page', params })
  },

  // 查询供应商审核详情
  getSupplierAudit: async (id: number) => {
    return await request.get({ url: '/qms/supplier-audit/get?id=' + id })
  },
  // 新增供应商审核
  createSupplierAudit: async (data: SupplierAuditVO) => {
    return await request.post({ url: '/qms/supplier-audit/create', data })
  },

  // 修改供应商审核
  updateSupplierAudit: async (data: SupplierAuditVO) => {
    return await request.put({ url: '/qms/supplier-audit/update', data })
  },

  // 删除供应商审核
  deleteSupplierAudit: async (id: number) => {
    return await request.delete({ url: '/qms/supplier-audit/delete?id=' + id })
  },
}
