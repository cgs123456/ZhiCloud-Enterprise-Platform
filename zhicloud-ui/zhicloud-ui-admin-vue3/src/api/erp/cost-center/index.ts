import request from '@/config/axios'

// Erp CostCenter VO
export interface CostCenterVO {
  id?: number // id
  code?: string // code
  name?: string // name
  parentId?: number // parentId
  managerId?: number // managerId
  status?: number // status
  remark?: string // remark
  createTime?: number // createTime
}

// Erp CostCenter API
export const CostCenterApi = {
  createCostCenter: async (data: any) => {
    // 创建CostCenter
    return await request.post({ url: `/erp/cost-center/create`, data })
  },
  updateCostCenter: async (data: any) => {
    // 更新CostCenter
    return await request.put({ url: `/erp/cost-center/update`, data })
  },
  deleteCostCenter: async (id: number) => {
    // 删除CostCenter
    return await request.delete({ url: `/erp/cost-center/delete?id=` + id })
  },
  getCostCenter: async (id: number) => {
    // 获得CostCenter
    return await request.get({ url: `/erp/cost-center/get?id=` + id })
  },
  getCostCenterPage: async (params: any) => {
    // 获得CostCenter分页
    return await request.get({ url: `/erp/cost-center/page`, params })
  },
  getCostCenterList: async (params?: any) => {
    // 获得CostCenter列表
    return await request.get({ url: `/erp/cost-center/list`, params })
  },
}
