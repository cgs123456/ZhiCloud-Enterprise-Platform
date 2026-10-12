import request from '@/config/axios'

// Erp ScmBi API
export const ScmBiApi = {
  ScmBiInventoryTurnoverRate: async (data?: any) => {
    // 获得库存周转率
    return await request.get({ url: `/erp/scm-bi/inventory-turnover-rate`, params: data })
  },
  ScmBiInventoryAging: async (data?: any) => {
    // 获得库龄分析
    return await request.get({ url: `/erp/scm-bi/inventory-aging`, params: data })
  },
  ScmBiSlowMovingInventory: async (data?: any) => {
    // 获得呆滞库存分析
    return await request.get({ url: `/erp/scm-bi/slow-moving-inventory`, params: data })
  },
  ScmBiPurchaseOnTimeRate: async (data?: any) => {
    // 获得采购到货及时率
    return await request.get({ url: `/erp/scm-bi/purchase-on-time-rate`, params: data })
  },
  ScmBiPriceFluctuation: async (data?: any) => {
    // 获得采购价格波动
    return await request.get({ url: `/erp/scm-bi/price-fluctuation`, params: data })
  },
  ScmBiSupplierPerformance: async (data?: any) => {
    // 获得供应商绩效评分
    return await request.get({ url: `/erp/scm-bi/supplier-performance`, params: data })
  },
  ScmBiOrderFulfillmentRate: async (data?: any) => {
    // 获得订单履约率
    return await request.get({ url: `/erp/scm-bi/order-fulfillment-rate`, params: data })
  },
  ScmBiStockoutRate: async (data?: any) => {
    // 获得缺货率
    return await request.get({ url: `/erp/scm-bi/stockout-rate`, params: data })
  },
}
