import request from '@/config/axios'

// 器具校准记录 VO
export interface QmsInstrumentCalibrationVO {
  id?: number
  instrumentId?: number
  calibrationNo?: string
  calibrationDate?: Date
  calibrationOrganization?: string
  calibrationResult?: number
  calibrationCertificateUrl?: string
  deviation?: number
  nextCalibrationDate?: Date
  remark?: string
  sort?: number
  createTime?: Date
}

// 器具校准记录 API
export const QmsInstrumentCalibrationApi = {
  // 查询器具校准记录分页
  getQmsInstrumentCalibrationPage: async (params: any) => {
    return await request.get({ url: '/qms/instrument-calibration/page', params })
  },

  // 查询器具校准记录详情
  getQmsInstrumentCalibration: async (id: number) => {
    return await request.get({ url: '/qms/instrument-calibration/get?id=' + id })
  },
  // 新增器具校准记录（后端端点为 /record：新增校准记录并联动器具的最近/下次校准日期）
  createQmsInstrumentCalibration: async (data: QmsInstrumentCalibrationVO) => {
    return await request.post({ url: '/qms/instrument-calibration/record', data })
  },

  // 修改器具校准记录
  updateQmsInstrumentCalibration: async (data: QmsInstrumentCalibrationVO) => {
    return await request.put({ url: '/qms/instrument-calibration/update', data })
  },

  // 删除器具校准记录
  deleteQmsInstrumentCalibration: async (id: number) => {
    return await request.delete({ url: '/qms/instrument-calibration/delete?id=' + id })
  },
}
