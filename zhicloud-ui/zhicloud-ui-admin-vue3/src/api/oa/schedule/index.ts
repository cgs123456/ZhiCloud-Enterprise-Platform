import request from '@/config/axios'

/** 日程类型：10 日程 / 20 任务 / 30 纪念日 / 40 会议 */
export const OaScheduleType = {
  SCHEDULE: 10,
  TASK: 20,
  ANNIVERSARY: 30,
  MEETING: 40
} as const

/** 日程状态：0 未完成 / 1 已完成 / 2 已取消 */
export const OaScheduleStatus = {
  UNFINISHED: 0,
  COMPLETED: 1,
  CANCELLED: 2
} as const

/** 重复类型：0 不重复 / 10 每天 / 20 每周 / 30 每月 / 40 每年 */
export const OaScheduleRepeatType = {
  NONE: 0,
  DAILY: 10,
  WEEKLY: 20,
  MONTHLY: 30,
  YEARLY: 40
} as const

// OA 日程 VO
// 注意：SaveReqVO 无 status 字段——通用更新禁止改状态，状态必须走 complete/cancel 端点。
export interface OaScheduleVO {
  id?: number
  userId?: number // 所属用户
  title?: string
  description?: string
  type?: number
  startTime?: string // yyyy-MM-ddTHH:mm:ss
  endTime?: string // 可空
  allDay?: boolean
  location?: string
  remindMinutes?: number // 提前分钟数
  reminded?: boolean // 是否已提醒（只读）
  repeatType?: number
  status?: number
  remark?: string
  createTime?: Date
}

// OA 日程 API
export const OaScheduleApi = {
  // 查询分页（按开始时间升序）
  getOaSchedulePage: async (params: any) => {
    return await request.get({ url: '/oa/schedule/page', params })
  },
  // 查询详情
  getOaSchedule: async (id: number) => {
    return await request.get({ url: '/oa/schedule/get?id=' + id })
  },
  // 新增
  createOaSchedule: async (data: OaScheduleVO) => {
    return await request.post({ url: '/oa/schedule/create', data })
  },
  // 修改（不能改状态）
  updateOaSchedule: async (data: OaScheduleVO) => {
    return await request.put({ url: '/oa/schedule/update', data })
  },
  // 删除
  deleteOaSchedule: async (id: number) => {
    return await request.delete({ url: '/oa/schedule/delete?id=' + id })
  },
  // 按用户 + 时间区间查询（时间格式 yyyy-MM-dd HH:mm:ss）
  listByUser: async (params: { userId: number; startTime: string; endTime: string }) => {
    return await request.get<OaScheduleVO[]>({ url: '/oa/schedule/list-by-user', params })
  },
  // 完成（status → 1）
  completeOaSchedule: async (id: number) => {
    return await request.put({ url: `/oa/schedule/complete?id=${id}` })
  },
  // 取消（status → 2）
  cancelOaSchedule: async (id: number) => {
    return await request.put({ url: `/oa/schedule/cancel?id=${id}` })
  }
}
