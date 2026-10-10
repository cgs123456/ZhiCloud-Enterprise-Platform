import request from '@/config/axios'

// ERP 会计凭证 VO
export interface ErpGlVoucherVO {
  id?: number
  voucherNo?: string
  voucherDate?: Date
  periodId?: number
  periodCode?: string
  voucherType?: number
  voucherTypeName?: string
  attachmentCount?: number
  summary?: string
  debitTotal?: number
  creditTotal?: number
  status?: number
  statusName?: string
  preparedBy?: string
  approvedBy?: string
  approvedTime?: Date
  remark?: string
  accountBookId?: number
  sourceEventId?: number
  sourceDocType?: string
  sourceDocId?: number
  sourceDocNo?: string
  createTime?: Date
}

// ERP 会计凭证 API
export const ErpGlVoucherApi = {
  // 查询分页
  getErpGlVoucherPage: async (params: any) => {
    return await request.get({ url: '/erp/gl-voucher/page', params })
  },
  // 查询详情
  getErpGlVoucher: async (id: number) => {
    return await request.get({ url: '/erp/gl-voucher/get?id=' + id })
  },
  // 新增
  createErpGlVoucher: async (data: ErpGlVoucherVO) => {
    return await request.post({ url: '/erp/gl-voucher/create', data })
  },
  // 修改
  updateErpGlVoucher: async (data: ErpGlVoucherVO) => {
    return await request.put({ url: '/erp/gl-voucher/update', data })
  },
  // 删除
  deleteErpGlVoucher: async (id: number) => {
    return await request.delete({ url: '/erp/gl-voucher/delete?id=' + id })
  },
}
