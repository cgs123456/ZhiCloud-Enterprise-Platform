import request from '@/config/axios'

// ERP 期末处理 VO
export interface ErpPeriodVO {
  id?: number
  year?: number
  month?: number
  code?: string
  startDate?: Date
  endDate?: Date
  status?: number
  statusName?: string
  closedBy?: string
  closedTime?: Date
  remark?: string
  createTime?: Date
}

// ERP 期末处理 API
export const ErpPeriodApi = {
  // 查询分页
  getErpPeriodPage: async (params: any) => {
    return await request.get({ url: '/erp/period/page', params })
  },
  // 查询详情
  getErpPeriod: async (id: number) => {
    return await request.get({ url: '/erp/period/get?id=' + id })
  },
  // 新增
  createErpPeriod: async (data: ErpPeriodVO) => {
    return await request.post({ url: '/erp/period/create', data })
  },
  // 修改
  updateErpPeriod: async (data: ErpPeriodVO) => {
    return await request.put({ url: '/erp/period/update', data })
  },
  // 删除
  deleteErpPeriod: async (id: number) => {
    return await request.delete({ url: '/erp/period/delete?id=' + id })
  },
}
