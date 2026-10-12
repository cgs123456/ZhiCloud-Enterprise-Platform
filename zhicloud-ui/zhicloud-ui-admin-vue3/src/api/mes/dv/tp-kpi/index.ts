import request from '@/config/axios'

// Mes TpKpi API
export const TpKpiApi = {
  TpKpiCalculate: async (data: any) => {
    // 计算 TPM KPI
    return await request.post({ url: `/mes/dv/tp-kpi/calculate`, data })
  },
  getTpKpi: async (id: number) => {
    // 获得TpKpi
    return await request.get({ url: `/mes/dv/tp-kpi/get?id=` + id })
  },
  TpKpiHistory: async (data?: any) => {
    // 查询设备 KPI 历史
    return await request.get({ url: `/mes/dv/tp-kpi/history`, params: data })
  },
}
