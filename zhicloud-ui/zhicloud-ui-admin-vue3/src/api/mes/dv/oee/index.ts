import request from '@/config/axios'

// Mes Oee API
export const OeeApi = {
  OeeCalculate: async (data: any) => {
    // 计算 OEE
    return await request.post({ url: `/mes/dv/oee/calculate`, data })
  },
  OeeTrend: async (data: any) => {
    // OEE 趋势图数据
    return await request.get({ url: `/mes/dv/oee/trend`, data })
  },
  OeeRecordList: async (data: any) => {
    // OEE 记录列表
    return await request.get({ url: `/mes/dv/oee/record/list`, data })
  },
}
