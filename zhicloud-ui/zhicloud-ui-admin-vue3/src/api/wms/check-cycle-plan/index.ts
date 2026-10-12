import request from '@/config/axios'

// Wms CheckCyclePlan VO
export interface CheckCyclePlanVO {
  id?: number // id
  warehouseId?: number // warehouseId
  warehouseName?: string // warehouseName
  abcClassification?: string // abcClassification
  cycleDays?: number // cycleDays
  nextCheckDate?: number // nextCheckDate
  enabled?: number // enabled
  remark?: string // remark
  createTime?: number // createTime
}

// Wms CheckCyclePlan API
export const CheckCyclePlanApi = {
  createCheckCyclePlan: async (data: any) => {
    // 创建循环盘点计划
    return await request.post({ url: `/wms/check-cycle-plan/create`, data })
  },
  updateCheckCyclePlan: async (data: any) => {
    // 更新循环盘点计划
    return await request.put({ url: `/wms/check-cycle-plan/update`, data })
  },
  deleteCheckCyclePlan: async (id: number) => {
    // 删除循环盘点计划
    return await request.delete({ url: `/wms/check-cycle-plan/delete?id=` + id })
  },
  getCheckCyclePlan: async (id: number) => {
    // 获得循环盘点计划
    return await request.get({ url: `/wms/check-cycle-plan/get?id=` + id })
  },
  getCheckCyclePlanPage: async (params: any) => {
    // 获得循环盘点计划分页
    return await request.get({ url: `/wms/check-cycle-plan/page`, params })
  },
}
