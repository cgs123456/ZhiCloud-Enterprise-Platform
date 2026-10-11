import request from '@/config/axios'

// HRM 职位 VO（HR/HRM 融合：复用 hr_position 表）
export interface HrmPositionVO {
  id?: number // 职位编号
  code?: string // 职位编码
  name?: string // 职位名称
  deptId?: number // 所属部门编号
  level?: number // 职级
  baseSalary?: number // 基本工资
  remark?: string // 备注
  sort?: number // 排序
  createTime?: Date // 创建时间
}

// 查询职位分页
export const getPositionPage = async (params: PageParam) => {
  return await request.get<PageResult<HrmPositionVO[]>>({ url: '/hrm/position/page', params })
}

// 查询职位详情
export const getPosition = async (id: number) => {
  return await request.get<HrmPositionVO>({ url: '/hrm/position/get?id=' + id })
}

// 查询指定部门的职位列表
export const getPositionListByDept = async (deptId: number) => {
  return await request.get<HrmPositionVO[]>({ url: '/hrm/position/list-by-dept', params: { deptId } })
}

// 新增职位
export const createPosition = async (data: HrmPositionVO) => {
  return await request.post<number>({ url: '/hrm/position/create', data })
}

// 修改职位
export const updatePosition = async (data: HrmPositionVO) => {
  return await request.put<boolean>({ url: '/hrm/position/update', data })
}

// 删除职位
export const deletePosition = async (id: number) => {
  return await request.delete<boolean>({ url: '/hrm/position/delete?id=' + id })
}
