import request from '@/config/axios'

// Mes DvDeviceData API
export const DvDeviceDataApi = {
  getDvDeviceDataPage: async (params: any) => {
    // 获得设备数据采集记录分页
    return await request.get({ url: `/mes/dv-device-data/page`, params })
  },
  DvDeviceDataListByMachinery: async (data: any) => {
    // 按设备与时间范围查询采集记录
    return await request.get({ url: `/mes/dv-device-data/list-by-machinery`, data })
  },
  DvDeviceDataLatest: async (machineryId: any) => {
    // 获得设备最新一帧采集数据
    return await request.get({ url: `/mes/dv-device-data/latest?machineryId=` + machineryId })
  },
  getDvDeviceData: async (id: number) => {
    // 获得设备数据采集记录详情
    return await request.get({ url: `/mes/dv-device-data/get?id=` + id })
  },
  deleteDvDeviceData: async (id: number) => {
    // 删除设备数据采集记录
    return await request.delete({ url: `/mes/dv-device-data/delete?id=` + id })
  },
  DvDeviceDataCollect: async (machineryId: any) => {
    // 手动触发一次设备数据采集
    return await request.post({ url: `/mes/dv-device-data/collect?machineryId=` + machineryId })
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
