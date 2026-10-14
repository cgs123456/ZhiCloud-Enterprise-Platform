import request from '@/config/axios'

/** 会议预约状态：10 待确认 / 20 已确认 / 30 已取消 / 40 已完成 */
export const OaMeetingReservationStatus = {
  PENDING: 10,
  CONFIRMED: 20,
  CANCELLED: 30,
  COMPLETED: 40
} as const

// OA 会议预约 VO
export interface OaMeetingReservationVO {
  id?: number
  title?: string // 会议主题
  roomId?: number // 会议室
  organizerUserId?: number // 组织人
  attendeeUserIds?: string // 参会人（逗号分隔）
  startTime?: string // 开始时间 yyyy-MM-ddTHH:mm:ss
  endTime?: string // 结束时间
  status?: number
  reminderEnabled?: boolean // 是否提醒
  reminderMinutes?: number // 提前分钟数
  remark?: string
  createTime?: Date
}

// OA 会议预约 API
export const OaMeetingReservationApi = {
  // 查询分页
  getOaMeetingReservationPage: async (params: any) => {
    return await request.get({ url: '/oa/meeting-reservation/page', params })
  },
  // 查询详情
  getOaMeetingReservation: async (id: number) => {
    return await request.get({ url: '/oa/meeting-reservation/get?id=' + id })
  },
  // 新增（服务端悲观锁会议室 + 冲突校验）
  createOaMeetingReservation: async (data: OaMeetingReservationVO) => {
    return await request.post({ url: '/oa/meeting-reservation/create', data })
  },
  // 修改
  updateOaMeetingReservation: async (data: OaMeetingReservationVO) => {
    return await request.put({ url: '/oa/meeting-reservation/update', data })
  },
  // 删除
  deleteOaMeetingReservation: async (id: number) => {
    return await request.delete({ url: '/oa/meeting-reservation/delete?id=' + id })
  },
  // 冲突检测（只读；时间格式 yyyy-MM-dd HH:mm:ss）
  checkConflict: async (params: {
    roomId: number
    startTime: string
    endTime: string
    excludeReservationId?: number
  }) => {
    return await request.get<boolean>({ url: '/oa/meeting-reservation/check-conflict', params })
  }
}
