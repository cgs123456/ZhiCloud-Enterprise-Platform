import request from '@/config/axios'

// 供应商评级 VO
export interface SupplierRatingVO {
  id?: number
  ratingNo?: string
  supplierId?: number
  supplierName?: string
  ratingPeriod?: string
  ppm?: number
  onTimeRate?: number
  qualityRate?: number
  grade?: string
  ratingDate?: Date
  remark?: string
  createTime?: Date
}

// 供应商评级 API
export const SupplierRatingApi = {
  // 查询供应商评级分页
  getSupplierRatingPage: async (params: any) => {
    return await request.get({ url: '/qms/supplier-rating/page', params })
  },

  // 查询供应商评级详情
  getSupplierRating: async (id: number) => {
    return await request.get({ url: '/qms/supplier-rating/get?id=' + id })
  },
  // 新增供应商评级
  createSupplierRating: async (data: SupplierRatingVO) => {
    return await request.post({ url: '/qms/supplier-rating/create', data })
  },

  // 修改供应商评级
  updateSupplierRating: async (data: SupplierRatingVO) => {
    return await request.put({ url: '/qms/supplier-rating/update', data })
  },

  // 删除供应商评级
  deleteSupplierRating: async (id: number) => {
    return await request.delete({ url: '/qms/supplier-rating/delete?id=' + id })
  },
}
