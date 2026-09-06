import request from '@/config/axios'

// 检验项目 VO
export interface InspectionItemVO {
  id?: number
  code?: string
  name?: string
  type?: number
  method?: number
  standard?: string
  target?: string
  upperLimit?: number
  lowerLimit?: number
  unit?: string
  remark?: string
  status?: number
  createTime?: Date
}

// 检验项目 API
export const InspectionItemApi = {
  // 查询检验项目分页
  getInspectionItemPage: async (params: any) => {
    return await request.get({ url: '/qms/inspection-item/page', params })
  },

  // 查询检验项目详情
  getInspectionItem: async (id: number) => {
    return await request.get({ url: '/qms/inspection-item/get?id=' + id })
  },
  // 新增检验项目
  createInspectionItem: async (data: InspectionItemVO) => {
    return await request.post({ url: '/qms/inspection-item/create', data })
  },

  // 修改检验项目
  updateInspectionItem: async (data: InspectionItemVO) => {
    return await request.put({ url: '/qms/inspection-item/update', data })
  },

  // 删除检验项目
  deleteInspectionItem: async (id: number) => {
    return await request.delete({ url: '/qms/inspection-item/delete?id=' + id })
  },
}
