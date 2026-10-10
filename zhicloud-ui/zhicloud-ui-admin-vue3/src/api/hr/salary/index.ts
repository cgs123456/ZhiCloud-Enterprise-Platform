import request from '@/config/axios'

// HR 薪资 VO
export interface HrSalaryVO {
  id?: number
  employeeId?: number
  salaryMonth?: string
  baseSalary?: number
  overtimePay?: number
  bonus?: number
  deduction?: number
  socialInsurance?: number
  housingFund?: number
  tax?: number
  netSalary?: number
  status?: number
  remark?: string
  createTime?: Date
}

// HR 薪资 API
export const HrSalaryApi = {
  // 查询分页
  getHrSalaryPage: async (params: any) => {
    return await request.get({ url: '/hr/salary/page', params })
  },
  // 查询详情
  getHrSalary: async (id: number) => {
    return await request.get({ url: '/hr/salary/get?id=' + id })
  },
  // 新增
  createHrSalary: async (data: HrSalaryVO) => {
    return await request.post({ url: '/hr/salary/create', data })
  },
  // 修改
  updateHrSalary: async (data: HrSalaryVO) => {
    return await request.put({ url: '/hr/salary/update', data })
  },
  // 删除
  deleteHrSalary: async (id: number) => {
    return await request.delete({ url: '/hr/salary/delete?id=' + id })
  },
}
