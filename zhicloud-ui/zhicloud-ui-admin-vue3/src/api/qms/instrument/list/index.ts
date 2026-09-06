import request from '@/config/axios'

// 计量器具台账 VO
export interface QmsInstrumentVO {
  id?: number
  code?: string
  name?: string
  model?: string
  manufacturer?: string
  serialNo?: string
  category?: number
  accuracy?: string
  measureRange?: string
  unit?: string
  status?: number
  location?: string
  responsiblePerson?: string
  calibrationCycleDays?: number
  lastCalibrationDate?: Date
  nextCalibrationDate?: Date
  remark?: string
  sort?: number
  createTime?: Date
}

// 计量器具台账 API
export const QmsInstrumentApi = {
  // 查询计量器具台账分页
  getQmsInstrumentPage: async (params: any) => {
    return await request.get({ url: '/qms/instrument/page', params })
  },

  // 查询计量器具台账详情
  getQmsInstrument: async (id: number) => {
    return await request.get({ url: '/qms/instrument/get?id=' + id })
  },
  // 新增计量器具台账
  createQmsInstrument: async (data: QmsInstrumentVO) => {
    return await request.post({ url: '/qms/instrument/create', data })
  },

  // 修改计量器具台账
  updateQmsInstrument: async (data: QmsInstrumentVO) => {
    return await request.put({ url: '/qms/instrument/update', data })
  },

  // 删除计量器具台账
  deleteQmsInstrument: async (id: number) => {
    return await request.delete({ url: '/qms/instrument/delete?id=' + id })
  },
}
