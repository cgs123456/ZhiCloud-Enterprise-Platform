import request from '@/config/axios'

// HR 岗位 VO
export interface HrPositionVO {
  id?: number
  code?: string
  name?: string
  deptId?: number
  level?: number
  baseSalary?: number
  remark?: string
  sort?: number
  createTime?: Date
}

// HR 岗位 API
export const HrPositionApi = {
  // 查询分页
  getHrPositionPage: async (params: any) => {
    return await request.get({ url: '/hr/position/page', params })
  },
  // 查询详情
  getHrPosition: async (id: number) => {
    return await request.get({ url: '/hr/position/get?id=' + id })
  },
  // 新增
  createHrPosition: async (data: HrPositionVO) => {
    return await request.post({ url: '/hr/position/create', data })
  },
  // 修改
  updateHrPosition: async (data: HrPositionVO) => {
    return await request.put({ url: '/hr/position/update', data })
  },
  // 删除
  deleteHrPosition: async (id: number) => {
    return await request.delete({ url: '/hr/position/delete?id=' + id })
  },
}
