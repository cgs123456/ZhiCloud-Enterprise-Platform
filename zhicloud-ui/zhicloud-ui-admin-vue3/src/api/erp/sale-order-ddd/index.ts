import request from '@/config/axios'

// Erp SaleOrderDdd API
export const SaleOrderDddApi = {
  SaleOrderDddSummary: async (data?: any) => {
    // 分页查询销售订单摘要（CQRS 读模型）
    return await request.get({ url: `/erp/sale-order-ddd/summary`, params: data })
  },
  getSaleOrderDddDetail: async (id: number) => {
    // 查询销售订单详情（CQRS 读模型）
    return await request.get({ url: `/erp/sale-order-ddd/detail/` + id })
  },
  SaleOrderDddStatistics: async (data?: any) => {
    // 销售订单统计查询（CQRS 读模型）
    return await request.get({ url: `/erp/sale-order-ddd/statistics`, params: data })
  },
}
