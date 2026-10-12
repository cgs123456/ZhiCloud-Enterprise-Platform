import request from '@/config/axios'

// Erp SupplierEvaluation VO
export interface SupplierEvaluationVO {
  id?: number // id
  supplierId?: number // supplierId
  supplierName?: string // supplierName
  evaluationPeriod?: string // evaluationPeriod
  qualityScore?: number // qualityScore
  deliveryScore?: number // deliveryScore
  priceScore?: number // priceScore
  serviceScore?: number // serviceScore
  totalScore?: number // totalScore
  grade?: string // grade
  evaluator?: string // evaluator
  remark?: string // remark
  creator?: string // creator
  createTime?: number // createTime
  items?: any[] // items
}

// Erp SupplierEvaluation API
export const SupplierEvaluationApi = {
  createSupplierEvaluation: async (data: any) => {
    // 创建供应商评估
    return await request.post({ url: `/erp/supplier-evaluation/create`, data })
  },
  updateSupplierEvaluation: async (data: any) => {
    // 更新供应商评估
    return await request.put({ url: `/erp/supplier-evaluation/update`, data })
  },
  deleteSupplierEvaluation: async (ids: any) => {
    // 删除供应商评估
    return await request.delete({ url: `/erp/supplier-evaluation/delete?ids=` + ids })
  },
  getSupplierEvaluation: async (id: number) => {
    // 获得供应商评估
    return await request.get({ url: `/erp/supplier-evaluation/get?id=` + id })
  },
  getSupplierEvaluationPage: async (params: any) => {
    // 获得供应商评估分页
    return await request.get({ url: `/erp/supplier-evaluation/page`, params })
  },
  SupplierEvaluationCalculate: async (data: any) => {
    // 自动计算供应商评估
    return await request.post({ url: `/erp/supplier-evaluation/calculate`, data })
  },
  SupplierEvaluationItemList: async (data: any) => {
    // 获得供应商评估指标项列表
    return await request.get({ url: `/erp/supplier-evaluation/item/list`, data })
  },
}
