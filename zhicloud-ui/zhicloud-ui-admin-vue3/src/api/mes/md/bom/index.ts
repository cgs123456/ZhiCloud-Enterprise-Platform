import request from '@/config/axios'

// Mes Bom VO
export interface BomVO {
  id?: number // id
  bomNo?: string // bomNo
  productId?: number // productId
  productCode?: string // productCode
  productName?: string // productName
  bomType?: string // bomType
  version?: string // version
  status?: number // status
  remark?: string // remark
  createTime?: number // createTime
  singleLayerCost?: number // singleLayerCost
  FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND?: any // FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND
}

// Mes Bom API
export const BomApi = {
  createBom: async (data: any) => {
    // 创建 BOM
    return await request.post({ url: `/mes/md/bom/create`, data })
  },
  updateBom: async (data: any) => {
    // 更新 BOM
    return await request.put({ url: `/mes/md/bom/update`, data })
  },
  deleteBom: async (id: number) => {
    // 删除 BOM
    return await request.delete({ url: `/mes/md/bom/delete?id=` + id })
  },
  getBom: async (id: number) => {
    // 获得 BOM
    return await request.get({ url: `/mes/md/bom/get?id=` + id })
  },
  getBomPage: async (params: any) => {
    // 获得 BOM 分页
    return await request.get({ url: `/mes/md/bom/page`, params })
  },
  BomExplode: async (productId: any) => {
    // BOM 递归展开
    return await request.get({ url: `/mes/md/bom/explode?productId=` + productId })
  },
  BomCalculateCost: async (productId: any) => {
    // BOM 成本卷积
    return await request.get({ url: `/mes/md/bom/calculate-cost?productId=` + productId })
  },
}
