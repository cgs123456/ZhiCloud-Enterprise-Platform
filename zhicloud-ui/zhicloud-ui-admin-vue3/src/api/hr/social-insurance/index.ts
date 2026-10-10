import request from '@/config/axios'

// HR 社保 VO
export interface HrSocialInsuranceVO {
  id?: number
  employeeId?: number
  year?: number
  pensionBase?: number
  medicalBase?: number
  unemploymentBase?: number
  workInjuryBase?: number
  maternityBase?: number
  housingFundBase?: number
  status?: number
  remark?: string
  createTime?: Date
}

// HR 社保 API
export const HrSocialInsuranceApi = {
  // 查询分页
  getHrSocialInsurancePage: async (params: any) => {
    return await request.get({ url: '/hr/social-insurance/page', params })
  },
  // 查询详情
  getHrSocialInsurance: async (id: number) => {
    return await request.get({ url: '/hr/social-insurance/get?id=' + id })
  },
  // 新增
  createHrSocialInsurance: async (data: HrSocialInsuranceVO) => {
    return await request.post({ url: '/hr/social-insurance/create', data })
  },
  // 修改
  updateHrSocialInsurance: async (data: HrSocialInsuranceVO) => {
    return await request.put({ url: '/hr/social-insurance/update', data })
  },
  // 删除
  deleteHrSocialInsurance: async (id: number) => {
    return await request.delete({ url: '/hr/social-insurance/delete?id=' + id })
  },
}
