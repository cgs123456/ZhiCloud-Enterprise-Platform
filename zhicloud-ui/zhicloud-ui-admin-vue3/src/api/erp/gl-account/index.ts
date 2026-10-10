import request from '@/config/axios'

// ERP 会计科目 VO
export interface ErpGlAccountVO {
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

// ERP 会计科目 API
export const ErpGlAccountApi = {
  // 查询分页
  getErpGlAccountPage: async (params: any) => {
    return await request.get({ url: '/erp/gl-account/page', params })
  },
  // 查询详情
  getErpGlAccount: async (id: number) => {
    return await request.get({ url: '/erp/gl-account/get?id=' + id })
  },
  // 新增
  createErpGlAccount: async (data: ErpGlAccountVO) => {
    return await request.post({ url: '/erp/gl-account/create', data })
  },
  // 修改
  updateErpGlAccount: async (data: ErpGlAccountVO) => {
    return await request.put({ url: '/erp/gl-account/update', data })
  },
  // 删除
  deleteErpGlAccount: async (id: number) => {
    return await request.delete({ url: '/erp/gl-account/delete?id=' + id })
  },
}
