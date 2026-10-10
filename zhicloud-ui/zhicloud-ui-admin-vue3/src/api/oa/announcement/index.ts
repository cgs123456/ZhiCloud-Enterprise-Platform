import request from '@/config/axios'

// OA 公告 VO
export interface OaAnnouncementVO {
  id?: number
  title?: string
  content?: string
  category?: string
  priority?: number
  publisherUserId?: number
  publisherName?: string
  targetScope?: string
  publishTime?: Date
  expireTime?: Date
  topFlag?: boolean
  status?: number
  viewCount?: number
  remark?: string
  createTime?: Date
}

// OA 公告 API
export const OaAnnouncementApi = {
  // 查询分页
  getOaAnnouncementPage: async (params: any) => {
    return await request.get({ url: '/oa/announcement/page', params })
  },
  // 查询详情
  getOaAnnouncement: async (id: number) => {
    return await request.get({ url: '/oa/announcement/get?id=' + id })
  },
  // 新增
  createOaAnnouncement: async (data: OaAnnouncementVO) => {
    return await request.post({ url: '/oa/announcement/create', data })
  },
  // 修改
  updateOaAnnouncement: async (data: OaAnnouncementVO) => {
    return await request.put({ url: '/oa/announcement/update', data })
  },
  // 删除
  deleteOaAnnouncement: async (id: number) => {
    return await request.delete({ url: '/oa/announcement/delete?id=' + id })
  },
}
