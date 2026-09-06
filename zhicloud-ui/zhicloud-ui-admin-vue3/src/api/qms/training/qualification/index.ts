import request from '@/config/axios'

// 上岗资质 VO
export interface QualificationVO {
  id?: number
  userId?: number
  userName?: string
  postId?: number
  postName?: string
  qualificationName?: string
  qualifyDate?: Date
  expireDate?: Date
  status?: number
  remark?: string
  createTime?: Date
}

// 上岗资质 API
export const QualificationApi = {
  // 查询上岗资质分页
  getQualificationPage: async (params: any) => {
    return await request.get({ url: '/qms/qualification/page', params })
  },

  // 查询上岗资质详情
  getQualification: async (id: number) => {
    return await request.get({ url: '/qms/qualification/get?id=' + id })
  },
  // 新增上岗资质
  createQualification: async (data: QualificationVO) => {
    return await request.post({ url: '/qms/qualification/create', data })
  },

  // 修改上岗资质
  updateQualification: async (data: QualificationVO) => {
    return await request.put({ url: '/qms/qualification/update', data })
  },

  // 删除上岗资质
  deleteQualification: async (id: number) => {
    return await request.delete({ url: '/qms/qualification/delete?id=' + id })
  },
}
