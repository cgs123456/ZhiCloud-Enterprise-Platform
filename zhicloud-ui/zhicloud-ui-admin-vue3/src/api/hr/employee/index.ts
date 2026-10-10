import request from '@/config/axios'

// HR 员工 VO
export interface HrEmployeeVO {
  id?: number
  empNo?: string
  name?: string
  gender?: number
  birthDate?: Date
  idCard?: string
  phone?: string
  email?: string
  deptId?: number
  positionId?: number
  hireDate?: Date
  leaveDate?: Date
  status?: number
  employmentType?: number
  remark?: string
  sort?: number
  createTime?: Date
}

// HR 员工 API
export const HrEmployeeApi = {
  // 查询分页
  getHrEmployeePage: async (params: any) => {
    return await request.get({ url: '/hr/employee/page', params })
  },
  // 查询详情
  getHrEmployee: async (id: number) => {
    return await request.get({ url: '/hr/employee/get?id=' + id })
  },
  // 新增
  createHrEmployee: async (data: HrEmployeeVO) => {
    return await request.post({ url: '/hr/employee/create', data })
  },
  // 修改
  updateHrEmployee: async (data: HrEmployeeVO) => {
    return await request.put({ url: '/hr/employee/update', data })
  },
  // 删除
  deleteHrEmployee: async (id: number) => {
    return await request.delete({ url: '/hr/employee/delete?id=' + id })
  },
}
