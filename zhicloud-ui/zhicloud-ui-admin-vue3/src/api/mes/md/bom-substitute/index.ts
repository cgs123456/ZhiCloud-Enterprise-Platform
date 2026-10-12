import request from '@/config/axios'

// Mes BomSubstitute VO
export interface BomSubstituteVO {
  id?: number // id
  bomId?: number // bomId
  bomDetailId?: number // bomDetailId
  substituteItemId?: number // substituteItemId
  substituteItemCode?: string // substituteItemCode
  substituteItemName?: string // substituteItemName
  substituteRatio?: number // substituteRatio
  priority?: number // priority
  effectiveDate?: number // effectiveDate
  expiryDate?: number // expiryDate
  status?: number // status
  remark?: string // remark
}

// Mes BomSubstitute API
export const BomSubstituteApi = {
  createBomSubstitute: async (data: any) => {
    // 创建 BOM 替代料
    return await request.post({ url: `/mes/md/bom-substitute/create`, data })
  },
  updateBomSubstitute: async (data: any) => {
    // 更新 BOM 替代料
    return await request.put({ url: `/mes/md/bom-substitute/update`, data })
  },
  deleteBomSubstitute: async (id: number) => {
    // 删除 BOM 替代料
    return await request.delete({ url: `/mes/md/bom-substitute/delete?id=` + id })
  },
  getBomSubstitute: async (id: number) => {
    // 获得 BOM 替代料
    return await request.get({ url: `/mes/md/bom-substitute/get?id=` + id })
  },
  getBomSubstitutePage: async (params: any) => {
    // 获得 BOM 替代料分页
    return await request.get({ url: `/mes/md/bom-substitute/page`, params })
  },
  BomSubstituteListByDetailId: async (data: any) => {
    // 按 BOM 明细 ID 获得替代料列表（按优先级升序）
    return await request.get({ url: `/mes/md/bom-substitute/list-by-detail-id`, data })
  },
}
