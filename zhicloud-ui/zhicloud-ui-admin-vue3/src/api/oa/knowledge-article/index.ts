import request from '@/config/axios'

// OA 知识文章 VO
export interface OaKnowledgeArticleVO {
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

// OA 知识文章 API
export const OaKnowledgeArticleApi = {
  // 查询分页
  getOaKnowledgeArticlePage: async (params: any) => {
    return await request.get({ url: '/oa/knowledge-article/page', params })
  },
  // 查询详情
  getOaKnowledgeArticle: async (id: number) => {
    return await request.get({ url: '/oa/knowledge-article/get?id=' + id })
  },
  // 新增
  createOaKnowledgeArticle: async (data: OaKnowledgeArticleVO) => {
    return await request.post({ url: '/oa/knowledge-article/create', data })
  },
  // 修改
  updateOaKnowledgeArticle: async (data: OaKnowledgeArticleVO) => {
    return await request.put({ url: '/oa/knowledge-article/update', data })
  },
  // 删除
  deleteOaKnowledgeArticle: async (id: number) => {
    return await request.delete({ url: '/oa/knowledge-article/delete?id=' + id })
  },
}
