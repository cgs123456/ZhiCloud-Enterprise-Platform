import request from '@/config/axios'

// Erp CpfrForecast VO
export interface CpfrForecastVO {
  id?: number // id
  no?: string // no
  partnerType?: number // partnerType
  partnerId?: number // partnerId
  productId?: number // productId
  productName?: string // productName
  forecastPeriod?: string // forecastPeriod
  forecastQuantity?: number // forecastQuantity
  actualQuantity?: number // actualQuantity
  deviationRate?: number // deviationRate
  remark?: string // remark
  createTime?: number // createTime
}

// Erp CpfrForecast API
export const CpfrForecastApi = {
  createCpfrForecast: async (data: any) => {
    // 创建预测
    return await request.post({ url: `/erp/cpfr-forecast/create`, data })
  },
  updateCpfrForecast: async (data: any) => {
    // 更新预测
    return await request.put({ url: `/erp/cpfr-forecast/update`, data })
  },
  deleteCpfrForecast: async (id: number) => {
    // 删除预测
    return await request.delete({ url: `/erp/cpfr-forecast/delete?id=` + id })
  },
  getCpfrForecast: async (id: number) => {
    // 获得预测
    return await request.get({ url: `/erp/cpfr-forecast/get?id=` + id })
  },
  getCpfrForecastPage: async (params: any) => {
    // 获得预测分页
    return await request.get({ url: `/erp/cpfr-forecast/page`, params })
  },
  CpfrForecastCalculateDeviation: async (id: any) => {
    // 计算偏差率
    return await request.post({ url: `/erp/cpfr-forecast/calculate-deviation?id=` + id })
  },
  CpfrForecastCheckException: async (data: any) => {
    // 检查异常，扫描预测偏差并创建异常记录
    return await request.post({ url: `/erp/cpfr-forecast/check-exception`, data })
  },
}
