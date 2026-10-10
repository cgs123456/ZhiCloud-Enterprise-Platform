import request from '@/config/axios'

// OA 审批模板 VO
export interface OaApprovalTemplateVO {
  id?: number
  code?: string
  name?: string
  category?: string
  processDefinitionKey?: string
  formSchema?: string
  formUiSchema?: string
  description?: string
  icon?: string
  sort?: number
  status?: number
  usageCount?: number
  createTime?: Date
}

// OA 审批模板 API
export const OaApprovalTemplateApi = {
  // 查询分页
  getOaApprovalTemplatePage: async (params: any) => {
    return await request.get({ url: '/oa/approval-template/page', params })
  },
  // 查询详情
  getOaApprovalTemplate: async (id: number) => {
    return await request.get({ url: '/oa/approval-template/get?id=' + id })
  },
  // 新增
  createOaApprovalTemplate: async (data: OaApprovalTemplateVO) => {
    return await request.post({ url: '/oa/approval-template/create', data })
  },
  // 修改
  updateOaApprovalTemplate: async (data: OaApprovalTemplateVO) => {
    return await request.put({ url: '/oa/approval-template/update', data })
  },
  // 删除
  deleteOaApprovalTemplate: async (id: number) => {
    return await request.delete({ url: '/oa/approval-template/delete?id=' + id })
  },
}
