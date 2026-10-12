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
    // 创建FixedAsset
    return await request.post({ url: `/erp/fixed-asset/create`, data })
  },
  updateFixedAsset: async (data: any) => {
    // 更新FixedAsset
    return await request.put({ url: `/erp/fixed-asset/update`, data })
  },
  deleteFixedAsset: async (id: number) => {
    // 删除FixedAsset
    return await request.delete({ url: `/erp/fixed-asset/delete?id=` + id })
  },
  getFixedAsset: async (id: number) => {
    // 获得FixedAsset
    return await request.get({ url: `/erp/fixed-asset/get?id=` + id })
  },
  getFixedAssetPage: async (params: any) => {
    // 获得FixedAsset分页
    return await request.get({ url: `/erp/fixed-asset/page`, params })
  },
  FixedAssetCalculateDepreciation: async (data: any) => {
    // 计提月度折旧
    return await request.post({ url: `/erp/fixed-asset/calculate-depreciation`, data })
  },
  FixedAssetApproveDepreciation: async (data: any) => {
    // 审核折旧记录
    return await request.put({ url: `/erp/fixed-asset/approve-depreciation`, data })
  },
  FixedAssetDepreciationList: async (data?: any) => {
    // 查询资产折旧记录列表
    return await request.get({ url: `/erp/fixed-asset/depreciation-list`, params: data })
  },
}
