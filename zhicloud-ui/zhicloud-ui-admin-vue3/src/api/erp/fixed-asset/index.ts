import request from '@/config/axios'

// Erp FixedAsset VO
export interface FixedAssetVO {
  id?: number // id
  code?: string // code
  name?: string // name
  category?: string // category
  specification?: string // specification
  departmentId?: number // departmentId
  location?: string // location
  responsiblePerson?: string // responsiblePerson
  originalValue?: number // originalValue
  salvageValue?: number // salvageValue
  usefulLifeMonths?: number // usefulLifeMonths
  depreciationMethod?: number // depreciationMethod
  depreciationMethodName?: string // depreciationMethodName
  capitalizationDate?: number // capitalizationDate
  assetAccountId?: number // assetAccountId
  accumulatedDepreciationAccountId?: number // accumulatedDepreciationAccountId
  depreciationExpenseAccountId?: number // depreciationExpenseAccountId
  depreciatedMonths?: number // depreciatedMonths
  accumulatedDepreciation?: number // accumulatedDepreciation
  netBookValue?: number // netBookValue
  status?: number // status
  statusName?: string // statusName
  remark?: string // remark
}

// Erp FixedAsset API
export const FixedAssetApi = {
  createFixedAsset: async (data: any) => {
    // 创建固定资产
    return await request.post({ url: `/erp/fixed-asset/create`, data })
  },
  updateFixedAsset: async (data: any) => {
    // 更新固定资产
    return await request.put({ url: `/erp/fixed-asset/update`, data })
  },
  deleteFixedAsset: async (id: number) => {
    // 删除固定资产
    return await request.delete({ url: `/erp/fixed-asset/delete?id=` + id })
  },
  getFixedAsset: async (id: number) => {
    // 获取固定资产
    return await request.get({ url: `/erp/fixed-asset/get?id=` + id })
  },
  getFixedAssetPage: async (params: any) => {
    // 分页查询固定资产
    return await request.get({ url: `/erp/fixed-asset/page`, params })
  },
  FixedAssetCalculateDepreciation: async (data: any) => {
    // 计提月度折旧
    return await request.post({ url: `/erp/fixed-asset/calculate-depreciation`, data })
  },
  FixedAssetApproveDepreciation: async (id: any) => {
    // 审核折旧记录
    return await request.put({ url: `/erp/fixed-asset/approve-depreciation?id=` + id })
  },
  FixedAssetDepreciationList: async (data: any) => {
    // 查询资产折旧记录列表
    return await request.get({ url: `/erp/fixed-asset/depreciation-list`, data })
  },
  idleFixedAsset: async (id: number) => {
    // 闲置固定资产（在用 → 闲置）
    return await request.put({ url: `/erp/fixed-asset/idle?id=` + id })
  },
  enableFixedAsset: async (id: number) => {
    // 启用固定资产（闲置 → 在用）
    return await request.put({ url: `/erp/fixed-asset/enable?id=` + id })
  },
  disposeFixedAsset: async (id: number) => {
    // 处置固定资产（在用/闲置 → 已处置，终态）
    return await request.put({ url: `/erp/fixed-asset/dispose?id=` + id })
  },
  scrapFixedAsset: async (id: number) => {
    // 报废固定资产（在用/闲置 → 已报废，终态）
    return await request.put({ url: `/erp/fixed-asset/scrap?id=` + id })
  },
}
