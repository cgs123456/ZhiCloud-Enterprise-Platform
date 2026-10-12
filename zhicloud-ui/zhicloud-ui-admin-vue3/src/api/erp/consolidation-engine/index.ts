import request from '@/config/axios'

// Erp ConsolidationEngine API
export const ConsolidationEngineApi = {
  ConsolidationEngineGenerateInvestmentEquity: async (data: any) => {
    // 生成投资权益抵消分录
    return await request.post({ url: `/erp/consolidation-engine/generate-investment-equity`, data })
  },
  ConsolidationEngineGenerateIntercompanyArAp: async (data: any) => {
    // 生成内部应收应付抵消分录
    return await request.post({ url: `/erp/consolidation-engine/generate-intercompany-ar-ap`, data })
  },
  ConsolidationEngineGenerateIntercompanySaleCogs: async (data: any) => {
    // 生成内部销售成本抵消分录
    return await request.post({ url: `/erp/consolidation-engine/generate-intercompany-sale-cogs`, data })
  },
  ConsolidationEngineGenerateIntercompanyFa: async (data: any) => {
    // 生成内部固定资产交易抵消分录
    return await request.post({ url: `/erp/consolidation-engine/generate-intercompany-fa`, data })
  },
  ConsolidationEngineGenerateAllEliminations: async (data: any) => {
    // 批量生成所有合并范围的抵消分录
    return await request.post({ url: `/erp/consolidation-engine/generate-all-eliminations`, data })
  },
  ConsolidationEngineConsolidatedBalanceSheet: async (data?: any) => {
    // 生成合并资产负债表
    return await request.get({ url: `/erp/consolidation-engine/consolidated-balance-sheet`, params: data })
  },
  ConsolidationEngineConsolidatedIncomeStatement: async (data?: any) => {
    // 生成合并利润表
    return await request.get({ url: `/erp/consolidation-engine/consolidated-income-statement`, params: data })
  },
}
