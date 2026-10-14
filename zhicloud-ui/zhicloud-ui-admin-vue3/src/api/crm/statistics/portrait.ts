import request from '@/config/axios'

/**
 * 客户画像统计 VO。
 *
 * 注意 *Portion 字段：**后端不返回**（画像端点只给 customerCount/dealCount），
 * 由各展示组件 fetch 后在前端 calculateProportion 自算并赋值（§12.222 核实）。
 * 这里保留声明是为了让模板 prop 有类型，但它们是前端派生值、不是接口字段。
 */
export interface CrmStatisticCustomerBaseRespVO {
  customerCount: number
  dealCount: number
  dealPortion: string | number
}

export interface CrmStatisticCustomerIndustryRespVO extends CrmStatisticCustomerBaseRespVO {
  industryId: number
  industryPortion: string | number
}

export interface CrmStatisticCustomerSourceRespVO extends CrmStatisticCustomerBaseRespVO {
  source: number
  sourcePortion: string | number
}

export interface CrmStatisticCustomerLevelRespVO extends CrmStatisticCustomerBaseRespVO {
  level: number
  levelPortion: string | number
}

export interface CrmStatisticCustomerAreaRespVO extends CrmStatisticCustomerBaseRespVO {
  areaId: number
  areaName: string
  areaPortion: string | number
}

// 客户分析 API
export const StatisticsPortraitApi = {
  // 获取客户行业统计数据
  getCustomerIndustry: (params: any) => {
    return request.get({
      url: '/crm/statistics-portrait/get-customer-industry-summary',
      params
    })
  },
  // 获取客户来源统计数据
  getCustomerSource: (params: any) => {
    return request.get({
      url: '/crm/statistics-portrait/get-customer-source-summary',
      params
    })
  },
  // 获取客户级别统计数据
  getCustomerLevel: (params: any) => {
    return request.get({
      url: '/crm/statistics-portrait/get-customer-level-summary',
      params
    })
  },
  // 获取客户地区统计数据
  getCustomerArea: (params: any) => {
    return request.get({
      url: '/crm/statistics-portrait/get-customer-area-summary',
      params
    })
  }
}
