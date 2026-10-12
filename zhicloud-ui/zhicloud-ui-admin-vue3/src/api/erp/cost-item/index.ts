import request from '@/config/axios'

// Erp CostItem VO
export interface CostItemVO {
  id?: number // id
  code?: string // code
  name?: string // name
  type?: number // type
  calculationMethod?: string // calculationMethod
  isStandard?: number // isStandard
  remark?: string // remark
  sort?: number // sort
  status?: number // status
  createTime?: number // createTime
}

// Erp CostItem API
export const CostItemApi = {
  createCostItem: async (data: any) => {
    // 创建CostItem
    return await request.post({ url: `/erp/cost-item/create`, data })
  },
  updateCostItem: async (data: any) => {
    // 更新CostItem
    return await request.put({ url: `/erp/cost-item/update`, data })
  },
  deleteCostItem: async (id: number) => {
    // 删除CostItem
    return await request.delete({ url: `/erp/cost-item/delete?id=` + id })
  },
  getCostItem: async (id: number) => {
    // 获得CostItem
    return await request.get({ url: `/erp/cost-item/get?id=` + id })
  },
  getCostItemPage: async (params: any) => {
    // 获得CostItem分页
    return await request.get({ url: `/erp/cost-item/page`, params })
  },
  getCostItemList: async (params?: any) => {
    // 获得CostItem列表
    return await request.get({ url: `/erp/cost-item/list`, params })
  },
}
