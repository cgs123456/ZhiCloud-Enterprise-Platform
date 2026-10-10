import request from '@/config/axios'

// HR 考勤 VO
export interface HrAttendanceVO {
  id?: number
  employeeId?: number
  month?: string
  totalDays?: number
  normalDays?: number
  lateDays?: number
  earlyLeaveDays?: number
  absentDays?: number
  overtimeDays?: number
  totalOvertimeHours?: number
  createTime?: Date
}

// HR 考勤 API
export const HrAttendanceApi = {
  // 查询分页
  getHrAttendancePage: async (params: any) => {
    return await request.get({ url: '/hr/attendance/page', params })
  },
  // 查询详情
  getHrAttendance: async (id: number) => {
    return await request.get({ url: '/hr/attendance/get?id=' + id })
  },
  // 新增
  createHrAttendance: async (data: HrAttendanceVO) => {
    return await request.post({ url: '/hr/attendance/create', data })
  },
  // 修改
  updateHrAttendance: async (data: HrAttendanceVO) => {
    return await request.put({ url: '/hr/attendance/update', data })
  },
  // 删除
  deleteHrAttendance: async (id: number) => {
    return await request.delete({ url: '/hr/attendance/delete?id=' + id })
  },
}
