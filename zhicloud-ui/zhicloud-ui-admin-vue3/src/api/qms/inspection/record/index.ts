import request from '@/config/axios'

// 检验记录 VO
export interface InspectionRecordVO {
  id?: number
  orderId?: number
  itemId?: number
  measuredValue?: string
  result?: number
  inspector?: string
  inspectTime?: Date
  remark?: string
  createTime?: Date
}

// 检验记录 API
export const InspectionRecordApi = {
  // 查询检验记录分页
  getInspectionRecordPage: async (params: any) => {
    return await request.get({ url: '/qms/inspection-record/page', params })
  },

  // 查询检验记录详情
  getInspectionRecord: async (id: number) => {
    return await request.get({ url: '/qms/inspection-record/get?id=' + id })
  },
  // 新增检验记录
  createInspectionRecord: async (data: InspectionRecordVO) => {
    return await request.post({ url: '/qms/inspection-record/create', data })
  },

  // 修改检验记录
  updateInspectionRecord: async (data: InspectionRecordVO) => {
    return await request.put({ url: '/qms/inspection-record/update', data })
  },

  // 删除检验记录
  deleteInspectionRecord: async (id: number) => {
    return await request.delete({ url: '/qms/inspection-record/delete?id=' + id })
  },
}
