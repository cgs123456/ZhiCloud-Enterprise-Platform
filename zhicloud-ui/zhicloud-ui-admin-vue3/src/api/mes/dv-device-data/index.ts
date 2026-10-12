import request from '@/config/axios'

// Mes DvDeviceData API
export const DvDeviceDataApi = {
  getDvDeviceDataPage: async (params: any) => {
    // 获得DvDeviceData分页
    return await request.get({ url: `/mes/dv-device-data/page`, params })
  },
  DvDeviceDataListByMachinery: async (data?: any) => {
    // 按设备与时间范围查询采集记录
    return await request.get({ url: `/mes/dv-device-data/list-by-machinery`, params: data })
  },
  DvDeviceDataLatest: async (data?: any) => {
    // 获得设备最新一帧采集数据
    return await request.get({ url: `/mes/dv-device-data/latest`, params: data })
  },
  getDvDeviceData: async (id: number) => {
    // 获得DvDeviceData
    return await request.get({ url: `/mes/dv-device-data/get?id=` + id })
  },
  deleteDvDeviceData: async (id: number) => {
    // 删除DvDeviceData
    return await request.delete({ url: `/mes/dv-device-data/delete?id=` + id })
  },
  DvDeviceDataCollect: async (data: any) => {
    // 手动触发一次设备数据采集
    return await request.post({ url: `/mes/dv-device-data/collect`, data })
  },
  DvDeviceDataStart: async (data: any) => {
    // 开启自动采集
    return await request.post({ url: `/mes/dv-device-data/start`, data })
  },
  DvDeviceDataStop: async (data: any) => {
    // 停止自动采集
    return await request.post({ url: `/mes/dv-device-data/stop`, data })
  },
}
