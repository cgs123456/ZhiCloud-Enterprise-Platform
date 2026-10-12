import request from '@/config/axios'

// Mes BomDetail VO
export interface BomDetailVO {
  id?: number // id
  bomId?: number // bomId
  productId?: number // productId
  productCode?: string // productCode
  productName?: string // productName
  quantity?: number // quantity
  unit?: string // unit
  scrapRate?: number // scrapRate
  unitCost?: number // unitCost
  remark?: string // remark
}

// Mes BomDetail API
export const BomDetailApi = {
  createBomDetail: async (data: any) => {
    // 创建 BOM 明细
    return await request.post({ url: `/mes/md/bom-detail/create`, data })
  },
  updateBomDetail: async (data: any) => {
    // 更新 BOM 明细
    return await request.put({ url: `/mes/md/bom-detail/update`, data })
  },
  deleteBomDetail: async (id: number) => {
    // 删除 BOM 明细
    return await request.delete({ url: `/mes/md/bom-detail/delete?id=` + id })
  },
  getBomDetailPage: async (params: any) => {
    // 获得 BOM 明细分页
    return await request.get({ url: `/mes/md/bom-detail/page`, params })
  },
  BomDetailListByBomId: async (data: any) => {
    // 按 BOM 编号获得明细列表
    return await request.get({ url: `/mes/md/bom-detail/list-by-bom-id`, data })
  },
}
