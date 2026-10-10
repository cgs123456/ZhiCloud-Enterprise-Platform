import request from '@/config/axios'

// MES SCADA 配置 VO
export interface MesDvScadaConfigVO {
  id?: number
  machineryId?: number
  machineryName?: string
  iotDevicePk?: string
  iotProductId?: number
  protocolType?: string
  pointConfig?: string
  enabled?: number
  remark?: string
  createTime?: Date
}

// MES SCADA 配置 API
export const MesDvScadaConfigApi = {
  // 查询分页
  getMesDvScadaConfigPage: async (params: any) => {
    return await request.get({ url: '/mes/dv-scada-config/page', params })
  },
  // 查询详情
  getMesDvScadaConfig: async (id: number) => {
    return await request.get({ url: '/mes/dv-scada-config/get?id=' + id })
  },
  // 新增
  createMesDvScadaConfig: async (data: MesDvScadaConfigVO) => {
    return await request.post({ url: '/mes/dv-scada-config/create', data })
  },
  // 修改
  updateMesDvScadaConfig: async (data: MesDvScadaConfigVO) => {
    return await request.put({ url: '/mes/dv-scada-config/update', data })
  },
  // 删除
  deleteMesDvScadaConfig: async (id: number) => {
    return await request.delete({ url: '/mes/dv-scada-config/delete?id=' + id })
  },
}
