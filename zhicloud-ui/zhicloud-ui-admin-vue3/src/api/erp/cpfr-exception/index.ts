import request from '@/config/axios'

// Erp CpfrException VO
export interface CpfrExceptionVO {
  id?: number // id
  forecastId?: number // forecastId
  exceptionType?: number // exceptionType
  exceptionDescription?: string // exceptionDescription
  handlingStatus?: number // handlingStatus
  handlerUserId?: number // handlerUserId
  handlingTime?: number // handlingTime
  remark?: string // remark
  createTime?: number // createTime
}

// Erp CpfrException API
export const CpfrExceptionApi = {
  deleteCpfrException: async (id: number) => {
    // 删除CpfrException
    return await request.delete({ url: `/erp/cpfr-exception/delete?id=` + id })
  },
  getCpfrException: async (id: number) => {
    // 获得CpfrException
    return await request.get({ url: `/erp/cpfr-exception/get?id=` + id })
  },
  getCpfrExceptionPage: async (params: any) => {
    // 获得CpfrException分页
    return await request.get({ url: `/erp/cpfr-exception/page`, params })
  },
  CpfrExceptionHandle: async (data: any) => {
    // 处理异常
    return await request.put({ url: `/erp/cpfr-exception/handle`, data })
  },
}
