import request from '@/config/axios'

// ERP 采购比价单
export const ErpPurchaseCompareApi = {
  // 查询比价单分页
  getComparePage: async (params: any) => {
    return await request.get({ url: '/erp/purchase-compare/page', params })
  },
  // 查询比价单详情
  getCompare: async (id: number) => {
    return await request.get({ url: '/erp/purchase-compare/get?id=' + id })
  },
  // 生成比价单（从询价单）
  generateCompare: async (data: any) => {
    return await request.post({ url: '/erp/purchase-compare/generate', data })
  }
}
