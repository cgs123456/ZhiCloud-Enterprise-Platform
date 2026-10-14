import request from '@/config/axios'

/** TMS 运费结算状态（后端 TmsFreightDO 常量约定，无枚举类） */
export const TmsFreightStatus = {
  PENDING_AUDIT: 10, // 待审核
  AUDITED: 20, // 已审核
  SETTLED: 30, // 已结算
  REJECTED: 40 // 已驳回
} as const

/** 计费方式（TmsFreightDO.BILLING_*）：10 按重量 / 20 按体积 / 30 按件数 / 40 整车一口价 / 50 里程计费 */
export const TmsFreightBillingMethod = {
  WEIGHT: 10,
  VOLUME: 20,
  QUANTITY: 30,
  LUMP_SUM: 40,
  MILEAGE: 50
} as const

// TMS 运费结算 VO
export interface TmsFreightVO {
  id?: number
  no?: string // 结算单号
  shipmentId?: number // 运单编号
  carrierId?: number // 承运商编号
  billingMethod?: number // 计费方式
  billingQuantity?: number // 计费数量
  unitPrice?: number // 单价
  surcharge?: number // 附加费
  discountAmount?: number // 折扣金额
  totalAmount?: number // 总金额
  status?: number // 状态
  auditor?: string // 审核人（后端暂未写入）
  auditTime?: Date
  settleTime?: Date
  rejectReason?: string // 驳回原因
  remark?: string
  createTime?: Date
}

// TMS 运费计算结果 VO（只读，不落库）
export interface TmsFreightCalculateVO {
  billingMethod?: number
  billingQuantity?: number
  unitPrice?: number
  baseAmount?: number // 计费数量 × 单价
  surcharge?: number
  discountAmount?: number
  totalAmount?: number // 基础 + 附加 - 折扣，下限 0
}

// TMS 运费结算 API
export const TmsFreightApi = {
  // 查询分页
  getTmsFreightPage: async (params: any) => {
    return await request.get({ url: '/tms/freight/page', params })
  },
  // 查询详情
  getTmsFreight: async (id: number) => {
    return await request.get({ url: '/tms/freight/get?id=' + id })
  },
  // 新增
  createTmsFreight: async (data: TmsFreightVO) => {
    return await request.post({ url: '/tms/freight/create', data })
  },
  // 修改
  updateTmsFreight: async (data: TmsFreightVO) => {
    return await request.put({ url: '/tms/freight/update', data })
  },
  // 删除
  deleteTmsFreight: async (id: number) => {
    return await request.delete({ url: '/tms/freight/delete?id=' + id })
  },
  // 运费试算（只读：按运单与计费方式算金额，不落库）
  calculateTmsFreight: async (data: {
    shipmentId: number
    billingMethod: number
    unitPrice?: number
    surcharge?: number
    discountAmount?: number
  }) => {
    return await request.post<TmsFreightCalculateVO>({ url: '/tms/freight/calculate', data })
  },
  // 审核（pass=true 通过 / false 驳回；驳回时 reason 必填）
  auditTmsFreight: async (data: { id: number; pass: boolean; reason?: string }) => {
    const params = `id=${data.id}&pass=${data.pass}` + (data.reason ? `&reason=${encodeURIComponent(data.reason)}` : '')
    return await request.post({ url: `/tms/freight/audit?${params}` })
  },
  // 结算（要求运单已签收）
  settleTmsFreight: async (id: number) => {
    return await request.post({ url: `/tms/freight/settle?id=${id}` })
  }
}
