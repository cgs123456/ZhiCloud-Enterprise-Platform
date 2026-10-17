import request from '@/config/axios'

// 检验记录 VO
export interface InspectionRecordVO {
  id?: number
  orderId?: number
  itemId?: number
  measuredValue?: string
  result?: number
  severity?: number
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

/** 检验结果（与后端 InspectionResultEnum 对齐：10 合格 / 20 不合格 / 30 不适用） */
export const InspectionResultOptions = [
  { value: 10, label: '合格' },
  { value: 20, label: '不合格' },
  { value: 30, label: '不适用' }
]

/** 缺陷严重度（与后端 InspectionSeverityEnum 对齐：10 致命 / 20 严重 / 30 轻微）；
 * §12.255：severity 属有值域契约（@InEnum），不再以自由数字输入。 */
export const InspectionSeverityOptions = [
  { value: 10, label: '致命' },
  { value: 20, label: '严重' },
  { value: 30, label: '轻微' }
]
