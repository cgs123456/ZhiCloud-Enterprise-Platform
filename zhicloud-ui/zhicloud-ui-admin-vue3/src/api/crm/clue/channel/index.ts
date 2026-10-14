import request from '@/config/axios'

/** 线索渠道类型：10 官网 / 20 广告 / 30 企微 / 40 小程序 / 50 API / 60 线下 */
export const CrmClueChannelType = {
  OFFICIAL_SITE: 10,
  ADVERTISEMENT: 20,
  WECOM: 30,
  MINI_PROGRAM: 40,
  API: 50,
  OFFLINE: 60
} as const

/** 线索渠道状态：10 启用 / 20 停用 */
export const CrmClueChannelStatus = {
  ENABLED: 10,
  DISABLED: 20
} as const

// CRM 线索渠道 VO
export interface CrmClueChannelVO {
  id?: number
  channelName?: string // 渠道名称
  channelType?: number // 渠道类型
  channelCode?: string // 渠道编码（唯一）
  apiUrl?: string // API 地址
  apiKey?: string // API 密钥
  autoAssignUserId?: number // 自动分配负责人
  status?: number // 状态
  remark?: string
  autoAssignUserName?: string // 自动分配负责人姓名（后端拼）
  createTime?: Date
}

// CRM 线索渠道 API
export const CrmClueChannelApi = {
  // 查询分页
  getCrmClueChannelPage: async (params: any) => {
    return await request.get({ url: '/crm/clue-channel/page', params })
  },
  // 查询详情
  getCrmClueChannel: async (id: number) => {
    return await request.get({ url: '/crm/clue-channel/get?id=' + id })
  },
  // 新增（channelCode 唯一）
  createCrmClueChannel: async (data: CrmClueChannelVO) => {
    return await request.post({ url: '/crm/clue-channel/create', data })
  },
  // 修改（channelCode 唯一）
  updateCrmClueChannel: async (data: CrmClueChannelVO) => {
    return await request.put({ url: '/crm/clue-channel/update', data })
  },
  // 删除
  deleteCrmClueChannel: async (id: number) => {
    return await request.delete({ url: '/crm/clue-channel/delete?id=' + id })
  }
}
