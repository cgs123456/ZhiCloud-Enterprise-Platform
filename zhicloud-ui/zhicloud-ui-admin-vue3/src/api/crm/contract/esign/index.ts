import request from '@/config/axios'

/** 电子签状态：10 待签署 / 20 已签署（后端 Stub：status 直接返回 20） */
export const CrmContractEsignStatus = {
  PENDING: 10,
  SIGNED: 20
} as const

// CRM 合同电子签 VO
export interface CrmContractEsignVO {
  esignTaskId?: string // 电子签任务 ID
  contractId?: number // 合同编号
  status?: number // 状态
  statusName?: string // 状态名称
  signTime?: Date // 签署时间
  fileUrls?: string[] // 已签署文件下载地址
}

// CRM 合同电子签 API
// 注意：本域 4 个端点的权限串全部套用 crm:contract:update / crm:contract:query
// （后端 CrmContractEsignController 的 @PreAuthorize 原文），没有独立的
// crm:contract-esign:* 权限——按 URL 臆造权限串会 403。
export const CrmContractEsignApi = {
  // 发起电子签（返回 esignTaskId；已有任务 ID 的合同再发起会被拒）
  initEsign: async (contractId: number) => {
    return await request.post<string>({ url: `/crm/contract-esign/init?contractId=${contractId}` })
  },
  // 查询签署状态
  getEsignStatus: async (esignTaskId: string) => {
    return await request.get<CrmContractEsignVO>({
      url: `/crm/contract-esign/status?esignTaskId=${esignTaskId}`
    })
  },
  // 下载已签署合同（返回 fileUrls）
  downloadSignedContract: async (contractId: number) => {
    return await request.get<CrmContractEsignVO>({
      url: `/crm/contract-esign/download?contractId=${contractId}`
    })
  }
}
