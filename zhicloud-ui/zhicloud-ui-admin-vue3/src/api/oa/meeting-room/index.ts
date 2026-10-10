import request from '@/config/axios'

// OA 会议室 VO
export interface OaMeetingRoomVO {
  id?: number
  title?: string
  roomId?: number
  organizerUserId?: number
  attendeeUserIds?: string
  startTime?: Date
  endTime?: Date
  status?: number
  reminderEnabled?: boolean
  reminderMinutes?: number
  remark?: string
  createTime?: Date
}

// OA 会议室 API
export const OaMeetingRoomApi = {
  // 查询分页
  getOaMeetingRoomPage: async (params: any) => {
    return await request.get({ url: '/oa/meeting-room/page', params })
  },
  // 查询详情
  getOaMeetingRoom: async (id: number) => {
    return await request.get({ url: '/oa/meeting-room/get?id=' + id })
  },
  // 新增
  createOaMeetingRoom: async (data: OaMeetingRoomVO) => {
    return await request.post({ url: '/oa/meeting-room/create', data })
  },
  // 修改
  updateOaMeetingRoom: async (data: OaMeetingRoomVO) => {
    return await request.put({ url: '/oa/meeting-room/update', data })
  },
  // 删除
  deleteOaMeetingRoom: async (id: number) => {
    return await request.delete({ url: '/oa/meeting-room/delete?id=' + id })
  },
}
