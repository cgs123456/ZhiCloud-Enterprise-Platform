import request from '@/config/axios'

// MES 计件规则 VO
export interface MesProPieceworkRuleVO {
  id?: number
  ruleName?: string
  processId?: number
  processName?: string
  routeId?: number
  itemId?: number
  itemName?: string
  workstationId?: number
  workstationName?: string
  qualifiedUnitPrice?: number
  scrapUnitPrice?: number
  stepConfig?: string
  effectiveDate?: Date
  expireDate?: Date
  status?: number
  enabled?: number
  remark?: string
  createTime?: Date
}

// MES 计件规则 API
export const MesProPieceworkRuleApi = {
  // 查询分页
  getMesProPieceworkRulePage: async (params: any) => {
    return await request.get({ url: '/mes/pro-piecework-rule/page', params })
  },
  // 查询详情
  getMesProPieceworkRule: async (id: number) => {
    return await request.get({ url: '/mes/pro-piecework-rule/get?id=' + id })
  },
  // 新增
  createMesProPieceworkRule: async (data: MesProPieceworkRuleVO) => {
    return await request.post({ url: '/mes/pro-piecework-rule/create', data })
  },
  // 修改
  updateMesProPieceworkRule: async (data: MesProPieceworkRuleVO) => {
    return await request.put({ url: '/mes/pro-piecework-rule/update', data })
  },
  // 删除
  deleteMesProPieceworkRule: async (id: number) => {
    return await request.delete({ url: '/mes/pro-piecework-rule/delete?id=' + id })
  },
}
