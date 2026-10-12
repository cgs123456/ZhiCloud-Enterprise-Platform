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
    // 创建Bom
    return await request.post({ url: `/mes/md/bom/create`, data })
  },
  updateBom: async (data: any) => {
    // 更新Bom
    return await request.put({ url: `/mes/md/bom/update`, data })
  },
  deleteBom: async (id: number) => {
    // 删除Bom
    return await request.delete({ url: `/mes/md/bom/delete?id=` + id })
  },
  getBom: async (id: number) => {
    // 获得Bom
    return await request.get({ url: `/mes/md/bom/get?id=` + id })
  },
  getBomPage: async (params: any) => {
    // 获得Bom分页
    return await request.get({ url: `/mes/md/bom/page`, params })
  },
  BomExplode: async (data?: any) => {
    // BOM 递归展开
    return await request.get({ url: `/mes/md/bom/explode`, params: data })
  },
  BomCalculateCost: async (data?: any) => {
    // BOM 成本卷积
    return await request.get({ url: `/mes/md/bom/calculate-cost`, params: data })
  },
}
