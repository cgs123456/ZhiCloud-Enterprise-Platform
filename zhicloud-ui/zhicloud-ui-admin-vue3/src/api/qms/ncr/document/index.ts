import request from '@/config/axios'

// 不合格品NCR VO
export interface NcrDocumentVO {
  id?: number
  ncrNo?: string
  source?: number
  inspectionOrderId?: number
  productId?: number
  supplierId?: number
  workOrderId?: number
  defectDescription?: string
  defectLevel?: number
  quantity?: number
  disposition?: number
  status?: number
  remark?: string
  createTime?: Date
}

// MRB 评审记录 VO
export interface NcrMrbRecordVO {
  id?: number
  ncrId?: number
  mrbDate?: number
  mrbMembers?: string
  decision?: number
  conditionTerms?: string
  remark?: string
  creator?: string
  createTime?: number
}

// MRB 处置决议 VO
export interface NcrDispositionVO {
  ncrId: number
  mrbDate?: number
  mrbMembers?: string
  decision?: number
  conditionTerms?: string
  remark?: string
}

// 不合格品NCR API
export const NcrDocumentApi = {
  // 查询不合格品NCR分页
  getNcrDocumentPage: async (params: any) => {
    return await request.get({ url: '/qms/ncr/page', params })
  },

  // 查询不合格品NCR详情
  getNcrDocument: async (id: number) => {
    return await request.get({ url: '/qms/ncr/get?id=' + id })
  },
  // 新增不合格品NCR
  createNcrDocument: async (data: NcrDocumentVO) => {
    return await request.post({ url: '/qms/ncr/create', data })
  },

  // 修改不合格品NCR
  updateNcrDocument: async (data: NcrDocumentVO) => {
    return await request.put({ url: '/qms/ncr/update', data })
  },

  // 删除不合格品NCR
  deleteNcrDocument: async (id: number) => {
    return await request.delete({ url: '/qms/ncr/delete?id=' + id })
  },

  // 提交 MRB 评审（状态 10 待处理 -> 20 MRB 评审中）
  submitForMrb: async (ncrId: number) => {
    return await request.put({ url: '/qms/ncr/submit-mrb?ncrId=' + ncrId })
  },

  // 记录 MRB 处置决议（状态 20 -> 30 已处置）
  recordDisposition: async (data: NcrDispositionVO) => {
    return await request.put({ url: '/qms/ncr/record-disposition', data })
  },

  // 关闭 NCR 报告（状态 30 -> 40 已关闭）
  // 受控操作：后端 @ElectronicSignature 要求 X-Electronic-Signature-* 三个头（21 CFR Part 11）。
  // HTTP 头只能是 Latin-1，含中文的签名理由/密码必须 URL 编码后传输，后端按 UTF-8 解码；
  // 不编码会把中文理由写成乱码，而 Part 11 要求审计记录里的签署理由可读。
  closeNcrDocument: async (id: number, sign?: { username: string; password: string; reason: string }) => {
    const enc = (v: string) => encodeURIComponent(v)
    return await request.put({
      url: '/qms/ncr/close?id=' + id,
      headers: sign
        ? {
            'X-Electronic-Signature-Username': enc(sign.username),
            'X-Electronic-Signature-Password': enc(sign.password),
            'X-Electronic-Signature-Reason': enc(sign.reason)
          }
        : undefined
    })
  },

  // 获得 NCR 关联的 MRB 评审记录列表
  getMrbRecordList: async (ncrId: number) => {
    return await request.get({ url: '/qms/ncr/mrb-record/list?ncrId=' + ncrId })
  }
}
