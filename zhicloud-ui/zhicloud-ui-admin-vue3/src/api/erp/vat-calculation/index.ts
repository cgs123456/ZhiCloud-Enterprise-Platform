import request from '@/config/axios'

// Erp VatCalculation API
export const VatCalculationApi = {
  VatCalculationOutputTax: async (data: any) => {
    // 计算销项税额
    return await request.get({ url: `/erp/vat-calculation/output-tax`, data })
  },
  VatCalculationInputTax: async (data: any) => {
    // 计算进项税额
    return await request.get({ url: `/erp/vat-calculation/input-tax`, data })
  },
  VatCalculationPayableTax: async (data: any) => {
    // 计算应纳增值税额
    return await request.get({ url: `/erp/vat-calculation/payable-tax`, data })
  },
}
