import request from '@/config/axios'

// OA 知识分类 VO
export interface OaKnowledgeCategoryVO {
  id?: number
  categoryId?: number
  title?: string
  summary?: string
  content?: string
  tags?: string
  authorUserId?: number
  authorName?: string
  viewCount?: number
  likeCount?: number
  commentCount?: number
  currentVersion?: number
  status?: number
  topFlag?: boolean
  remark?: string
  createTime?: Date
}

// OA 知识分类 API
export const OaKnowledgeCategoryApi = {
  // 查询分页
  getOaKnowledgeCategoryPage: async (params: any) => {
    return await request.get({ url: '/oa/knowledge-category/page', params })
  },
  // 查询详情
  getOaKnowledgeCategory: async (id: number) => {
    return await request.get({ url: '/oa/knowledge-category/get?id=' + id })
  },
  // 新增
  createOaKnowledgeCategory: async (data: OaKnowledgeCategoryVO) => {
    return await request.post({ url: '/oa/knowledge-category/create', data })
  },
  // 修改
  updateOaKnowledgeCategory: async (data: OaKnowledgeCategoryVO) => {
    return await request.put({ url: '/oa/knowledge-category/update', data })
  },
  // 删除
  deleteOaKnowledgeCategory: async (id: number) => {
    return await request.delete({ url: '/oa/knowledge-category/delete?id=' + id })
  },
}
