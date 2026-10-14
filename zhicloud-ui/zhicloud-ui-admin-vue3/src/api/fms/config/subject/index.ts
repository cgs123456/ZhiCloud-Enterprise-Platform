import request from '@/config/axios'

/**
 * FMS 语境会计科目 API
 *
 * FMS/ERP 财务融合（台账 §12.214）：FMS 是「ERP 财务的配置层」，会计科目的单一真相是
 * ERP 的 erp_gl_account 表（CRUD 端点在 /erp/gl-account）。FMS 页面**直连 ERP 端点**，
 * 不再建第二套科目表/代理层——权限串 erp:gl-account:* 本来就是单一真相。
 */

/** FMS 语境会计科目 VO（字段与 ERP ErpGlAccountRespVO 一致，独立类型名避免跨模块耦合） */
export interface FmsSubjectVO {
  id?: number
  parentId?: number
  code?: string
  name?: string
  type?: number
  typeName?: string
  balanceDirection?: number
  balanceDirectionName?: string
  level?: number
  isLeaf?: boolean
  openingDebit?: number
  openingCredit?: number
  currentDebit?: number
  currentCredit?: number
  closingDebit?: number
  closingCredit?: number
  status?: number
  sort?: number
  remark?: string
  createTime?: Date
}

export const FmsSubjectApi = {
  // 查询分页
  getSubjectPage: async (params: any) => {
    return await request.get({ url: '/erp/gl-account/page', params })
  },
  // 查询全量列表（前端构建树形结构）
  getSubjectList: async () => {
    return await request.get<FmsSubjectVO[]>({ url: '/erp/gl-account/list' })
  },
  // 查询详情
  getSubject: async (id: number) => {
    return await request.get({ url: '/erp/gl-account/get?id=' + id })
  },
  // 新增
  createSubject: async (data: FmsSubjectVO) => {
    return await request.post({ url: '/erp/gl-account/create', data })
  },
  // 修改
  updateSubject: async (data: FmsSubjectVO) => {
    return await request.put({ url: '/erp/gl-account/update', data })
  },
  // 删除
  deleteSubject: async (id: number) => {
    return await request.delete({ url: '/erp/gl-account/delete?id=' + id })
  }
}
