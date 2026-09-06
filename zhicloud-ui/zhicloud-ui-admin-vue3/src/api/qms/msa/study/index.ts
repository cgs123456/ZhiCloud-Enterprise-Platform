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
}
