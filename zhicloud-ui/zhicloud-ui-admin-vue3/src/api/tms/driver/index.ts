import request from '@/config/axios'

// TMS 司机 VO
export interface TmsDriverVO {
  id?: number
  name?: string
  phone?: string
  licenseNo?: string
  licenseType?: string
  carrierId?: number
  status?: number
  remark?: string
  createTime?: Date
}

// TMS 司机 API
export const TmsDriverApi = {
  // 查询分页
  getTmsDriverPage: async (params: any) => {
    return await request.get({ url: '/tms/driver/page', params })
  },
  // 查询详情
  getTmsDriver: async (id: number) => {
    return await request.get({ url: '/tms/driver/get?id=' + id })
  },
  // 新增
  createTmsDriver: async (data: TmsDriverVO) => {
    return await request.post({ url: '/tms/driver/create', data })
  },
  // 修改
  updateTmsDriver: async (data: TmsDriverVO) => {
    return await request.put({ url: '/tms/driver/update', data })
  },
  // 删除
  deleteTmsDriver: async (id: number) => {
    return await request.delete({ url: '/tms/driver/delete?id=' + id })
  },
}
