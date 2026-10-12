import request from '@/config/axios'

// Erp FaChange VO
export interface FaChangeVO {
  id?: number // id
  assetId?: number // assetId
  changeType?: number // changeType
  changeTypeName?: string // changeTypeName
  beforeValue?: string // beforeValue
  afterValue?: string // afterValue
  changeDate?: number // changeDate
  changeReason?: string // changeReason
  operatorId?: number // operatorId
  approverId?: number // approverId
  status?: number // status
  statusName?: string // statusName
  rejectReason?: string // rejectReason
  remark?: string // remark
}

// Erp FaChange API
export const FaChangeApi = {
  createFaChange: async (data: any) => {
    // 创建FaChange
    return await request.post({ url: `/erp/fa-change/create`, data })
  },
  updateFaChange: async (data: any) => {
    // 更新FaChange
    return await request.put({ url: `/erp/fa-change/update`, data })
  },
  deleteFaChange: async (id: number) => {
    // 删除FaChange
    return await request.delete({ url: `/erp/fa-change/delete?id=` + id })
  },
  getFaChange: async (id: number) => {
    // 获得FaChange
    return await request.get({ url: `/erp/fa-change/get?id=` + id })
  },
  getFaChangePage: async (params: any) => {
    // 获得FaChange分页
    return await request.get({ url: `/erp/fa-change/page`, params })
  },
  FaChangeApprove: async (data: any) => {
    // 审核通过资产变动申请
    return await request.put({ url: `/erp/fa-change/approve`, data })
  },
  FaChangeReject: async (data: any) => {
    // 驳回资产变动申请
    return await request.put({ url: `/erp/fa-change/reject`, data })
  },
  FaChangeListByAsset: async (data?: any) => {
    // 查询某资产的变动历史
    return await request.get({ url: `/erp/fa-change/list-by-asset`, params: data })
  },
}
