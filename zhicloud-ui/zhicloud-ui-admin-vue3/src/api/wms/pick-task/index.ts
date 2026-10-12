import request from '@/config/axios'

// Wms PickTask VO
export interface PickTaskVO {
  id?: number // id
  taskNo?: string // taskNo
  shipmentOrderId?: number // shipmentOrderId
  shipmentOrderNo?: string // shipmentOrderNo
  waveOrderId?: number // waveOrderId
  waveOrderNo?: string // waveOrderNo
  skuId?: number // skuId
  skuCode?: string // skuCode
  skuName?: string // skuName
  productName?: string // productName
  quantity?: number // quantity
  pickedQuantity?: number // pickedQuantity
  locationId?: number // locationId
  locationCode?: string // locationCode
  pickSequence?: number // pickSequence
  status?: number // status
  pickerUserId?: number // pickerUserId
  pickerUserName?: string // pickerUserName
  pickTime?: number // pickTime
  remark?: string // remark
  createTime?: number // createTime
}

// Wms PickTask API
export const PickTaskApi = {
  getPickTaskPage: async (params: any) => {
    // 获得拣货任务分页
    return await request.get({ url: `/wms/pick-task/page`, params })
  },
  PickTaskGenerate: async (shipmentOrderId: number) => {
    // 按出库单生成拣货任务
    return await request.post({ url: `/wms/pick-task/generate?shipmentOrderId=` + shipmentOrderId })
  },
  PickTaskConfirmPick: async (taskId: number, pickedQuantity: number) => {
    // 确认拣货
    return await request.put({ url: `/wms/pick-task/confirm-pick?taskId=` + taskId + `&pickedQuantity=` + pickedQuantity })
  }
}
