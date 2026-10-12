import request from '@/config/axios'

// Erp ProfitCenter VO
export interface ProfitCenterVO {
  id?: number // id
  code?: string // code
  name?: string // name
  parentId?: number // parentId
  managerId?: number // managerId
  status?: number // status
  remark?: string // remark
  createTime?: number // createTime
}

// Erp ProfitCenter API
export const ProfitCenterApi = {
  createProfitCenter: async (data: any) => {
    // 创建利润中心
    return await request.post({ url: `/erp/profit-center/create`, data })
  },
  updateProfitCenter: async (data: any) => {
    // 更新利润中心
    return await request.put({ url: `/erp/profit-center/update`, data })
  },
  deleteProfitCenter: async (id: number) => {
    // 删除利润中心
    return await request.delete({ url: `/erp/profit-center/delete?id=` + id })
  },
  getProfitCenter: async (id: number) => {
    // 获得利润中心
    return await request.get({ url: `/erp/profit-center/get?id=` + id })
  },
  getProfitCenterPage: async (params: any) => {
    // 获得利润中心分页
    return await request.get({ url: `/erp/profit-center/page`, params })
  },
  getProfitCenterList: async (params?: any) => {
    // 获得利润中心列表
    return await request.get({ url: `/erp/profit-center/list`, params })
  },
}
