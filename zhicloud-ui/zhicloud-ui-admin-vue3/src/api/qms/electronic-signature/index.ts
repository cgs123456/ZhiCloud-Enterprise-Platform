import request from '@/config/axios'

// QMS 电子签名记录 VO
export interface ElectronicSignatureLogVO {
  id?: number
  userId?: number
  signatureMeaning?: string
  operationType?: string
  operationContent?: string
  signatureTime?: Date
  ipAddress?: string
  remark?: string
  createTime?: Date
}

// QMS 电子签名记录 API（只读）
export const ElectronicSignatureLogApi = {
  // 查询电子签名记录分页
  getElectronicSignatureLogPage: async (params: any) => {
    return await request.get({ url: '/qms/electronic-signature/page', params })
  }
}
