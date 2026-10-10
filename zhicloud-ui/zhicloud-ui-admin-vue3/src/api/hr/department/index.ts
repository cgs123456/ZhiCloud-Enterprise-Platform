import request from '@/config/axios'

// HR 部门 VO
export interface HrDepartmentVO {
  id?: number
  parentId?: number
  code?: string
  name?: string
  leaderId?: number
  status?: number
  remark?: string
  sort?: number
  createTime?: Date
}

// HR 部门 API（后端用 list，不是 page；部门是树形结构）
export const HrDepartmentApi = {
  // 查询部门列表
  getDepartmentList: async (params?: any) => {
    return await request.get({ url: '/hr/department/list', params })
  },
  // 精简列表（下拉用）
  getDepartmentSimpleList: async () => {
    return await request.get({ url: '/hr/department/simple-list' })
  },
  // 查询详情
  getDepartment: async (id: number) => {
    return await request.get({ url: '/hr/department/get?id=' + id })
  },
  // 新增
  createDepartment: async (data: HrDepartmentVO) => {
    return await request.post({ url: '/hr/department/create', data })
  },
  // 修改
  updateDepartment: async (data: HrDepartmentVO) => {
    return await request.put({ url: '/hr/department/update', data })
  },
  // 删除
  deleteDepartment: async (id: number) => {
    return await request.delete({ url: '/hr/department/delete?id=' + id })
  }
}
