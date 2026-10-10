import request from '@/config/axios'

// HR 劳动合同 VO
export interface HrContractVO {
  id?: number
  employeeId?: number
  contractNo?: string
  contractType?: number
  startDate?: Date
  endDate?: Date
  signDate?: Date
  probationEndDate?: Date
  positionId?: number
  departmentId?: number
  salary?: number
  status?: number
  fileUrl?: string
  remark?: string
  createTime?: Date
}

// HR 劳动合同 API
export const HrContractApi = {
  // 查询分页
  getHrContractPage: async (params: any) => {
    return await request.get({ url: '/hr/contract/page', params })
  },
  // 查询详情
  getHrContract: async (id: number) => {
    return await request.get({ url: '/hr/contract/get?id=' + id })
  },
  // 新增
  createHrContract: async (data: HrContractVO) => {
    return await request.post({ url: '/hr/contract/create', data })
  },
  // 修改
  updateHrContract: async (data: HrContractVO) => {
    return await request.put({ url: '/hr/contract/update', data })
  },
  // 删除
  deleteHrContract: async (id: number) => {
    return await request.delete({ url: '/hr/contract/delete?id=' + id })
  },
}
