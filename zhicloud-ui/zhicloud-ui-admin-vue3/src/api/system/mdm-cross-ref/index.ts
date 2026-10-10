import request from '@/config/axios'

// 系统 - 主数据跨域对照 VO
export interface MdmCrossRefVO {
  id?: number
  sourceSystem?: string
  sourceEntity?: string
  sourceId?: string
  targetSystem?: string
  targetEntity?: string
  targetId?: string
  status?: string
  remark?: string
  createTime?: Date
}

// 系统 - 主数据跨域对照 API（upsert 语义：save 为幂等登记/覆盖）
export const MdmCrossRefApi = {
  // 分页查询（按当前租户）
  getCrossRefPage: async (params: any) => {
    return await request.get({ url: '/system/mdm-cross-ref/page', params })
  },
  // 登记/更新一条对照（幂等 upsert）
  saveCrossRef: async (data: MdmCrossRefVO) => {
    return await request.post({ url: '/system/mdm-cross-ref/save', data })
  },
  // 批量登记（逐条独立，部分成功）
  saveCrossRefBatch: async (data: MdmCrossRefVO[]) => {
    return await request.post({ url: '/system/mdm-cross-ref/save-batch', data })
  },
  // 失效一条对照（软删除）
  invalidateCrossRef: async (params: any) => {
    return await request.delete({ url: '/system/mdm-cross-ref/invalidate', params })
  }
}
