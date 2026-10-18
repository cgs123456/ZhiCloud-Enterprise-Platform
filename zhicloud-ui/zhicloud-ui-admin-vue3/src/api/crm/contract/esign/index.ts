import request from '@/config/axios'

/** 电子签状态：10 待签署 / 20 已签署 / 30 已拒签（后端任务真状态机，ADR 271） */
export const CrmContractEsignStatus = {
  PENDING: 10,
  SIGNED: 20,
  REJECTED: 30
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
  // 发起电子签（返回 taskNo；任务进行中/已签署会被拒，已拒签可重新发起）
  initEsign: async (contractId: number) => {
    return await request.post<string>({ url: `/crm/contract-esign/init?contractId=${contractId}` })
  },
  // 查询签署状态
  getEsignStatus: async (esignTaskId: string) => {
    return await request.get<CrmContractEsignVO>({
      url: `/crm/contract-esign/status?esignTaskId=${esignTaskId}`
    })
  },
  // 下载已签署合同（仅任务已签署时可取；未签署返回 1020018004）
  downloadSignedContract: async (contractId: number) => {
    return await request.get<CrmContractEsignVO>({
      url: `/crm/contract-esign/download?contractId=${contractId}`
    })
  }
}
