import request from '@/config/axios'

// OA 知识文章评论 VO（树形：children 嵌套）
export interface OaKnowledgeCommentVO {
  id?: number
  articleId?: number // 文章 ID
  parentId?: number // 父评论 ID（0 = 根评论）
  content?: string // 评论内容
  commentatorUserId?: number // 评论人 ID
  commentatorName?: string // 评论人姓名
  likeCount?: number // 点赞数
  status?: number // 0 正常 / 1 已删除（软删除）
  createTime?: Date
  children?: OaKnowledgeCommentVO[]
}

// OA 知识文章评论 API
export const OaKnowledgeCommentApi = {
  // 查询分页
  getOaKnowledgeCommentPage: async (params: any) => {
    return await request.get({ url: '/oa/knowledge-comment/page', params })
  },
  // 查询详情
  getOaKnowledgeComment: async (id: number) => {
    return await request.get({ url: '/oa/knowledge-comment/get?id=' + id })
  },
  // 新增（parentId 空 = 根评论；commentatorUserId 空 = 当前登录用户）
  createOaKnowledgeComment: async (data: OaKnowledgeCommentVO) => {
    return await request.post({ url: '/oa/knowledge-comment/create', data })
  },
  // 修改
  updateOaKnowledgeComment: async (data: OaKnowledgeCommentVO) => {
    return await request.put({ url: '/oa/knowledge-comment/update', data })
  },
  // 删除（软删除，文章评论数同步 -1）
  deleteOaKnowledgeComment: async (id: number) => {
    return await request.delete({ url: '/oa/knowledge-comment/delete?id=' + id })
  },
  // 按文章查评论树（根评论列表，children 嵌套）
  getCommentTreeByArticle: async (articleId: number) => {
    return await request.get<OaKnowledgeCommentVO[]>({
      url: '/oa/knowledge-comment/list?articleId=' + articleId
    })
  }
}
