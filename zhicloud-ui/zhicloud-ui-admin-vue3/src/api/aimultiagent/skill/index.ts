import request from '@/config/axios'

// 技能分组（含下属技能）
export interface MultiAgentSkillGroupVO {
  id: number
  code: string
  name: string
  description?: string
  status: number // 0开启 1关闭
  sort: number
  skills: MultiAgentSkillVO[]
}

// 技能
export interface MultiAgentSkillVO {
  id: number
  groupId: number
  groupCode?: string
  groupName?: string
  code: string
  name: string
  toolName?: string
  description?: string
  configJson?: string
  status: number // 0开启 1关闭
  sort: number
  createTime: string
}

// 多 Agent 技能目录
export const MultiAgentSkillApi = {
  // 查询完整技能目录（分组及其下属技能）
  getCatalog: async () => {
    return await request.get({ url: '/aimultiagent/skill/catalog' })
  },

  // 查询技能详情
  getSkill: async (id: number) => {
    return await request.get({ url: `/aimultiagent/skill/get?id=${id}` })
  },

  // 新建技能（config 中的 endpoint_url 会做 SSRF 校验）
  createSkill: async (data: any) => {
    return await request.post({ url: '/aimultiagent/skill/create', data })
  },

  // 更新技能（config 中的 endpoint_url 会做 SSRF 校验）
  updateSkill: async (id: number, data: any) => {
    return await request.put({ url: `/aimultiagent/skill/update?id=${id}`, data })
  },

  // 更新技能状态（启用/禁用）
  updateSkillStatus: async (id: number, status: number) => {
    return await request.put({
      url: `/aimultiagent/skill/update-status?id=${id}&status=${status}`
    })
  },

  // 删除技能
  deleteSkill: async (id: number) => {
    return await request.delete({ url: `/aimultiagent/skill/delete?id=${id}` })
  }
}
