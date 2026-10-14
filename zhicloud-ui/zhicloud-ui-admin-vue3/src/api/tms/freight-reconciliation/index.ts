import request from '@/config/axios'

/** TMS 运费对账状态（DDL DEFAULT 0，ServiceImpl 硬编码） */
export const TmsFreightReconciliationStatus = {
  PENDING: 0, // 待对账
  RECONCILED: 10, // 已对账（差异为 0）
  DIFF: 20, // 有差异
  CONFIRMED: 30, // 已确认
  REJECTED: 40 // 已驳回
} as const

// TMS 运费对账 VO
export interface TmsFreightReconciliationVO {
  id?: number
  no?: string // 对账单号
  carrierId?: number // 承运商编号
  periodStart?: Date // 对账周期起
  periodEnd?: Date // 对账周期止
  systemAmount?: number // 系统金额
  carrierAmount?: number // 承运商金额
  diffAmount?: number // 差异（服务端重算，勿提交）
  status?: number
  reconcileTime?: Date
  confirmTime?: Date
  remark?: string
  createTime?: Date
}

// TMS 运费对账 API
export const TmsFreightReconciliationApi = {
  // 查询分页
  getTmsFreightReconciliationPage: async (params: any) => {
    return await request.get({ url: '/tms/freight-reconciliation/page', params })
  },
  // 查询详情
  getTmsFreightReconciliation: async (id: number) => {
    return await request.get({ url: '/tms/freight-reconciliation/get?id=' + id })
  },
  // 新增
  createTmsFreightReconciliation: async (data: TmsFreightReconciliationVO) => {
    return await request.post({ url: '/tms/freight-reconciliation/create', data })
  },
  // 修改
  updateTmsFreightReconciliation: async (data: TmsFreightReconciliationVO) => {
    return await request.put({ url: '/tms/freight-reconciliation/update', data })
  },
  // 删除
  deleteTmsFreightReconciliation: async (id: number) => {
    return await request.delete({ url: '/tms/freight-reconciliation/delete?id=' + id })
  },
  // 执行对账（重算差异；差异 0 → 已对账(10)，否则 有差异(20)）
  doReconcileTmsFreightReconciliation: async (id: number) => {
    return await request.post({ url: `/tms/freight-reconciliation/do-reconcile?id=${id}` })
  },
  // 确认（要求已执行过对账）
  confirmTmsFreightReconciliation: async (id: number) => {
    return await request.post({ url: `/tms/freight-reconciliation/confirm?id=${id}` })
  },
  // 驳回（reason 会覆盖备注）
  rejectTmsFreightReconciliation: async (data: { id: number; reason?: string }) => {
    const params = `id=${data.id}` + (data.reason ? `&reason=${encodeURIComponent(data.reason)}` : '')
    return await request.post({ url: `/tms/freight-reconciliation/reject?${params}` })
  }
}
