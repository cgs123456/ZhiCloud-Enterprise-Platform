import request from '@/config/axios'

// 供应商纠正措施SCAR VO
export interface ScarVO {
  id?: number
  scarNo?: string
  supplierId?: number
  supplierName?: string
  productId?: number
  productName?: string
  defectDescription?: string
  rootCause?: string
  correctiveAction?: string
  status?: number
  closeTime?: Date
  remark?: string
  createTime?: Date
}

// 供应商纠正措施SCAR API
export const ScarApi = {
  // 查询供应商纠正措施SCAR分页
  getScarPage: async (params: any) => {
    return await request.get({ url: '/qms/scar/page', params })
  },

  // 查询供应商纠正措施SCAR详情
  getScar: async (id: number) => {
    return await request.get({ url: '/qms/scar/get?id=' + id })
  },
  // 新增供应商纠正措施SCAR
  createScar: async (data: ScarVO) => {
    return await request.post({ url: '/qms/scar/create', data })
  },

  // 修改供应商纠正措施SCAR
  updateScar: async (data: ScarVO) => {
    return await request.put({ url: '/qms/scar/update', data })
  },

  // 删除供应商纠正措施SCAR
  deleteScar: async (id: number) => {
    return await request.delete({ url: '/qms/scar/delete?id=' + id })
  },
}
