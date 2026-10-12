import request from '@/config/axios'

// Erp StandardCost VO
export interface StandardCostVO {
  id?: number // id
  productId?: number // productId
  productCode?: string // productCode
  productName?: string // productName
  costItemId?: number // costItemId
  standardCost?: number // standardCost
  effectiveDate?: number // effectiveDate
  expiryDate?: number // expiryDate
  status?: number // status
  remark?: string // remark
  createTime?: number // createTime
}

// Erp StandardCost API
export const StandardCostApi = {
  createStandardCost: async (data: any) => {
    // 创建StandardCost
    return await request.post({ url: `/erp/standard-cost/create`, data })
  },
  updateStandardCost: async (data: any) => {
    // 更新StandardCost
    return await request.put({ url: `/erp/standard-cost/update`, data })
  },
  deleteStandardCost: async (id: number) => {
    // 删除StandardCost
    return await request.delete({ url: `/erp/standard-cost/delete?id=` + id })
  },
  getStandardCost: async (id: number) => {
    // 获得StandardCost
    return await request.get({ url: `/erp/standard-cost/get?id=` + id })
  },
  getStandardCostPage: async (params: any) => {
    // 获得StandardCost分页
    return await request.get({ url: `/erp/standard-cost/page`, params })
  },
  StandardCostEffectiveList: async (data?: any) => {
    // 获得产品在某天生效的标准成本列表
    return await request.get({ url: `/erp/standard-cost/effective-list`, params: data })
  },
}
