import request from '@/config/axios'

// Erp VatCalculation API
export const VatCalculationApi = {
  VatCalculationOutputTax: async (data?: any) => {
    // 计算销项税额
    return await request.get({ url: `/erp/vat-calculation/output-tax`, params: data })
  },
  VatCalculationInputTax: async (data?: any) => {
    // 计算进项税额
    return await request.get({ url: `/erp/vat-calculation/input-tax`, params: data })
  },
  VatCalculationPayableTax: async (data?: any) => {
    // 计算应纳增值税额
    return await request.get({ url: `/erp/vat-calculation/payable-tax`, params: data })
  },
}
