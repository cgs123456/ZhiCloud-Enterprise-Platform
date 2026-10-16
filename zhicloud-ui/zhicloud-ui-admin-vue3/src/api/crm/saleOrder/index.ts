import request from '@/config/axios'

/** 销售订单状态：10 草稿 / 20 已确认 / 30 已发货 / 40 已完成 / 50 已取消 */
export const CrmSaleOrderStatus = {
  DRAFT: 10,
  CONFIRMED: 20,
  SHIPPED: 30,
  COMPLETED: 40,
  CANCELLED: 50
} as const

/** 付款状态：10 未付款 / 20 部分付款 / 30 已付款 */
export const CrmSaleOrderPaymentStatus = {
  UNPAID: 10,
  PARTIAL: 20,
  PAID: 30
} as const

/** 发货状态：10 未发货 / 20 部分发货 / 30 已发货 / 40 已签收 */
export const CrmSaleOrderDeliveryStatus = {
  UNSHIPPED: 10,
  PARTIAL_SHIPPED: 20,
  SHIPPED: 30,
  SIGNED: 40
} as const

// CRM 销售订单明细 VO
export interface CrmSaleOrderItemVO {
  id?: number
  productId?: number // 产品
  productName?: string // 产品名称
  quantity?: number // 数量
  unitPrice?: number // 单价
  discount?: number // 折扣
  taxRate?: number // 税率
  amount?: number // 金额（后端算）
  taxAmount?: number // 税额（后端算）
  remark?: string
}

// CRM 销售订单 VO
export interface CrmSaleOrderVO {
  id?: number
  no?: string // 订单号（后端生成）
  customerId?: number // 客户
  customerName?: string
  contractId?: number // 合同
  contractName?: string
  businessId?: number // 商机
  businessName?: string
  contactId?: number // 联系人
  contactName?: string
  orderDate?: Date // 订单日期
  deliveryDate?: Date // 交货日期
  ownerUserId?: number // 负责人
  ownerUserName?: string
  discountAmount?: number // 整单折扣
  totalAmount?: number // 总金额（后端算）
  finalAmount?: number // 最终金额（后端算）
  paymentStatus?: number // 付款状态
  deliveryStatus?: number // 发货状态
  status?: number // 状态
  remark?: string
  processInstanceId?: string
  creator?: string
  creatorName?: string
  createTime?: Date
  items?: CrmSaleOrderItemVO[] // 订单明细
}

// CRM 销售订单 API
export const CrmSaleOrderApi = {
  // 查询分页
  getCrmSaleOrderPage: async (params: any) => {
    return await request.get({ url: '/crm/sale-order/page', params })
  },
  // 查询详情
  getCrmSaleOrder: async (id: number) => {
    return await request.get({ url: '/crm/sale-order/get?id=' + id })
  },
  // 新增
  createCrmSaleOrder: async (data: CrmSaleOrderVO) => {
    return await request.post({ url: '/crm/sale-order/create', data })
  },
  // 修改（仅草稿状态可编辑）
  updateCrmSaleOrder: async (data: CrmSaleOrderVO) => {
    return await request.put({ url: '/crm/sale-order/update', data })
  },
  // 删除（仅草稿状态可删除）
  deleteCrmSaleOrder: async (id: number) => {
    return await request.delete({ url: '/crm/sale-order/delete?id=' + id })
  },
  // 按合同分页
  pageByContract: async (params: any) => {
    return await request.get({ url: '/crm/sale-order/page-by-contract', params })
  },
  // 按客户分页
  pageByCustomer: async (params: any) => {
    return await request.get({ url: '/crm/sale-order/page-by-customer', params })
  },
  // 确认（仅草稿）
  confirmCrmSaleOrder: async (id: number) => {
    return await request.put({ url: `/crm/sale-order/confirm?id=${id}` })
  },
  // 提交审批（仅草稿；暂不接入 BPM，直接更新为已确认）
  submitCrmSaleOrder: async (id: number) => {
    return await request.put({ url: `/crm/sale-order/submit?id=${id}` })
  },
  // 发货（仅已确认）
  shipCrmSaleOrder: async (id: number) => {
    return await request.put({ url: `/crm/sale-order/ship?id=${id}` })
  },
  // 完成（仅已发货）
  completeCrmSaleOrder: async (id: number) => {
    return await request.put({ url: `/crm/sale-order/complete?id=${id}` })
  },
  // 取消（仅草稿/已确认）
  cancelCrmSaleOrder: async (id: number) => {
    return await request.put({ url: `/crm/sale-order/cancel?id=${id}` })
  }
}
