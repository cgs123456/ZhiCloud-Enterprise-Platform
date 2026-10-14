import request from '@/config/axios'

/** 售后工单状态机：10 待分配 → 20 已分配 → 30 处理中 → 40 已解决 → 50 已关闭 */
export const CrmWorkOrderStatus = {
  UNASSIGNED: 10,
  ASSIGNED: 20,
  PROCESSING: 30,
  RESOLVED: 40,
  CLOSED: 50
} as const

/** 工单类型：10 安装 / 20 维修 / 30 投诉 / 40 咨询 / 50 退换货 */
export const CrmWorkOrderType = {
  INSTALL: 10,
  REPAIR: 20,
  COMPLAINT: 30,
  CONSULT: 40,
  RETURN: 50
} as const

/** 优先级：10 低 / 20 中 / 30 高 / 40 紧急 */
export const CrmWorkOrderPriority = {
  LOW: 10,
  MEDIUM: 20,
  HIGH: 30,
  URGENT: 40
} as const

// CRM 售后工单 VO
export interface CrmWorkOrderVO {
  id?: number
  no?: string // 工单号（后端生成）
  title?: string // 标题
  customerId?: number // 客户
  customerName?: string
  contactId?: number // 联系人
  contactName?: string
  productId?: number // 产品
  productName?: string
  workOrderType?: number // 工单类型
  priority?: number // 优先级
  description?: string // 问题描述
  assigneeUserId?: number // 处理人
  assigneeUserName?: string
  slaDeadline?: Date // SLA 截止
  status?: number // 状态
  resolution?: string // 解决方案
  respondTime?: Date // 响应时间
  resolveTime?: Date // 解决时间
  remark?: string
  creator?: string
  creatorName?: string
  createTime?: Date
}

// CRM 售后工单 API
export const CrmWorkOrderApi = {
  // 查询分页
  getCrmWorkOrderPage: async (params: any) => {
    return await request.get({ url: '/crm/work-order/page', params })
  },
  // 查询详情
  getCrmWorkOrder: async (id: number) => {
    return await request.get({ url: '/crm/work-order/get?id=' + id })
  },
  // 新增（创建时固定为待分配）
  createCrmWorkOrder: async (data: CrmWorkOrderVO) => {
    return await request.post({ url: '/crm/work-order/create', data })
  },
  // 修改
  updateCrmWorkOrder: async (data: CrmWorkOrderVO) => {
    return await request.put({ url: '/crm/work-order/update', data })
  },
  // 删除
  deleteCrmWorkOrder: async (id: number) => {
    return await request.delete({ url: '/crm/work-order/delete?id=' + id })
  },
  // 分配处理人（仅待分配）
  assignCrmWorkOrder: async (data: { id: number; assigneeUserId: number }) => {
    return await request.put({
      url: `/crm/work-order/assign?id=${data.id}&assigneeUserId=${data.assigneeUserId}`
    })
  },
  // 解决（仅已分配/处理中；需解决方案）
  resolveCrmWorkOrder: async (data: { id: number; resolution: string }) => {
    return await request.put({
      url: `/crm/work-order/resolve?id=${data.id}&resolution=${encodeURIComponent(data.resolution)}`
    })
  },
  // 关闭（仅已解决）
  closeCrmWorkOrder: async (id: number) => {
    return await request.put({ url: `/crm/work-order/close?id=${id}` })
  }
}
