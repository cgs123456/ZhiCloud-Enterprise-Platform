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
  // 查询启用的账簿精简列表（下拉选项；V195 银行账户挂账簿用）
  getErpAccountBookSimpleList: async () => {
    return await request.get({ url: '/erp/account-book/simple-list' })
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
