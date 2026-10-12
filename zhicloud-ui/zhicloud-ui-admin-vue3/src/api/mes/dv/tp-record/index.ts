import request from '@/config/axios'

// Mes TpRecord API
export const TpRecordApi = {
  getTpRecord: async (id: number) => {
    // 获得TpRecord
    return await request.get({ url: `/mes/dv/tp-record/get?id=` + id })
  },
  getTpRecordPage: async (params: any) => {
    // 获得TpRecord分页
    return await request.get({ url: `/mes/dv/tp-record/page`, params })
  },
}
