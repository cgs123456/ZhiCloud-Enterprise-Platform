import request from '@/config/axios'

// OA 知识版本 VO
// 注意：本域 5 个端点的权限串全部套用文章权限（oa:knowledge-article:query/update）。
export interface OaKnowledgeVersionVO {
  id?: number
  articleId?: number // 文章 ID
  versionNo?: number // 版本号（同一文章内唯一）
  title?: string
  content?: string
  summary?: string
  changeLog?: string // 变更说明
  editorUserId?: number // 编辑人 ID
  editorName?: string // 编辑人姓名
  createTime?: Date
}

// OA 知识版本 API
export const OaKnowledgeVersionApi = {
  // 查询分页（按 versionNo 降序）
  getOaKnowledgeVersionPage: async (params: any) => {
    return await request.get({ url: '/oa/knowledge-version/page', params })
  },
  // 查询详情
  getOaKnowledgeVersion: async (id: number) => {
    return await request.get({ url: '/oa/knowledge-version/get?id=' + id })
  },
  // 新增（权限串为 oa:knowledge-article:update）
  createOaKnowledgeVersion: async (data: OaKnowledgeVersionVO) => {
    return await request.post({ url: '/oa/knowledge-version/create', data })
  },
  // 修改（权限串为 oa:knowledge-article:update）
  updateOaKnowledgeVersion: async (data: OaKnowledgeVersionVO) => {
    return await request.put({ url: '/oa/knowledge-version/update', data })
  },
  // 删除（物理删除，权限串为 oa:knowledge-article:update）
  deleteOaKnowledgeVersion: async (id: number) => {
    return await request.delete({ url: '/oa/knowledge-version/delete?id=' + id })
  }
}
