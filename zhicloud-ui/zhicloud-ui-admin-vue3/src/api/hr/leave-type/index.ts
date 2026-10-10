import request from '@/config/axios'

// HR 假期类型 VO
export interface HrLeaveTypeVO {
  id?: number
  name?: string
  code?: string
  isPaid?: number
  deductSalary?: number
  remark?: string
  createTime?: Date
}

// HR 假期类型 API（后端用 list，不是 page）
export const HrLeaveTypeApi = {
  // 查询列表
  getLeaveTypeList: async (params?: any) => {
    return await request.get({ url: '/hr/leave-type/list', params })
  },
  // 查询详情
  getLeaveType: async (id: number) => {
    return await request.get({ url: '/hr/leave-type/get?id=' + id })
  },
  // 新增
  createLeaveType: async (data: HrLeaveTypeVO) => {
    return await request.post({ url: '/hr/leave-type/create', data })
  },
  // 修改
  updateLeaveType: async (data: HrLeaveTypeVO) => {
    return await request.put({ url: '/hr/leave-type/update', data })
  },
  // 删除
  deleteLeaveType: async (id: number) => {
    return await request.delete({ url: '/hr/leave-type/delete?id=' + id })
  }
}
