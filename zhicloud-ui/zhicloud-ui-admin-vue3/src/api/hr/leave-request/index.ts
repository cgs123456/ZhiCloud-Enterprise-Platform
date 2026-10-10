import request from '@/config/axios'

// HR 请假申请 VO
export interface HrLeaveRequestVO {
  id?: number
  employeeId?: number
  leaveTypeId?: number
  startTime?: Date
  endTime?: Date
  duration?: number
  reason?: string
  status?: number
  processInstanceId?: string
  createTime?: Date
}

// HR 请假申请 API（含审批流转：submit/approve/cancel）
export const HrLeaveRequestApi = {
  // 分页查询
  getLeaveRequestPage: async (params: any) => {
    return await request.get({ url: '/hr/leave-request/page', params })
  },
  // 详情
  getLeaveRequest: async (id: number) => {
    return await request.get({ url: '/hr/leave-request/get?id=' + id })
  },
  // 假期余额
  getLeaveBalance: async (params?: any) => {
    return await request.get({ url: '/hr/leave-request/balance', params })
  },
  // 新增（草稿）
  createLeaveRequest: async (data: HrLeaveRequestVO) => {
    return await request.post({ url: '/hr/leave-request/create', data })
  },
  // 提交审批
  submitLeaveRequest: async (id: number) => {
    return await request.put({ url: '/hr/leave-request/submit?id=' + id })
  },
  // 审批（通过/驳回）
  approveLeaveRequest: async (data: any) => {
    return await request.put({ url: '/hr/leave-request/approve', data })
  },
  // 撤销
  cancelLeaveRequest: async (id: number) => {
    return await request.put({ url: '/hr/leave-request/cancel?id=' + id })
  }
}
