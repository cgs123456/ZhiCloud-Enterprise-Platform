import request from '@/config/axios'

// ERP 账簿管理 VO
export interface ErpAccountBookVO {
  id?: number
  code?: string
  name?: string
  accountingStandard?: number
  accountingStandardName?: string
  currencyId?: number
  isPrimary?: boolean
  status?: number
  statusName?: string
  sort?: number
  remark?: string
  createTime?: Date
}

// ERP 账簿管理 API
export const ErpAccountBookApi = {
  // 查询分页
  getErpAccountBookPage: async (params: any) => {
    return await request.get({ url: '/erp/account-book/page', params })
  },
  // 查询详情
  getErpAccountBook: async (id: number) => {
    return await request.get({ url: '/erp/account-book/get?id=' + id })
  },
  // 新增
  createErpAccountBook: async (data: ErpAccountBookVO) => {
    return await request.post({ url: '/erp/account-book/create', data })
  },
  // 修改
  updateErpAccountBook: async (data: ErpAccountBookVO) => {
    return await request.put({ url: '/erp/account-book/update', data })
  },
  // 删除
  deleteErpAccountBook: async (id: number) => {
    return await request.delete({ url: '/erp/account-book/delete?id=' + id })
  },
}
