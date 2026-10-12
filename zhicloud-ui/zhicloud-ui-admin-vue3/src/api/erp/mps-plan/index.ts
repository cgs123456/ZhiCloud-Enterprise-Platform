import request from '@/config/axios'

// Erp MpsPlan VO
export interface MpsPlanVO {
  id?: number // id
  planNo?: string // planNo
  productId?: number // productId
  productCode?: string // productCode
  productName?: string // productName
  planPeriod?: string // planPeriod
  planType?: number // planType
  demandDate?: number // demandDate
  plannedQuantity?: number // plannedQuantity
  plannedFinishDate?: number // plannedFinishDate
  source?: number // source
  sourceOrderId?: number // sourceOrderId
  status?: number // status
  remark?: string // remark
  sort?: number // sort
  creator?: string // creator
  createTime?: number // createTime
  details?: any[] // details
}

// Erp MpsPlan API
export const MpsPlanApi = {
  createMpsPlan: async (data: any) => {
    // 创建主生产计划
    return await request.post({ url: `/erp/mps-plan/create`, data })
  },
  updateMpsPlan: async (data: any) => {
    // 更新主生产计划
    return await request.put({ url: `/erp/mps-plan/update`, data })
  },
  deleteMpsPlan: async (ids: any) => {
    // 删除主生产计划
    return await request.delete({ url: `/erp/mps-plan/delete?ids=` + ids })
  },
  getMpsPlan: async (id: number) => {
    // 获得主生产计划
    return await request.get({ url: `/erp/mps-plan/get?id=` + id })
  },
  getMpsPlanPage: async (params: any) => {
    // 获得主生产计划分页
    return await request.get({ url: `/erp/mps-plan/page`, params })
  },
  MpsPlanGenerate: async (data: any) => {
    // 生成 MPS 计划（核心算法）
    return await request.post({ url: `/erp/mps-plan/generate`, data })
  },
  MpsPlanConfirm: async (id: any) => {
    // 确认主生产计划
    return await request.put({ url: `/erp/mps-plan/confirm?id=` + id })
  },
  MpsPlanReleaseMrp: async (id: any) => {
    // 下发 MRP
    return await request.put({ url: `/erp/mps-plan/release-mrp?id=` + id })
  },
  MpsPlanClose: async (id: any) => {
    // 关闭主生产计划
    return await request.put({ url: `/erp/mps-plan/close?id=` + id })
  },
  MpsPlanDetailList: async (data: any) => {
    // 获得主生产计划明细列表
    return await request.get({ url: `/erp/mps-plan/detail/list`, data })
  },
}
