import request from '@/config/axios'

// WMS PDA 作业（手持端任务）
export const WmsPdaApi = {
  // 获取待执行任务（拣货/上架/盘点）
  getTaskList: async () => {
    return await request.get({ url: '/wms/pda/task/list' })
  }
}
