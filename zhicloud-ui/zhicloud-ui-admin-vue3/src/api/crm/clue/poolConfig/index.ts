import request from '@/config/axios'

// CRM 线索公海配置 VO（单例配置：enabled 时 contactExpireDays 必填；notifyEnabled 时 notifyDays 必填）
export interface CrmCluePoolConfigVO {
  enabled?: boolean // 是否开启公海
  contactExpireDays?: number // 未联系回收天数
  notifyEnabled?: boolean // 是否开启通知
  notifyDays?: number // 提前通知天数
}

// CRM 线索公海配置 API
export const CrmCluePoolConfigApi = {
  // 查询配置
  getCrmCluePoolConfig: async () => {
    return await request.get({ url: '/crm/clue-pool-config/get' })
  },
  // 保存配置（单例）
  saveCrmCluePoolConfig: async (data: CrmCluePoolConfigVO) => {
    return await request.put({ url: '/crm/clue-pool-config/save', data })
  }
}
