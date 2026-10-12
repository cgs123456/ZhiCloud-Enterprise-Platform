import request from '@/config/axios'

// Mes MdEcnOrder VO
export interface MdEcnOrderVO {
  id?: number // id
  no?: string // no
  ecnName?: string // ecnName
  changeType?: number // changeType
  bomId?: number // bomId
  bomNo?: string // bomNo
  newBomId?: number // newBomId
  newBomNo?: string // newBomNo
  changeReason?: string // changeReason
  changeDescription?: string // changeDescription
  status?: number // status
  applicantUserId?: number // applicantUserId
  approveUserId?: number // approveUserId
  approveDate?: number // approveDate
  effectiveDate?: number // effectiveDate
  remark?: string // remark
  createTime?: number // createTime
  items?: any[] // items
}

// Mes MdEcnOrder API
export const MdEcnOrderApi = {
  createMdEcnOrder: async (data: any) => {
    // 创建MdEcnOrder
    return await request.post({ url: `/mes/md-ecn-order/create`, data })
  },
  updateMdEcnOrder: async (data: any) => {
    // 更新MdEcnOrder
    return await request.put({ url: `/mes/md-ecn-order/update`, data })
  },
  deleteMdEcnOrder: async (id: number) => {
    // 删除MdEcnOrder
    return await request.delete({ url: `/mes/md-ecn-order/delete?id=` + id })
  },
  getMdEcnOrder: async (id: number) => {
    // 获得MdEcnOrder
    return await request.get({ url: `/mes/md-ecn-order/get?id=` + id })
  },
  getMdEcnOrderPage: async (params: any) => {
    // 获得MdEcnOrder分页
    return await request.get({ url: `/mes/md-ecn-order/page`, params })
  },
  MdEcnOrderSubmit: async (data: any) => {
    // 提交审核
    return await request.put({ url: `/mes/md-ecn-order/submit`, data })
  },
  MdEcnOrderApprove: async (data: any) => {
    // 审核 ECN 单
    return await request.put({ url: `/mes/md-ecn-order/approve`, data })
  },
  MdEcnOrderExecute: async (data: any) => {
    // 执行变更
    return await request.put({ url: `/mes/md-ecn-order/execute`, data })
  },
}
