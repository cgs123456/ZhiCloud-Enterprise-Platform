import request from '@/config/axios'

// Mes TpRecord API
export const TpRecordApi = {
  getTpRecord: async (id: number) => {
    // 获得 TPM 执行记录
    return await request.get({ url: `/mes/dv/tp-record/get?id=` + id })
  },
  getTpRecordPage: async (params: any) => {
    // 获得 TPM 执行记录分页
    return await request.get({ url: `/mes/dv/tp-record/page`, params })
  },
}
