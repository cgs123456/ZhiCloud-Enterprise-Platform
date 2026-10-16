import request from '@/config/axios'

// MSA研究 VO
export interface MsaStudyVO {
  id?: number
  studyNo?: string
  studyType?: number
  characteristicName?: string
  equipmentId?: number
  appraiserCount?: number
  trialCount?: number
  partCount?: number
  status?: number
  remark?: string
  createTime?: Date
}

// MSA研究 API
export const MsaStudyApi = {
  // 查询MSA研究分页
  getMsaStudyPage: async (params: any) => {
    return await request.get({ url: '/qms/msa/page', params })
  },

  // 查询MSA研究详情
  getMsaStudy: async (id: number) => {
    return await request.get({ url: '/qms/msa/get?id=' + id })
  },
  // 新增MSA研究
  createMsaStudy: async (data: MsaStudyVO) => {
    return await request.post({ url: '/qms/msa/create', data })
  },

  // 修改MSA研究
  updateMsaStudy: async (data: MsaStudyVO) => {
    return await request.put({ url: '/qms/msa/update', data })
  },

  // 删除MSA研究
  deleteMsaStudy: async (id: number) => {
    return await request.delete({ url: '/qms/msa/delete?id=' + id })
  },

  // 测量数据列表（§12.239 接线 MSA 数据闭环）
  getMsaMeasurementList: async (studyId: number) => {
    return await request.get({ url: '/qms/msa/measurement/list?studyId=' + studyId })
  },

  // 保存测量数据
  saveMsaMeasurement: async (data: any) => {
    return await request.post({ url: '/qms/msa/measurement/save', data })
  },

  // 计算 Gage R&R（副作用：研究状态置为已完成 30）
  calculateMsaGageRR: async (studyId: number) => {
    return await request.get({ url: '/qms/msa/calculate-gage-rr?studyId=' + studyId })
  }
}
