import request from '@/config/axios'

// WMS 计费合同 VO
export interface WmsBillingContractVO {
  id?: number
  contractNo?: string
  ownerId?: number
  ownerName?: string
  contractName?: string
  startDate?: Date
  endDate?: Date
  status?: number
  remark?: string
  creatorName?: string
  totalAmount?: number
  createTime?: Date
}

// WMS 计费合同 API
export const WmsBillingContractApi = {
  // 查询分页
  getWmsBillingContractPage: async (params: any) => {
    return await request.get({ url: '/wms/billing-contract/page', params })
  },
  // 查询详情
  getWmsBillingContract: async (id: number) => {
    return await request.get({ url: '/wms/billing-contract/get?id=' + id })
  },
  // 新增
  createWmsBillingContract: async (data: WmsBillingContractVO) => {
    return await request.post({ url: '/wms/billing-contract/create', data })
  },
  // 修改
  updateWmsBillingContract: async (data: WmsBillingContractVO) => {
    return await request.put({ url: '/wms/billing-contract/update', data })
  },
  // 删除
  deleteWmsBillingContract: async (id: number) => {
    return await request.delete({ url: '/wms/billing-contract/delete?id=' + id })
  },
}
