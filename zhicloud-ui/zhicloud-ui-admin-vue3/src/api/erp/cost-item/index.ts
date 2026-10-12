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
    // 创建成本项目
    return await request.post({ url: `/erp/cost-item/create`, data })
  },
  updateCostItem: async (data: any) => {
    // 更新成本项目
    return await request.put({ url: `/erp/cost-item/update`, data })
  },
  deleteCostItem: async (id: number) => {
    // 删除成本项目
    return await request.delete({ url: `/erp/cost-item/delete?id=` + id })
  },
  getCostItem: async (id: number) => {
    // 获得成本项目
    return await request.get({ url: `/erp/cost-item/get?id=` + id })
  },
  getCostItemPage: async (params: any) => {
    // 获得成本项目分页
    return await request.get({ url: `/erp/cost-item/page`, params })
  },
  getCostItemList: async (params?: any) => {
    // 获得启用的成本项目列表
    return await request.get({ url: `/erp/cost-item/list`, params })
  },
}
