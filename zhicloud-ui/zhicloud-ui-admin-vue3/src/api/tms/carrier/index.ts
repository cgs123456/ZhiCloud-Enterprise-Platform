import request from '@/config/axios'

// TMS 承运商 VO
export interface TmsCarrierVO {
  id?: number
  name?: string
  code?: string
  serviceType?: number
  contactPerson?: string
  contactPhone?: string
  qualificationNo?: string
  rating?: number
  status?: number
  remark?: string
  createTime?: Date
}

// TMS 承运商 API
export const TmsCarrierApi = {
  // 查询分页
  getTmsCarrierPage: async (params: any) => {
    return await request.get({ url: '/tms/carrier/page', params })
  },
  // 查询详情
  getTmsCarrier: async (id: number) => {
    return await request.get({ url: '/tms/carrier/get?id=' + id })
  },
  // 新增
  createTmsCarrier: async (data: TmsCarrierVO) => {
    return await request.post({ url: '/tms/carrier/create', data })
  },
  // 修改
  updateTmsCarrier: async (data: TmsCarrierVO) => {
    return await request.put({ url: '/tms/carrier/update', data })
  },
  // 删除
  deleteTmsCarrier: async (id: number) => {
    return await request.delete({ url: '/tms/carrier/delete?id=' + id })
  },
}
